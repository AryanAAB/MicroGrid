package org.example.microgrid.lan;

import org.example.microgrid.house.House;
import org.example.microgrid.registry.MeterRegistry;

import java.util.HashMap;
import java.util.Map;

public class LANManager {

    private final Map<String, LAN> lans = new HashMap<>();
    private final MeterRegistry meterRegistry;

    public LANManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void registerLAN(String lanId, LAN lan) {
        lans.put(lanId, lan);
    }

    public LAN assignHouse(House house) {

        String lanId = meterRegistry.getLanId(house.getMeterId());

        if (lanId == null) {
            throw new RuntimeException("No LAN mapping found for meter: " + house.getMeterId());
        }

        LAN lan = lans.get(lanId);

        if (lan == null) {
            throw new RuntimeException("LAN not registered: " + lanId);
        }

        lan.addHouse(house);
        return lan;
    }
}
