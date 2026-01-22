package org.example.microgrid.house;

import org.example.microgrid.meter.Meter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class House {

    /* =========================
       Configuration
       ========================= */
    private static final long FIVE_MINUTES_SECONDS = 5 * 60;

    /* =========================
       Meter
       ========================= */
    private final Meter meter;

    /* =========================
       Instantaneous Rates (kW)
       ========================= */
    private double currentConsumptionRate;
    private double currentProductionRate;

    /* =========================
       Energy Totals (kWh)
       ========================= */
    private double totalConsumption;
    private double totalProduction;

    /* =========================
       5-minute Accumulators (kWh)
       ========================= */
    private double consumptionLast5Min;
    private double productionLast5Min;

    /* =========================
       Time Tracking
       ========================= */
    private Instant t1;
    private Instant t2;

    /* =========================
       Scheduler
       ========================= */
    private final ScheduledExecutorService scheduler;

    /* =========================
       Constructor
       ========================= */
    public House(Meter meter) {
        this.meter = meter;

        this.currentConsumptionRate = 0.0;
        this.currentProductionRate = 0.0;

        this.totalConsumption = 0.0;
        this.totalProduction = 0.0;

        this.consumptionLast5Min = 0.0;
        this.productionLast5Min = 0.0;

        Instant now = Instant.now();
        this.t1 = now;
        this.t2 = now;

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(
                this::pushFiveMinuteUpdateToMeter,
                FIVE_MINUTES_SECONDS,
                FIVE_MINUTES_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /* =========================
       Setters
       ========================= */

    public synchronized void setCurrentConsumption(double consumptionKw) {
        if (consumptionKw < 0)
            throw new IllegalArgumentException("Consumption cannot be negative");

        updateEnergyAccounting();
        this.currentConsumptionRate = consumptionKw;
        this.currentProductionRate = 0.0;
    }

    public synchronized void setCurrentProduction(double productionKw) {
        if (productionKw < 0)
            throw new IllegalArgumentException("Production cannot be negative");

        updateEnergyAccounting();
        this.currentProductionRate = productionKw;
        this.currentConsumptionRate = 0.0;
    }

    /* =========================
       Core Accounting
       ========================= */

    private synchronized void updateEnergyAccounting() {
        Instant now = Instant.now();

        double dt1 =
                Duration.between(t1, now).toMillis() / 3_600_000.0;
        double dt2 =
                Duration.between(t2, now).toMillis() / 3_600_000.0;

        totalConsumption += currentConsumptionRate * dt1;
        totalProduction  += currentProductionRate  * dt1;

        consumptionLast5Min += currentConsumptionRate * dt2;
        productionLast5Min  += currentProductionRate  * dt2;

        t1 = now;
        t2 = now;
    }

    /* =========================
       5-minute Meter Push
       ========================= */

    private synchronized void pushFiveMinuteUpdateToMeter() {
        Instant now = Instant.now();

        double dt =
                Duration.between(t2, now).toMillis() / 3_600_000.0;

        double cons =
                consumptionLast5Min + currentConsumptionRate * dt;
        double prod =
                productionLast5Min + currentProductionRate * dt;

        double avgConsKw = cons / (FIVE_MINUTES_SECONDS / 3600.0);
        double avgProdKw = prod / (FIVE_MINUTES_SECONDS / 3600.0);

        meter.update(avgConsKw, avgProdKw);

        consumptionLast5Min = 0.0;
        productionLast5Min = 0.0;
        t2 = now;
    }

    /* =========================
       Billing
       ========================= */

    public synchronized Bill generateBill(double sellingPricePerKwh) {
        waitForNextFiveMinuteBoundary();

        // Ensure all energy is accounted
        updateEnergyAccounting();

        double consumed = totalConsumption;
        double sold = meter.getExportEnergy();
        double amount = sold * sellingPricePerKwh;

        // Reset EVERYTHING including 5-min window
        resetAll();

        return new Bill(consumed, sold, sellingPricePerKwh, amount);
    }

    private void waitForNextFiveMinuteBoundary() {
        long elapsed =
                Duration.between(t2, Instant.now()).getSeconds();

        long remaining = FIVE_MINUTES_SECONDS - elapsed;
        if (remaining <= 0) return;

        try {
            TimeUnit.SECONDS.sleep(remaining + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /* =========================
       Reset Logic
       ========================= */

    private synchronized void resetAll() {
        this.totalConsumption = 0.0;
        this.totalProduction = 0.0;
        this.consumptionLast5Min = 0.0;
        this.productionLast5Min = 0.0;

        Instant now = Instant.now();
        this.t1 = now;
        this.t2 = now;
    }

    /* =========================
       Shutdown
       ========================= */

    public void shutdown() {
        scheduler.shutdownNow();
    }

    /* =========================
       Bill DTO
       ========================= */

    public static record Bill(
            double totalConsumedKwh,
            double totalSoldKwh,
            double sellingPricePerKwh,
            double totalAmount
    ) {}
}