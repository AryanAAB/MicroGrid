package org.example.microgrid.meter;

import org.example.microgrid.constants.Constants;

import java.time.Instant;
import java.util.Random;

public class MeterSimulator extends Meter
{
    // Demand curve parameters
    private static final double MORNING_PEAK_CENTER = 0.30; // ~7 AM
    private static final double EVENING_PEAK_CENTER = 0.75; // ~6 PM
    private static final double PEAK_WIDTH = 0.02;

    private static final double MORNING_PEAK_WEIGHT = 0.3;
    private static final double EVENING_PEAK_WEIGHT = 0.4;

    // Solar parameters
    private static final double DAY_START = 0.25; // 6 AM
    private static final double DAY_END = 0.75; // 6 PM

    // Noise scaling
    private static final double DEMAND_NOISE_FACTOR = 0.05; // 5%
    private static final double SOLAR_NOISE_FACTOR = 0.05; // 5%

    //random variables
    private final Random random = new Random();

    //user-configurable parameters
    private volatile double averageDemandKw;
    private volatile double peakSolarKw;

    //state variables
    private double importEnergyKwh = 0.0;
    private double exportEnergyKwh = 0.0;
    private double rawDemandKwh = 0.0;
    private double rawSolarKwh = 0.0;
    private Instant timestamp = null;

    public MeterSimulator(String meterId, double averageDemandKw, double peakSolarKw) throws IllegalArgumentException
    {
        super(meterId);
        setDemandKw(averageDemandKw);
        setPeakSolarKw(peakSolarKw);
    }

    @Override
    public void setDemandKw(double averageDemandKw) throws IllegalArgumentException
    {
        if (averageDemandKw < 0)
            throw new IllegalArgumentException("Average demand must be >= 0");

        this.averageDemandKw = averageDemandKw;
    }

    public void setPeakSolarKw(double peakSolarKw) throws IllegalArgumentException
    {
        if (peakSolarKw < 0)
            throw new IllegalArgumentException("Peak solar must be >= 0");

        this.peakSolarKw = peakSolarKw;
    }

    @Override
    public synchronized void readEnergy(Instant timestamp, double fractionOfDay)
    {
        if (fractionOfDay < 0 || fractionOfDay > 1)
            throw new IllegalArgumentException("fractionOfDay must be between 0 and 1");

        double deltaHours = Constants.STEP_TO_SECONDS / Constants.SEC_IN_HOUR;

        double demand = demandPowerKw(fractionOfDay);
        double solar = solarPowerKw(fractionOfDay);

        double selfConsumption = Math.min(demand, solar);
        double importPower = demand - selfConsumption;
        double exportPower = solar - selfConsumption;

        importEnergyKwh += importPower * deltaHours;
        exportEnergyKwh += exportPower * deltaHours;
        rawDemandKwh = demand * deltaHours;
        rawSolarKwh = solar * deltaHours;

        this.timestamp = timestamp;
    }

    private double demandPowerKw(double t)
    {
        double morningPeak =
                Math.exp(-Math.pow(t - MORNING_PEAK_CENTER, 2) / PEAK_WIDTH);

        double eveningPeak =
                Math.exp(-Math.pow(t - EVENING_PEAK_CENTER, 2) / PEAK_WIDTH);

        double variation =
                MORNING_PEAK_WEIGHT * morningPeak +
                        EVENING_PEAK_WEIGHT * eveningPeak;

        double noise =
                averageDemandKw * DEMAND_NOISE_FACTOR *
                        (random.nextDouble() - 0.5);

        return Math.max(0.0, averageDemandKw * (1 + variation) + noise);
    }

    private double solarPowerKw(double t)
    {
        if (t < DAY_START || t > DAY_END)
            return 0.0;

        double daylightT = (t - DAY_START) / (DAY_END - DAY_START);
        double solar = Math.sin(Math.PI * daylightT);

        double supply = peakSolarKw * Math.max(0.0, solar);

        double noise =
                peakSolarKw * SOLAR_NOISE_FACTOR *
                        (random.nextDouble() - 0.5);

        return Math.min(Math.max(0.0, supply + noise), peakSolarKw);
    }

    @Override
    public synchronized double getImportEnergy()
    {
        return importEnergyKwh;
    }

    @Override
    public synchronized double getExportEnergy()
    {
        return exportEnergyKwh;
    }

    @Override
    public synchronized double getRawDemandEnergy()
    {
        return rawDemandKwh;
    }

    @Override
    public synchronized double getRawSolarEnergy()
    {
        return rawSolarKwh;
    }

    @Override
    public Instant getReadingTimestamp()
    {
        return timestamp;
    }
}
