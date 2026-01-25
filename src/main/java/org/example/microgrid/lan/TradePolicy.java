package org.example.microgrid.Lan;

import org.example.microgrid.house.House;
import org.example.microgrid.Grid.Grid;

import java.util.*;

public class TradePolicy {

    /* ---------- rolling market state ---------- */

    private final TreeSet<BuyerNode> buyers = new TreeSet<>();
    private final TreeSet<SellerNode> sellers = new TreeSet<>();

    private final Map<String, Double> consumption = new HashMap<>();
    private final Map<String, Double> production = new HashMap<>();

    private final Map<String, Boolean> isBuyer = new HashMap<>();
    private final Map<String, Boolean> isSeller = new HashMap<>();

    private final Map<String, BuyerNode> buyerRef = new HashMap<>();
    private final Map<String, SellerNode> sellerRef = new HashMap<>();

    /* ---------- called once per 15 min ---------- */

    /**
     * Register a new house in the rolling market state with default values.
     * @param house the House to register
     */
    public void registerHouse(House house) {
        String id = house.getHouseId();

        // initialize physical state
        consumption.put(id, 0.0);
        production.put(id, 0.0);

        // default role: buyer
        isBuyer.put(id, true);
        isSeller.put(id, false);

        // remove any stale references (safety)
        BuyerNode oldBuyer = buyerRef.remove(id);
        if (oldBuyer != null) {
            buyers.remove(oldBuyer);
        }

        SellerNode oldSeller = sellerRef.remove(id);
        if (oldSeller != null) {
            sellers.remove(oldSeller);
        }

        // add default buyer node with zero demand
        BuyerNode node = new BuyerNode(
                id,
                house.getCostPrice(),
                0.0
        );

        buyers.add(node);
        buyerRef.put(id, node);
    }

    public void runMarketCycle(LAN lan) {

        Grid grid = lan.getGrid();

        /* STEP 1: refresh production / consumption */
        for (House h : lan.getHouses()) {
            consumption.put(h.getHouseId(), h.getIntervalConsumption());
            production.put(h.getHouseId(), h.getIntervalProduction());
        }

        /* STEP 2: role transitions */
        for (House h : lan.getHouses()) {

            String id = h.getHouseId();
            double net = consumption.get(id) - production.get(id);

            boolean nowSeller = net < 0;
            boolean nowBuyer = !nowSeller;

            isBuyer.putIfAbsent(id, true);
            isSeller.putIfAbsent(id, false);

            if (isBuyer.get(id) && nowSeller) {
                buyers.remove(buyerRef.get(id));
                buyerRef.remove(id);
                isBuyer.put(id, false);
                isSeller.put(id, true);
            }

            if (isSeller.get(id) && nowBuyer) {
                sellers.remove(sellerRef.get(id));
                sellerRef.remove(id);
                isSeller.put(id, false);
                isBuyer.put(id, true);
            }
        }

        /* STEP 3: update order books */
        for (House h : lan.getHouses()) {

            String id = h.getHouseId();

            if (isBuyer.get(id)) {
                double demand = consumption.get(id) - production.get(id);
                BuyerNode old = buyerRef.get(id);
                BuyerNode neu =
                        new BuyerNode(id, h.getCostPrice(), demand);

                if (!neu.equals(old)) {
                    if (old != null) buyers.remove(old);
                    buyers.add(neu);
                    buyerRef.put(id, neu);
                }
            }

            if (isSeller.get(id)) {
                double supply = production.get(id) - consumption.get(id);
                SellerNode old = sellerRef.get(id);
                SellerNode neu =
                        new SellerNode(id, h.getSellingPrice(), supply);

                if (!neu.equals(old)) {
                    if (old != null) sellers.remove(old);
                    sellers.add(neu);
                    sellerRef.put(id, neu);
                }
            }
        }

        /* STEP 4 + 5: market clearing */
        clearMarket(lan, grid);

        /* STEP 6: reset interval stats */
        for (House h : lan.getHouses()) {
            h.resetIntervalStats();
        }
    }

    /* ---------- window-based clearing ---------- */

    private void clearMarket(LAN lan, Grid grid) {

        Iterator<BuyerNode> bit = buyers.iterator();
        Iterator<SellerNode> sit = sellers.iterator();

        BuyerNode lb = bit.hasNext() ? bit.next() : null;
        BuyerNode rb = lb;

        SellerNode ls = sit.hasNext() ? sit.next() : null;
        SellerNode rs = ls;

        double pd1 = 0, ps2 = 0;
        double b1 = 0, b2 = 0;
        int buyerWindowSize = 0, sellerWindowSize = 0;

        while (lb != null && ls != null) {

            while (rb != null &&
                    rb.costPrice == lb.costPrice &&
                    rb.demand == lb.demand) {
                pd1 += rb.demand;
                buyerWindowSize++;
                rb = bit.hasNext() ? bit.next() : null;
            }

            while (rs != null &&
                    rs.sellingPrice == ls.sellingPrice &&
                    rs.supply == ls.supply) {
                ps2 += rs.supply;
                sellerWindowSize++;
                rs = sit.hasNext() ? sit.next() : null;
            }

            double price = (lb.costPrice + ls.sellingPrice) / 2.0;

            if (pd1 <= ps2) {
                b1 += pd1 * price;
                b2 -= pd1 * price;
                ps2 -= pd1;
                pd1 = 0;

                BuyerNode it = lb;
                while (it != rb) {
                    lan.addBill(it.houseId, b1/buyerWindowSize);
                    it = bit.hasNext() ? bit.next() : null;
                }
                b1 = 0;
                buyerWindowSize = 0;
                lb = rb;

            } else {
                b1 += ps2 * price;
                b2 -= ps2 * price;
                pd1 -= ps2;
                ps2 = 0;

                SellerNode it = ls;
                while (it != rs) {
                    lan.addBill(it.houseId, b2/sellerWindowSize);
                    it = sit.hasNext() ? sit.next() : null;
                }
                b2 = 0;
                sellerWindowSize = 0;
                ls = rs;
            }
        }

        if (lb != null) {
            BuyerNode it = lb;
            while (it != rb) {
                lan.addBill(
                        it.houseId,
                        b1/buyerWindowSize + (pd1/buyerWindowSize) * grid.getGridSellPrice()
                );
                it = bit.hasNext() ? bit.next() : null;
            }
        }

        if (ls != null) {
            SellerNode it = ls;
            while (it != rs) {
                lan.addBill(
                        it.houseId,
                        b2/sellerWindowSize - (ps2/sellerWindowSize) * grid.getGridBuyPrice()
                );
                it = sit.hasNext() ? sit.next() : null;
            }
        }

        while (bit.hasNext()) {
            BuyerNode b = bit.next();
            lan.addBill(
                    b.houseId,
                    b.demand * grid.getGridSellPrice()
            );
        }

        while (sit.hasNext()) {
            SellerNode s = sit.next();
            lan.addBill(
                    s.houseId,
                    -s.supply * grid.getGridBuyPrice()
            );
        }
    }
}