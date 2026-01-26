package org.example.microgrid.lan;

/**
 * @param energy kWh
 * @param price  price per kWh
 */
public record P2PTrade(double energy, double price)
{
    public double getValue()
    {
        return energy * price;
    }
}
