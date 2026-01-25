package org.example.microgrid.Lan;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarketScheduler {

    private final LAN lan;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public MarketScheduler(LAN lan) {
        this.lan = lan;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                lan::runMarketCycle,
                0,
                15,
                TimeUnit.MINUTES
        );
    }

    public void stop() {
        scheduler.shutdown();
    }
}