package org.example.microgrid.lan;

/**
 * @param surplus      kWh
 * @param deficit      kWh
 * @param sellingPrice ₹/kWh
 * @param costPrice    ₹/kWh
 */
public record EnergySnapshot(String houseId, double surplus, double deficit, double sellingPrice, double costPrice)
{
    public boolean isSeller()
    {
        return surplus >= deficit;
    }

    public boolean isBuyer()
    {
        return !isSeller();
    }
}
