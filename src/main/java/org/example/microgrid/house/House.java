package org.example.microgrid.house;

import org.example.microgrid.meter.Meter;
import org.example.microgrid.meter.MeterSimulator;

public class House {

    private final String houseId;

    private volatile double costPrice;
    private volatile double sellingPrice;

    // daily stats
    private double dailyConsumptionKwh = 0.0;
    private double dailyProductionKwh = 0.0;

    // interval stats (15 min)
    private double intervalConsumptionKwh = 0.0;
    private double intervalProductionKwh = 0.0;

    private final Meter meter;

    public House(
            String id,
            double initialProduction,
            double initialConsumption,
            double costPrice,
            double sellingPrice
    ) {
        this.houseId = id;
        this.meter = new MeterSimulator(id, initialConsumption, initialProduction);
        setCostPrice(costPrice);
        setSellingPrice(sellingPrice);
    }

    public void setProduction(double production) {
        if (production < 0) throw new IllegalArgumentException();
        meter.setPeakSolarKw(production);
    }

    public void setConsumption(double consumption) {
        if (consumption < 0) throw new IllegalArgumentException();
        meter.setDemandKw(consumption);
    }

    public void setCostPrice(double costPrice) {
        if (costPrice <= 0) throw new IllegalArgumentException();
        this.costPrice = costPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        if (sellingPrice <= 0) throw new IllegalArgumentException();
        this.sellingPrice = sellingPrice;
    }

    public String getHouseId() {
        return houseId;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public double getDailyConsumption() {
        return dailyConsumptionKwh;
    }

    public double getDailyProduction() {
        return dailyProductionKwh;
    }

    public double getIntervalConsumption() {
        return intervalConsumptionKwh;
    }

    public double getIntervalProduction() {
        return intervalProductionKwh;
    }

    public void step() {
        meter.readEnergy();

        double demand = meter.getRawDemandEnergy();
        double solar = meter.getRawSolarEnergy();

        dailyConsumptionKwh += demand;
        dailyProductionKwh += solar;

        intervalConsumptionKwh += demand;
        intervalProductionKwh += solar;
    }

    public void resetIntervalStats() {
        intervalConsumptionKwh = 0.0;
        intervalProductionKwh = 0.0;
    }

    public void resetDailyStats() {
        dailyConsumptionKwh = 0.0;
        dailyProductionKwh = 0.0;
    }
}
