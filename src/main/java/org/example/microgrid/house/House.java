package org.example.microgrid.house;

import org.example.microgrid.meter.Meter;
import org.example.microgrid.meter.MeterSimulator;

public class House {

    /* =========================
       Identity
       ========================= */
    private final String houseId;

    /* =========================
       Prices (currency / kWh)
       ========================= */
    private volatile double costPrice;
    private volatile double sellingPrice;

    /* =========================
       Daily stats
       ========================= */
    private double dailyConsumptionKwh = 0.0;
    private double dailyProductionKwh = 0.0;

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
    ) {
        this.houseId = id;
        this.meter = new MeterSimulator(id, initialConsumption, initialProduction);
        setCostPrice(costPrice);
        setSellingPrice(sellingPrice);
    }

    /* =========================
       Setters
       ========================= */
    public void setProduction(double production) {
        if (production < 0) throw new IllegalArgumentException("Production cannot be negative");
        meter.setPeakSolarKw(production);
    }

    public void setConsumption(double consumption) {
        if (consumption < 0) throw new IllegalArgumentException();
        meter.setDemandKw(consumption);
    }

    public void setCostPrice(double costPrice) {
        if (costPrice < 0) throw new IllegalArgumentException();
        this.costPrice = costPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        if (sellingPrice < 0) throw new IllegalArgumentException();
        this.sellingPrice = sellingPrice;
    }

    /* =========================
       Getters
       ========================= */
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

    /* =========================
       Simulation step
       ========================= */
    public void step() {
        meter.readEnergy();
        dailyConsumptionKwh += meter.getRawDemandEnergy();
        dailyProductionKwh += meter.getRawSolarEnergy();
    }

    public void resetDailyStats() {
        dailyConsumptionKwh = 0.0;
        dailyProductionKwh = 0.0;
    }
}
