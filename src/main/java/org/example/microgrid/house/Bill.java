package org.example.microgrid.house;

public class Bill
{
    private double netEnergyFromGrid = 0;
    private double earnings = 0;

    public void addEnergy(double energy)
    {
        netEnergyFromGrid += energy;
    }

    public void addEarnings(double earnings)
    {
        this.earnings += earnings;
    }

    public double getNetEnergyFromGrid()
    {
        return netEnergyFromGrid;
    }

    public double getEarnings()
    {
        return earnings;
    }
}
