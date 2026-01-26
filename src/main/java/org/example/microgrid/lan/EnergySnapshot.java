package org.example.microgrid.lan;

public class EnergySnapshot
{
    private final String houseId;
    private final double surplus;      // kWh
    private final double deficit;      // kWh
    private final double sellingPrice; // ₹/kWh
    private final double costPrice;    // ₹/kWh

    public EnergySnapshot(
            String houseId,
            double surplus,
            double deficit,
            double sellingPrice,
            double costPrice
    )
    {
        this.houseId = houseId;
        this.surplus = surplus;
        this.deficit = deficit;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
    }

    public String getHouseId()
    {
        return houseId;
    }

    public double surplus()
    {
        return surplus;
    }

    public double deficit()
    {
        return deficit;
    }

    public double getSellingPrice()
    {
        return sellingPrice;
    }

    public double getCostPrice()
    {
        return costPrice;
    }

    public boolean isSeller()
    {
        return surplus >= deficit;
    }

    public boolean isBuyer()
    {
        return !isSeller();
    }
}
