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

        // cheapest sellers first
        sellers.sort(Comparator.comparingDouble(EnergySnapshot::getSellingPrice));

        // buyers who can pay more first
        buyers.sort((a, b) -> Double.compare(b.getCostPrice(), a.getCostPrice()));

        int i = 0, j = 0;
        while(i < buyers.size() && j < sellers.size())
        {
            EnergySnapshot buyer = buyers.get(i);
            EnergySnapshot seller = sellers.get(j);
            if (buyer.deficit() <= 1e-9)
            {
                i++;
                continue;
            }
            else if(seller.surplus() <= 1e-9)
            {
                j++;
                continue;
            }

            if (seller.getSellingPrice() > buyer.getCostPrice()) break;

            double tradeAmount = Math.min(seller.surplus(), buyer.deficit());

            seller.sell(tradeAmount);
            buyer.buy(tradeAmount);
        }
        for(; i < buyers.size(); i++)
        {
            grid.buyFromGrid(buyers.get(i).deficit());
        }
        for(; j < sellers.size(); j++)
        {
            grid.sellToGrid(sellers.get(j).surplus());
        }
    }
}
