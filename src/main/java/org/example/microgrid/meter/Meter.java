package org.example.microgrid.meter;

import java.time.Instant;

public abstract class Meter
{
    private final String meterId;

    public Meter(String meterId)
    {
        this.meterId = meterId;
    }

    public abstract void setDemandKw(double averageDemandKw) throws IllegalArgumentException;

    public abstract void setPeakSolarKw(double peakSolarKw) throws IllegalArgumentException;

    public abstract void readEnergy(Instant timestamp, double fractionOfDay);

    public abstract double getImportEnergy();

    public abstract double getExportEnergy();

    public abstract double getRawDemandEnergy();

    public abstract double getRawSolarEnergy();

    public abstract Instant getReadingTimestamp();

    public final String getMeterId()
    {
        return this.meterId;
    }

    public final MeterData getMeterSnapshot()
    {
        return new MeterData(getImportEnergy(), getExportEnergy(), getReadingTimestamp().getEpochSecond());
    }

    public static record MeterData(double demand, double supply, long timestamp)
    {
    }
}
