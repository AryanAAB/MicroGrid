package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;
import java.util.Comparator;
import java.util.List;

public class TradePolicy {

    public static void match(
            List<EnergySnapshot> sellers,
            List<EnergySnapshot> buyers,
            Grid grid
    ) {

        // cheapest sellers first
        sellers.sort(Comparator.comparingDouble(s -> s.sellingPrice));

        // buyers who can pay more first
        buyers.sort((a, b) -> Double.compare(b.costPrice, a.costPrice));

        for (EnergySnapshot buyer : buyers) {

            while (buyer.deficit() > 0) {

                boolean matched = false;

                for (EnergySnapshot seller : sellers) {

                    if (seller.surplus() <= 0) continue;
                    if (seller.sellingPrice > buyer.costPrice) continue;

                    double tradeAmount =
                            Math.min(seller.surplus(), buyer.deficit());

                    seller.sell(tradeAmount);
                    buyer.buy(tradeAmount);
                    matched = true;

                    System.out.println(
                            "TRADE | Seller: " + seller.houseId +
                            " -> Buyer: " + buyer.houseId +
                            " | Units: " + tradeAmount +
                            " | Price: " + seller.sellingPrice
                    );
                    break;
                }

                if (!matched) {
                    double units = buyer.deficit();
                    double cost = grid.buyFromGrid(units);

                    System.out.println(
                            "GRID USED | Buyer: " + buyer.houseId +
                            " | Units: " + units +
                            " | Cost: " + cost
                    );
                    break;
                }
            }
        }
    }
}
