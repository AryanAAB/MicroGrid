package org.example.microgrid.grid;

public class Grid
{
    private final double buyPrice;
    private final double sellPrice;

    public Grid(double buyPrice, double sellPrice)
    {
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public double getBuyPrice()
    {
        return buyPrice;
    }

    public double getSellPrice()
    {
        return sellPrice;
    }
}
