package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;

import java.util.Comparator;
import java.util.List;

public class TradePolicy
{
    public static void match(
            List<EnergySnapshot> sellers,
            List<EnergySnapshot> buyers,
            Grid grid
    )
    {
        // Sellers: cheapest energy first
        sellers.sort(Comparator.comparingDouble(EnergySnapshot::getSellingPrice));

        // Buyers: highest willingness-to-pay first
        buyers.sort((a, b) -> Double.compare(b.getCostPrice(), a.getCostPrice()));

        int bi = 0; // buyer pointer
        int si = 0; // seller pointer
        while (bi < buyers.size() && si < sellers.size())
        {
            EnergySnapshot lb = buyers.get(bi);   // leading buyer (highest price)
            EnergySnapshot ls = sellers.get(si);  // leading seller (lowest price)

            // Stop clearing if prices no longer cross
            if (lb.getCostPrice() < ls.getSellingPrice()) break;

            // Group all buyers with the same cost in 1 window
            int bj = bi;
            double totalDemand = 0.0;

            while (bj < buyers.size()
                    && buyers.get(bj).getCostPrice() == lb.getCostPrice())
            {
                // Only count buyers that still need energy
                double d = buyers.get(bj).deficit();
                if (d > 1e-9)
                {
                    totalDemand += d;
                }
                bj++;
            }

            //Group all sellers with the SAME selling price
            int sj = si;
            double totalSupply = 0.0;

            while (sj < sellers.size()
                    && sellers.get(sj).getSellingPrice() == ls.getSellingPrice())
            {

                // Only count sellers that still have surplus
                double s = sellers.get(sj).surplus();
                if (s > 1e-9)
                {
                    totalSupply += s;
                }
                sj++;
            }

            // Nothing meaningful to trade in these windows
            if (totalDemand <= 1e-9)
            {
                bi = bj;
                continue;
            }
            else if (totalSupply <= 1e-9)
            {
                si = sj;
                continue;
            }

            double traded = Math.min(totalDemand, totalSupply);

            double remaining = traded;
            for (int k = bi; k < bj && remaining > 1e-9; k++)
            {
                EnergySnapshot b = buyers.get(k);
                double d = b.deficit();

                if (d <= 1e-9) continue;

                // Proportional share of this buyer in the price window
                double share = (d / totalDemand) * traded;

                // Cannot buy more than remaining deficit
                double actual = Math.min(share, d);

                b.buy(actual);
                remaining -= actual;
            }

            remaining = traded;
            for (int k = si; k < sj && remaining > 1e-9; k++)
            {
                EnergySnapshot s = sellers.get(k);
                double sup = s.surplus();

                if (sup <= 1e-9) continue;

                // Proportional share of this seller in the price window
                double share = (sup / totalSupply) * traded;

                // Cannot sell more than remaining surplus
                double actual = Math.min(share, sup);

                s.sell(actual);
                remaining -= actual;
            }

            //If buyers were fully satisfied, move to next buyer window
            //Otherwise, move to next seller window
            if (totalDemand <= totalSupply)
                bi = bj;
            else
                si = sj;
        }

        for (int i = bi; i < buyers.size(); i++)
        {
            double d = buyers.get(i).deficit();
            if (d > 1e-9)
            {
                grid.buyFromGrid(d);
            }
        }

        for (int j = si; j < sellers.size(); j++)
        {
            double s = sellers.get(j).surplus();
            if (s > 1e-9)
            {
                grid.sellToGrid(s);
            }
        }
    }
}
