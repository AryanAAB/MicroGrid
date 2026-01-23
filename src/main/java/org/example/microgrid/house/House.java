package org.example.microgrid.house;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.meter.MeterSimulator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class House {

    /* =========================
       Energy Rates (kW)
       ========================= */
    private double production;   // kW
    private double consumption;  // kW

    /* =========================
       Prices (currency / kWh)
       ========================= */
    private double costPrice;
    private double sellingPrice;

    /* =========================
       Meter
       ========================= */
    private final MeterSimulator meter;

    /* =========================
       Scheduler
       ========================= */
    private final ScheduledExecutorService scheduler;

    /* =========================
       Constructor
       ========================= */
    public House(
            MeterSimulator meter,
            double initialProduction,
            double initialConsumption,
            double costPrice,
            double sellingPrice
    ) {
        this.meter = meter;

        this.production = 0.0;
        this.consumption = 0.0;

        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;

        setProduction(initialProduction);
        setConsumption(initialConsumption);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(
                meter::readEnergy,
                0,
                Constants.STEP_TO_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /* =========================
       Setters
       ========================= */

    public synchronized void setProduction(double production) {
        if (production < 0)
            throw new IllegalArgumentException("Production cannot be negative");

        this.production = production;
        meter.setPeakExport(production);
    }

    public synchronized void setConsumption(double consumption) {
        if (consumption < 0)
            throw new IllegalArgumentException("Consumption cannot be negative");

        this.consumption = consumption;
        meter.setAverageDemandKw(consumption);
    }

    /* =========================
       Getters
       ========================= */

    public double getProduction() {
        return production;
    }

    public double getConsumption() {
        return consumption;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public MeterSimulator getMeter() {
        return meter;
    }

    /* =========================
       Shutdown
       ========================= */

    public void shutdown() {
        scheduler.shutdownNow();
    }
}