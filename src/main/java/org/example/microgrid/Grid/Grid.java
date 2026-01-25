package org.example.microgrid.Grid;

public class Grid {
    private final double gridBuyPrice;   // x
    private final double gridSellPrice;  // y

    public Grid(double gridBuyPrice, double gridSellPrice) {
        this.gridBuyPrice = gridBuyPrice;
        this.gridSellPrice = gridSellPrice;
    }

    public double getGridBuyPrice() {
        return this.gridBuyPrice;
    }

    public double getGridSellPrice() {
        return gridSellPrice;
    }

    public double buyFromGrid(double units) {
        return units * gridBuyPrice;
    }

    public double sellToGrid(double units) {
        return units * gridSellPrice;
    }
}
