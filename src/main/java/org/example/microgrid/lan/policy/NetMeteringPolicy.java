package org.example.microgrid.lan.policy;

import org.example.microgrid.house.House;
import org.example.microgrid.lan.EnergySnapshot;
import org.example.microgrid.lan.LAN;

import java.util.List;

public class NetMeteringPolicy implements TradePolicy
{
    @Override
    public void trade(LAN lan, List<EnergySnapshot> haveSurplus, List<EnergySnapshot> haveDeficit)
    {
        exchangeWithGrid(lan, haveSurplus);
        exchangeWithGrid(lan, haveDeficit);
    }

    @Override
    public EnergySnapshot getEnergySnapshot(LAN lan, House house)
    {
        return new EnergySnapshot(house.getHouseId(), house.getIntervalProduction(), house.getIntervalConsumption(),
                0, 0, house.getSellingPrice(), house.getCostPrice());
    }

    private void exchangeWithGrid(LAN lan, List<EnergySnapshot> snapshots)
    {
        for (EnergySnapshot snap : snapshots)
        {
            lan.getBill(snap.houseId()).addGridImport(snap.gridImport());
            lan.getBill(snap.houseId()).addGridExport(snap.gridExport());
        }
    }
}
