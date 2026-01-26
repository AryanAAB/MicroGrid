package org.example.microgrid.lan;

import java.util.ArrayList;
import java.util.List;

public class Bill
{
    private final List<P2PTrade> p2pBuys = new ArrayList<>();
    private final List<P2PTrade> p2pSells = new ArrayList<>();

    private double gridImported;
    private double gridExported;

    // ---------- P2P ----------
    public void addP2PBuy(double energy, double price)
    {
        if (energy > 1e-9)
            p2pBuys.add(new P2PTrade(energy, price));
    }

    public void addP2PSell(double energy, double price)
    {
        if (energy > 1e-9)
            p2pSells.add(new P2PTrade(energy, price));
    }

    // ---------- Grid ----------
    public void addGridImport(double energy)
    {
        gridImported += energy;
    }

    public void addGridExport(double energy)
    {
        gridExported += energy;
    }

    // ---------- Costs ----------
    public double getP2PCost()
    {
        return p2pBuys.stream().mapToDouble(P2PTrade::getValue).sum();
    }

    public double getP2PRevenue()
    {
        return p2pSells.stream().mapToDouble(P2PTrade::getValue).sum();
    }

    public double getNetBill(double gridBuyPrice, double gridSellPrice)
    {
        double cost = getP2PCost() - getP2PRevenue();

        if(gridExported >= gridImported)
        {
            cost -= (gridExported - gridImported) * gridSellPrice;
        }
        else
        {
            cost += (gridImported - gridExported) * gridBuyPrice;
        }

        return cost;
    }
}
