package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;

import java.util.Comparator;
import java.util.List;

public class TradePolicy
{
    public static void match(
            LAN lan,
            List<EnergySnapshot> sellers,
            List<EnergySnapshot> buyers,
            Grid grid
    )
    {
        sellers.sort(
                Comparator.comparingDouble(EnergySnapshot::getSellingPrice)
                        .thenComparing(
                                Comparator.comparingDouble(EnergySnapshot::surplus).reversed()
                        )
        );

        // Buyers: highest willingness-to-pay first followed by min deficit
        buyers.sort(
                Comparator.comparingDouble(EnergySnapshot::getCostPrice).reversed()
                        .thenComparingDouble(EnergySnapshot::deficit)
        );

        int bi = 0; // buyer pointer
        int si = 0; // seller pointer
        int bj = 0, sj = 0;
        double totalDemand = 0.0, totalSupply = 0.0, deltaBillBuyer = 0.0, deltaBillSeller = 0.0;
        while (bi < buyers.size() && si < sellers.size())
        {
            // slide the windows
            EnergySnapshot lb = buyers.get(bi);   // leading buyer (highest price)
            EnergySnapshot ls = sellers.get(si);  // leading seller (lowest price)

            while (bj < buyers.size()
                    && buyers.get(bj).getCostPrice() == lb.getCostPrice()
                    && buyers.get(bj).deficit() == lb.deficit()) {
                totalDemand += buyers.get(bj).deficit();
                bj++;
            }

            while (sj < sellers.size()
                    && sellers.get(sj).getSellingPrice() == ls.getSellingPrice()
                    && sellers.get(sj).surplus() == ls.surplus()) {
                totalSupply += sellers.get(sj).surplus();
                sj++;
            }
            // Stop clearing if prices no longer cross
            if (lb.getCostPrice() < ls.getSellingPrice()) break;

            double price = 0.5*(lb.getCostPrice() + ls.getSellingPrice());

            if (totalDemand <= totalSupply) {
                deltaBillBuyer += totalDemand*price;
                deltaBillSeller -= totalDemand*price;
                double len = bj-bi;
                while (bi < bj) {
                    lan.addBill(buyers.get(bi).getHouseId(), deltaBillBuyer/len);
                    bi++;
                }
                deltaBillBuyer = 0;
                totalSupply -= totalDemand;
                totalDemand = 0;
            }
            else {
                deltaBillSeller -= totalSupply*price;
                deltaBillBuyer += totalSupply*price;
                double len = sj-si;
                while (si < sj) {
                    lan.addBill(sellers.get(si).getHouseId(), deltaBillSeller/len);
                    si++;
                }
                deltaBillSeller = 0;
                totalDemand -= totalSupply;
                totalSupply = 0;
            }
        }
        int len_buyers = bj - bi;
        int len_sellers = sj-si;

        while (bi < bj) {
            lan.addBill(buyers.get(bi).getHouseId(), deltaBillBuyer/len_buyers + grid.buyFromGrid(totalDemand/len_buyers));
            bi++;
        }
        while (si < sj) {
            lan.addBill(sellers.get(si).getHouseId(), deltaBillSeller/len_sellers + grid.sellToGrid(totalSupply/len_sellers));
            si++;
        }
        for (int i = bi; i < buyers.size(); i++)
        {
            double d = buyers.get(i).deficit();
            if (d > 1e-9)
            {
                lan.addResidualDemand(buyers.get(i).getHouseId(), d);
            }
        }

        for (int j = si; j < sellers.size(); j++)
        {
            double s = sellers.get(j).surplus();
            if (s > 1e-9)
            {
                lan.addResidualSupply(sellers.get(j).getHouseId(), s);
            }
        }
    }
}
