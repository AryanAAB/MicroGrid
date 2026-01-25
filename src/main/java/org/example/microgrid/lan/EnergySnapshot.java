package org.example.microgrid.lan;

public class EnergySnapshot {

    public final String houseId;
    public final double sellingPrice;
    public final double costPrice;

    private double remainingProduction;
    private double remainingConsumption;

    public EnergySnapshot(
            String houseId,
            double production,
            double consumption,
            double sellingPrice,
            double costPrice
    ) {
        this.houseId = houseId;
        this.remainingProduction = production;
        this.remainingConsumption = consumption;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
    }

    public boolean isSeller() {
        return remainingProduction > remainingConsumption;
    }

    public boolean isBuyer() {
        return remainingConsumption > remainingProduction;
    }

    public double surplus() {
        return Math.max(0, remainingProduction - remainingConsumption);
    }

    public double deficit() {
        return Math.max(0, remainingConsumption - remainingProduction);
    }

    public void sell(double units) {
        remainingProduction = Math.max(0, remainingProduction - units);
    }

    public void buy(double units) {
        remainingConsumption = Math.max(0, remainingConsumption - units);
    }
}
