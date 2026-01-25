package org.example.microgrid.Lan;

import java.util.Objects;

public class SellerNode implements Comparable<SellerNode> {

    final String houseId;
    final double sellingPrice;
    final double supply;

    SellerNode(String houseId, double sellingPrice, double supply) {
        this.houseId = houseId;
        this.sellingPrice = sellingPrice;
        this.supply = supply;
    }

    @Override
    public int compareTo(SellerNode o) {
        int c = Double.compare(this.sellingPrice, o.sellingPrice); // SP asc
        if (c != 0) return c;

        c = Double.compare(o.supply, this.supply);                 // supply desc
        if (c != 0) return c;

        return this.houseId.compareTo(o.houseId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SellerNode)) return false;
        SellerNode s = (SellerNode) o;
        return houseId.equals(s.houseId)
                && sellingPrice == s.sellingPrice
                && supply == s.supply;
    }

    @Override
    public int hashCode() {
        return Objects.hash(houseId, sellingPrice, supply);
    }
}