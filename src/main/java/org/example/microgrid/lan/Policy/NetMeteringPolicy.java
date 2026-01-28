package org.example.microgrid.lan.Policy;

import org.example.microgrid.house.House;
import org.example.microgrid.lan.EnergySnapshot;
import org.example.microgrid.lan.LAN;

import java.util.List;

public class NetMeteringPolicy implements TradePolicy
{
    @Override
    public void trade(LAN lan, List<EnergySnapshot> haveSurplus, List<EnergySnapshot> haveDeficit)
    {
    }

    @Override
    public EnergySnapshot getEnergySnapshot(LAN lan, House house)
    {
        lan.getBill(house.getHouseId()).addGridExport(house.getIntervalProduction());
        lan.getBill(house.getHouseId()).addGridImport(house.getIntervalConsumption());

        return new EnergySnapshot(house.getHouseId(), 0, 0, house.getSellingPrice(), house.getCostPrice());
    }
}
