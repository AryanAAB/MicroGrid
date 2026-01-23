package org.example.microgrid.house;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.meter.MeterSimulator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class House
{

    /* =========================
       Prices (currency / kWh)
       ========================= */
    private volatile double costPrice;
    private volatile double sellingPrice;

    /* =========================
       Meter
       ========================= */
    private final MeterSimulator meter;

    /* =========================
       Scheduler
       ========================= */
    private final ScheduledExecutorService scheduler;
    private volatile boolean started = false;

    /* =========================
       Constructor
       ========================= */
    public House(
            MeterSimulator meter,
            double initialProduction,
            double initialConsumption,
            double costPrice,
            double sellingPrice
    )
    {
        this.meter = meter;

        setCostPrice(costPrice);
        setSellingPrice(sellingPrice);
        setProduction(initialProduction);
        setConsumption(initialConsumption);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /* =========================
       Setters
       ========================= */

    public void setProduction(double production)
    {
        if (production < 0)
            throw new IllegalArgumentException("Production cannot be negative");

        meter.setPeakSolarKw(production);
    }

    public void setConsumption(double consumption)
    {
        if (consumption < 0)
            throw new IllegalArgumentException("Consumption cannot be negative");

        meter.setDemandKw(consumption);
    }

    public void setCostPrice(double costPrice)
    {
        if (costPrice < 0) throw new IllegalArgumentException("CostPrice cannot be negative");
        this.costPrice = costPrice;
    }

    public void setSellingPrice(double sellingPrice)
    {
        if (sellingPrice < 0) throw new IllegalArgumentException("SellingPrice cannot be negative");
        this.sellingPrice = sellingPrice;
    }

    /* =========================
       Getters
       ========================= */

    public double getExportEnergy()
    {
        return meter.getExportEnergy();
    }

    public double getImportEnergy()
    {
        return meter.getImportEnergy();
    }

    public double getCostPrice()
    {
        return costPrice;
    }

    public double getSellingPrice()
    {
        return sellingPrice;
    }

    public MeterSimulator getMeter()
    {
        return meter;
    }

    /* =========================
       Scheduler
       ========================= */

    public synchronized void start()
    {
        if (started)
            return;

        scheduler.scheduleAtFixedRate(
                meter::readEnergy,
                0,
                Math.max(1, (long) Constants.STEP_TO_SECONDS),
                TimeUnit.SECONDS
        );

        started = true;
    }

    public synchronized void shutdown()
    {
        if (!scheduler.isShutdown())
        {
            scheduler.shutdownNow();
            started = false;
        }
    }
}