package org.example.microgrid.house;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.meter.Meter;
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
    private final Meter meter;

    /* =========================
       Constructor
       ========================= */
    public House(
            String id,
            double initialProduction,
            double initialConsumption,
            double costPrice,
            double sellingPrice
    )
    {
        this.meter = new MeterSimulator(id, initialConsumption, initialProduction);

        setCostPrice(costPrice);
        setSellingPrice(sellingPrice);
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

    public double getCostPrice()
    {
        return costPrice;
    }

    public double getSellingPrice()
    {
        return sellingPrice;
    }

    public void step()
    {
        meter.readEnergy();
    }

    public Meter.MeterData getData()
    {
        return meter.getMeterSnapshot();
    }
}