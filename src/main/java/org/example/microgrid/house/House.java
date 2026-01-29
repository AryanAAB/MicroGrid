package org.example.microgrid.house;

import org.example.microgrid.meter.Meter;
import org.example.microgrid.meter.MeterSimulator;

import java.time.Instant;

public class House
{
    private final String houseId;

    private volatile double costPrice;
    private volatile double sellingPrice;
    private volatile double sellThreshold;

    private double intervalConsumptionKwh = 0.0;
    private double intervalProductionKwh = 0.0;

    private final Meter meter;

    public House(
            String id,
            double initialProduction,
            double initialConsumption,
            double sellThreshold,
            double costPrice,
            double sellingPrice
    )
    {
        this.houseId = id;
        this.meter = new MeterSimulator(id, initialConsumption, initialProduction);

        setCostPrice(costPrice);
        setSellingPrice(sellingPrice);
        setThreshold(sellThreshold);
    }

    public void setPeakSolarKw(double production)
    {
        if (production < 0) throw new IllegalArgumentException("Production cannot be negative");
        meter.setPeakSolarKw(production);
    }

    public void setConsumption(double consumption)
    {
        if (consumption < 0) throw new IllegalArgumentException("Consumption cannot be negative");
        meter.setDemandKw(consumption);
    }

    public void setThreshold(double threshold)
    {
        if (threshold < 0) throw new IllegalArgumentException("Threshold cannot be negative");
        this.sellThreshold = threshold;
    }

    public void setCostPrice(double costPrice)
    {
        if (costPrice <= 0) throw new IllegalArgumentException("Cost price must be positive");
        this.costPrice = costPrice;
    }

    public void setSellingPrice(double sellingPrice)
    {
        if (sellingPrice <= 0) throw new IllegalArgumentException("Selling price must be positive");
        this.sellingPrice = sellingPrice;
    }

    public String getHouseId()
    {
        return houseId;
    }

    public double getCostPrice()
    {
        return costPrice;
    }

    public double getSellingPrice()
    {
        return sellingPrice;
    }

    public double getIntervalConsumption()
    {
        return intervalConsumptionKwh;
    }

    public double getIntervalProduction()
    {
        return intervalProductionKwh;
    }

    public double getTotalConsumption()
    {
        return meter.getImportEnergy();
    }

    public double getTotalProduction()
    {
        return meter.getExportEnergy();
    }

    public double getSellThreshold()
    {
        return sellThreshold;
    }

    public String getMeterId()
    {
        return meter.getMeterId();
    }

    public void step(Instant timestamp, double fractionOfDay)
    {
        meter.readEnergy(timestamp, fractionOfDay);
        intervalConsumptionKwh += meter.getRawDemandEnergy();
        intervalProductionKwh += meter.getRawSolarEnergy();
    }

    public void resetIntervalStats()
    {
        intervalConsumptionKwh = 0.0;
        intervalProductionKwh = 0.0;
    }
}