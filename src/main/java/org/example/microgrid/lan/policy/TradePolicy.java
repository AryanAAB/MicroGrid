package org.example.microgrid.lan.policy;

import org.example.microgrid.house.House;
import org.example.microgrid.lan.EnergySnapshot;
import org.example.microgrid.lan.LAN;

import java.util.List;

public interface TradePolicy
{
    public abstract void trade(LAN lan, List<EnergySnapshot> haveSurplus, List<EnergySnapshot> haveDeficit);
    public abstract EnergySnapshot getEnergySnapshot(LAN lan, House house);
}
