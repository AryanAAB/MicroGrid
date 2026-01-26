package org.example.microgrid.lan;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class TradePolicy
{
    private static final double EPS = 1e-9;

    public static void match(
            LAN lan,
            List<EnergySnapshot> sellers,
            List<EnergySnapshot> buyers
    )
    {
        // Sellers: Lowest Selling price first followed by max surplus
        sellers.sort(
                Comparator.comparingDouble(EnergySnapshot::sellingPrice)
                        .thenComparing(
                                Comparator.comparingDouble(EnergySnapshot::surplus).reversed()
                        )
        );

        // Buyers: highest willingness-to-pay first followed by min deficit
        buyers.sort(
                Comparator.comparingDouble(EnergySnapshot::costPrice).reversed()
                        .thenComparingDouble(EnergySnapshot::deficit)
        );

        int bi = 0; // buyer pointer
        int si = 0; // seller pointer
        int bj = 0, sj = 0;
        double totalDemand = 0.0, totalSupply = 0.0;

        while (bi < buyers.size() && si < sellers.size())
        {
            // slide the windows
            EnergySnapshot lb = buyers.get(bi);   // leading buyer (highest price)
            EnergySnapshot ls = sellers.get(si);  // leading seller (lowest price)

            while (bj < buyers.size()
                    && equals(buyers.get(bj).costPrice(), lb.costPrice())
                    && equals(buyers.get(bj).deficit(), lb.deficit()))
            {
                totalDemand += buyers.get(bj).deficit();
                bj++;
            }

            while (sj < sellers.size()
                    && equals(sellers.get(sj).sellingPrice(), ls.sellingPrice())
                    && equals(sellers.get(sj).surplus(), ls.surplus()))
            {
                totalSupply += sellers.get(sj).surplus();
                sj++;
            }
            // Stop clearing if prices no longer cross
            if (lb.costPrice() + EPS < ls.sellingPrice()) break;

            double price = 0.5 * (lb.costPrice() + ls.sellingPrice());
            double traded = Math.min(totalDemand, totalSupply);

            int buyerCount = bj - bi;
            int sellerCount = sj - si;

            if (buyerCount == 0 || sellerCount == 0)
                break;

            // allocate P2P trades
            applyP2PBuy(lan, buyers, bi, bj, traded / buyerCount, price);
            applyP2PSell(lan, sellers, si, sj, traded / sellerCount, price);

            totalDemand -= traded;
            totalSupply -= traded;

            if (totalDemand <= EPS) bi = bj;
            if (totalSupply <= EPS) si = sj;
        }

        // settle remaining window demand/supply with grid
        if (bi < bj && totalDemand > EPS)
            settleGridImport(lan, buyers, bi, bj, totalDemand);

        if (si < sj && totalSupply > EPS)
            settleGridExport(lan, sellers, si, sj, totalSupply);

        for (int i = bi; i < buyers.size(); i++)
        {
            double d = buyers.get(i).deficit();
            if (d > EPS)
                lan.getBill(buyers.get(i).houseId()).addGridImport(d);
        }

        for (int j = si; j < sellers.size(); j++)
        {
            double s = sellers.get(j).surplus();
            if (s > EPS)
                lan.getBill(sellers.get(j).houseId()).addGridExport(s);
        }
    }

    private static void applyP2PBuy(
            LAN lan, List<EnergySnapshot> buyers,
            int from, int to, double qty, double price
    )
    {
        IntStream.range(from, to)
                .mapToObj(buyers::get)
                .forEach(b ->
                        lan.getBill(b.houseId()).addP2PBuy(qty, price)
                );
    }

    private static void applyP2PSell(
            LAN lan, List<EnergySnapshot> sellers,
            int from, int to, double qty, double price
    )
    {
        IntStream.range(from, to)
                .mapToObj(sellers::get)
                .forEach(s ->
                        lan.getBill(s.houseId()).addP2PSell(qty, price)
                );
    }

    private static void settleGridImport(
            LAN lan, List<EnergySnapshot> buyers,
            int from, int to, double total
    )
    {
        double per = total / (to - from);
        IntStream.range(from, to)
                .mapToObj(buyers::get)
                .forEach(b ->
                        lan.getBill(b.houseId()).addGridImport(per)
                );
    }

    private static void settleGridExport(
            LAN lan, List<EnergySnapshot> sellers,
            int from, int to, double total
    )
    {
        double per = total / (to - from);
        IntStream.range(from, to)
                .mapToObj(sellers::get)
                .forEach(b ->
                        lan.getBill(b.houseId()).addGridExport(per));
    }

    private static boolean equals(double a, double b)
    {
        return Math.abs(a - b) < EPS;
    }
}
