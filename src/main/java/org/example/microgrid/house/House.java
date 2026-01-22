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
    private double currentConsumptionRate; // kW
    private double currentProductionRate;  // kW

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
    private Instant t1; // last total update
    private Instant t2; // last 5-min update

    /* =========================
       Scheduler
       ========================= */
    private final ScheduledExecutorService scheduler;

    /* =========================
       Constructor
       ========================= */
    public House(Meter meter) {
        this.meter = meter;

        // Initialize all numeric fields explicitly
        this.currentConsumptionRate = 0.0;
        this.currentProductionRate = 0.0;

        this.totalConsumption = 0.0;
        this.totalProduction = 0.0;

        this.consumptionLast5Min = 0.0;
        this.productionLast5Min = 0.0;

        Instant now = Instant.now();
        this.t1 = now;
        this.t2 = now;

        // Start 5-minute scheduler
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(
                this::pushFiveMinuteUpdateToMeter,
                FIVE_MINUTES_SECONDS,
                FIVE_MINUTES_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /* =========================
       Setters (USER ACTIONS)
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
       Core Accounting Logic
       ========================= */

    private synchronized void updateEnergyAccounting() {
        Instant now = Instant.now();

        double deltaT1Hours =
                Duration.between(t1, now).toMillis() / 3_600_000.0;

        double deltaT2Hours =
                Duration.between(t2, now).toMillis() / 3_600_000.0;

        // Update totals
        totalConsumption += currentConsumptionRate * deltaT1Hours;
        totalProduction  += currentProductionRate  * deltaT1Hours;

        // Update 5-minute buckets
        consumptionLast5Min += currentConsumptionRate * deltaT2Hours;
        productionLast5Min  += currentProductionRate  * deltaT2Hours;

        t1 = now;
        t2 = now;
    }

    /* =========================
       5-Minute Meter Push
       ========================= */

    private synchronized void pushFiveMinuteUpdateToMeter() {
        Instant now = Instant.now();

        double deltaHours =
                Duration.between(t2, now).toMillis() / 3_600_000.0;

        double cons =
                consumptionLast5Min + currentConsumptionRate * deltaHours;

        double prod =
                productionLast5Min + currentProductionRate * deltaHours;

        double avgConsumptionKw = cons / (FIVE_MINUTES_SECONDS / 3600.0);
        double avgProductionKw  = prod / (FIVE_MINUTES_SECONDS / 3600.0);

        // Expected method in Meter implementation
        meter.update(avgConsumptionKw, avgProductionKw);

        consumptionLast5Min = 0.0;
        productionLast5Min = 0.0;
        t2 = now;
    }

    /* =========================
       Getters (WITH UPDATE)
       ========================= */

    public synchronized double getTotalConsumption() {
        updateEnergyAccounting();
        return totalConsumption;
    }

    public synchronized double getTotalProduction() {
        updateEnergyAccounting();
        return totalProduction;
    }

    public synchronized double getCurrentConsumptionRate() {
        return currentConsumptionRate;
    }

    public synchronized double getCurrentProductionRate() {
        return currentProductionRate;
    }

    public synchronized double getConsumptionLast5Minutes() {
        return consumptionLast5Min;
    }

    public synchronized double getProductionLast5Minutes() {
        return productionLast5Min;
    }

    /* =========================
       Reset Logic
       ========================= */

    public synchronized void resetTotals() {
        totalConsumption = 0.0;
        totalProduction = 0.0;
        t1 = Instant.now();
    }

    /* =========================
       Cleanup
       ========================= */

    public void shutdown() {
        scheduler.shutdownNow();
    }
}