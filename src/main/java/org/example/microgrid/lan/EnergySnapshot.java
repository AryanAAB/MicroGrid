package org.example.microgrid.lan;

public record EnergySnapshot(
        String houseId,
        double gridExport,
        double gridImport,
        double surplus,
        double deficit,
        double sellingPrice,
        double costPrice
)
{
    public boolean isSeller()
    {
        return surplus > 0;
    }

    public boolean isBuyer()
    {
        return !isSeller();
    }
}
