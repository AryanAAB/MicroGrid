package org.example.microgrid.lan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LANManager
{

    private final Map<String, LAN> lans = new HashMap<>(); // Lan Id -> LAN Object
    private final Map<House, String> houseLanMap = new HashMap<>(); // House Object -> LAN id

    public LANManager()
    {
        initializeMapping();
    }

    public void initializeMapping()
    {
        // Initialize Firebase users.
        // Every user has a meterID in firebase.
        // The first letter of the meter ID is the ID of the newly created lan.
        // Constructor of House is public House( String id <use same as meter id of this user in firebase>, double initialProduction <set 2.5>, double initialConsumption <set 0.5>, double sellThreshold <set 0.2>, double costPrice <use buyBidPrice of this user from firebase>, double sellingPrice <use sellingPrice from firebase>)
        // Constructor of Lan is just public LAN()
        // You add a House to a lan using public void addHouse(House house) of the lan object.
    }

    public void runMarketCycles() // called every t2
    {
        lans.forEach((key, lan) -> lan.runMarketCycle());
    }

    //called every t1
    public void step(Instant timestamp, double fractionOfDay) //timestamp can be <currentTime.toInstant()> // use private static final double FRACTION_OF_DAY_PER_STEP = 1.0 / (24 * 60); and reset steps at the end of the day (24hrs worth of steps)
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

    public void addUser(){
        //Add user when new registration happens. You can add parameters to it.
    }

    public void uploadToFirebase(){ //called at the end of t3
        //Do this.getBills and get a List of bill objects.
        //bill objects do not have users, maybe try to query it seperately per user from the lan using lan.getBill(houseID)

        /*
            The bill object has:
    public double getGridImported() //use to fill the specific user's energyConsumed in firebase.

    public double getGridExported() //use to fill the specific user's energySold in firebase.

    public double getP2PRevenue() // use this to fill the specific user's gridSavings in firebase.

    public double getNetBill() // use this to fill the specific user's earnings in firebase.
        */
        
    }
}
