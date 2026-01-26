package org.example.microgrid.lan;

/**
 * @param surplus      kWh
 * @param deficit      kWh
 * @param sellingPrice ₹/kWh
 * @param costPrice    ₹/kWh
 */
public record EnergySnapshot(
        String houseId,
        double surplus,
        double deficit,
        double sellingPrice,
        double costPrice
) {
    public EnergySnapshot {
        boolean surplusPositive = surplus > 0;
        boolean deficitPositive = deficit > 0;

        //  both non-zero OR both zero → invalid state
        if (surplusPositive == deficitPositive) {
            throw new IllegalArgumentException(
                "Invalid EnergySnapshot for house " + houseId +
                ": exactly one of surplus or deficit must be > 0. " +
                "Found surplus=" + surplus + ", deficit=" + deficit
            );
        }
    }

    public boolean isSeller() {
        return surplus > 0;
    }

    public boolean isBuyer() {
        return deficit > 0;
    }
}
