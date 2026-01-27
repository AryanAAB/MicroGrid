package org.example.microgrid.lan;

import org.example.microgrid.house.House;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public interface TradePolicy
{
    public abstract void trade(LAN lan, List<EnergySnapshot> haveSurplus, List<EnergySnapshot> haveDeficit);
    public abstract EnergySnapshot getEnergySnapshot(LAN lan, House house);
}
