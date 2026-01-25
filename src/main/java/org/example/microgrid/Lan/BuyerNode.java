package org.example.microgrid.Lan;

import java.util.Objects;

public class BuyerNode implements Comparable<BuyerNode> {

    final String houseId;
    final double costPrice;
    final double demand;

    BuyerNode(String houseId, double costPrice, double demand) {
        this.houseId = houseId;
        this.costPrice = costPrice;
        this.demand = demand;
    }

    @Override
    public int compareTo(BuyerNode o) {
        int c = Double.compare(o.costPrice, this.costPrice); // CP desc
        if (c != 0) return c;

        c = Double.compare(this.demand, o.demand);           // demand asc
        if (c != 0) return c;

        return this.houseId.compareTo(o.houseId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BuyerNode)) return false;
        BuyerNode b = (BuyerNode) o;
        return houseId.equals(b.houseId)
                && costPrice == b.costPrice
                && demand == b.demand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(houseId, costPrice, demand);
    }
}