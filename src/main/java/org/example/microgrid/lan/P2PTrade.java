package org.example.microgrid.lan;

public class P2PTrade
{
    private final double energy;   // kWh
    private final double price;    // price per kWh

    public P2PTrade(double energy, double price)
    {
        this.energy = energy;
        this.price = price;
    }

    public double getEnergy() { return energy; }
    public double getPrice() { return price; }

    public double getValue()
    {
        return energy * price;
    }
}
