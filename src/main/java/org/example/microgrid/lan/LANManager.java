package org.example.microgrid.lan;

import org.example.microgrid.house.House;
import org.example.microgrid.registry.MeterRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LANManager
{

    private final Map<String, LAN> lans = new HashMap<>();

    public LANManager()
    {
        initializeMapping();
    }

    public void initializeMapping(){
        // Initialize Firebase users.
        // Every user has a meterID.
        // The first letter of the meter ID is the ID of the newly created lan.
        // Constructor of House is     public House( String id <use same as meter id of this user in firebase>, double initialProduction <set 2.5>, double initialConsumption <set 0.5>, double sellThreshold <set 0.2>, double costPrice <use buyBidPrice of this user from firebase>, double sellingPrice <use sellingPrice from firebase>)
        // Constructor of Lan is just public LAN()
    }

    public void runMarketCycles()
    {
        lans.forEach((key, lan) -> lan.runMarketCycle());
    }

    public void step(Instant timestamp, double fractionOfDay)
    {
        lans.forEach((key, lan) -> lan.step(timestamp, fractionOfDay));
    }

    public ArrayList<Bill> getBills()
    {
        return lans.values()
                .stream()
                .flatMap(lan -> lan.getBills().stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
