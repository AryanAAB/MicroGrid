package org.example.microgrid.lan;

import java.util.*;

public class TradePolicy {

    public static void match(
            List<EnergySnapshot> sellers,
            List<EnergySnapshot> buyers
    ) {
        sellers.sort(Comparator.comparingDouble(s -> s.sellingPrice));
        buyers.sort(Comparator.comparingDouble(b -> b.costPrice));

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
            System.out.println(
                    "GRID USED | Buyer: " + buyer.houseId +
                    " | Units: " + buyer.deficit()
            );
            break;
        }
    }
}

       
    }
}
