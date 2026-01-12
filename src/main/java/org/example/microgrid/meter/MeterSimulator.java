package org.example.microgrid.meter;

import org.example.microgrid.constants.Constants;

import java.time.Duration;
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
    private volatile double peakExportKw;

    //state variables
    private double importEnergyKwh = 0.0;
    private double exportEnergyKwh = 0.0;

    private final Instant startTimestamp;
    private Instant simulatedTimestamp;

    public MeterSimulator(String meterId, double averageDemandKw, double peakExportKw) throws IllegalArgumentException
    {
        super(meterId);
        setAverageDemandKw(averageDemandKw);
        setPeakExport(peakExportKw);

        this.startTimestamp = Instant.now();
        this.simulatedTimestamp = startTimestamp;
    }

    public void setAverageDemandKw(double averageDemandKw) throws IllegalArgumentException
    {
        if (averageDemandKw < 0)
            throw new IllegalArgumentException("Average demand must be >= 0");

        this.averageDemandKw = averageDemandKw;
    }

    public void setPeakExport(double peakExportKw) throws IllegalArgumentException
    {
        if (peakExportKw < 0)
            throw new IllegalArgumentException("Peak export must be >= 0");

        this.peakExportKw = peakExportKw;
    }

    @Override
    public synchronized void readEnergy()
    {
        double deltaHours = Constants.STEP_TO_SECONDS / Constants.SEC_IN_HOUR;

        double importPower = importPowerKw();
        double exportPower = exportPowerKw();

        importEnergyKwh = importPower * deltaHours;
        exportEnergyKwh = exportPower * deltaHours;
        simulatedTimestamp = simulatedTimestamp.plusSeconds(
                (long) Constants.STEP_TO_SECONDS
        );
    }

    private double importPowerKw()
    {
        double t = fractionOfDay();

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

    private double exportPowerKw()
    {
        double t = fractionOfDay();

        if (t < DAY_START || t > DAY_END)
            return 0.0;

        double daylightT = (t - DAY_START) / (DAY_END - DAY_START);
        double solar = Math.sin(Math.PI * daylightT);

        double supply = peakExportKw * Math.max(0.0, solar);

        double noise =
                peakExportKw * SOLAR_NOISE_FACTOR *
                        (random.nextDouble() - 0.5);

        return Math.min(Math.max(0.0, supply + noise), peakExportKw);
    }

    private double fractionOfDay()
    {
        double hours =
                Duration.between(startTimestamp, simulatedTimestamp)
                        .toSeconds() / Constants.SEC_IN_HOUR;

        return (hours % Constants.HOUR_IN_DAY) / Constants.HOUR_IN_DAY;
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
    public Instant getReadingTimestamp()
    {
        return simulatedTimestamp;
    }
}
