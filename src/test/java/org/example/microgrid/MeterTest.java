package org.example.microgrid;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.meter.MeterSimulator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class MeterTest
{
    @Test
    public void testConstructorAndSetters()
    {
        // Valid constructor
        MeterSimulator meter = new MeterSimulator("testMeter", 0.5, 3.0);
        assertEquals(0.0, meter.getImportEnergy(), 0.001); // initially 0
        assertEquals(0.0, meter.getExportEnergy(), 0.001); // initially 0

        // Invalid constructor
        assertThrows(IllegalArgumentException.class, () -> new MeterSimulator("m1", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new MeterSimulator("m2", 1, -1));

        // Setters
        meter.setDemandKw(0.3);
        meter.setPeakSolarKw(4.0);
    }

    @Test
    public void testPowerAndEnergy()
    {
        MeterSimulator meter = new MeterSimulator("sim1", 0.5, 2.0);

        // Simulate 24 hours in steps
        int steps = (int) (Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS); // 1-min steps
        List<Double> importList = new ArrayList<>();
        List<Double> exportList = new ArrayList<>();
        List<Double> demand = new ArrayList<>();
        List<Double> solar = new ArrayList<>();
        List<Double> timeList = new ArrayList<>();

        double totalDemand = 0.0;
        double totalSolar = 0.0;
        double fractionOfDay =
                Constants.STEP_TO_SECONDS / (double) Constants.SEC_IN_DAY;

        Instant start = Instant.now();
        Instant now  = start;

        for (int i = 0; i < 2 * steps; i++)
        {
            meter.readEnergy(now, (i % steps) * fractionOfDay);

            double importPower = meter.getImportEnergy();
            double exportPower = meter.getExportEnergy();
            double rawDemand = meter.getRawDemandEnergy();
            double rawSolar = meter.getRawSolarEnergy();
            Instant time = meter.getReadingTimestamp();

            totalDemand += rawDemand;
            totalSolar += rawSolar;
            importList.add(importPower);
            exportList.add(exportPower);
            demand.add(rawDemand);
            solar.add(rawSolar);

            timeList.add(1.0 * Duration.between(start, time).toSeconds() / Constants.SEC_IN_HOUR);

            now = now.plusSeconds((long)(Constants.STEP_TO_SECONDS));
        }

        // After simulation, energy should be positive
        assertTrue(meter.getImportEnergy() > 0);
        assertTrue(meter.getExportEnergy() >= 0);

        // Plot the results
        plotEnergyGraph(timeList, importList, exportList, demand, solar, totalDemand, totalSolar);
    }

    private void plotEnergyGraph(List<Double> time, List<Double> importEnergy, List<Double> exportEnergy,
                                 List<Double> rawDemand, List<Double> rawSolar, double totalDemand, double totalSolar)
    {
        XYSeries importSeries = new XYSeries("Import Energy (kWh)");
        XYSeries exportSeries = new XYSeries("Export Energy (kWh)");

        for (int i = 0; i < time.size(); i++)
        {
            importSeries.add(time.get(i), importEnergy.get(i));
            exportSeries.add(time.get(i), exportEnergy.get(i));
        }

        XYSeriesCollection dataset1 = new XYSeriesCollection();
        dataset1.addSeries(importSeries);
        dataset1.addSeries(exportSeries);

        JFreeChart chart1 = ChartFactory.createXYLineChart(
                "Simulated Meter Energy",
                "Time (hours)",
                "Energy (kWh)",
                dataset1,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        XYPlot plot = chart1.getXYPlot();
        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
        renderer1.setSeriesPaint(0, Color.RED);
        renderer1.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer1.setSeriesPaint(1, Color.BLUE);
        renderer1.setSeriesStroke(1, new BasicStroke(2.0f));
        plot.setRenderer(renderer1);

        XYSeries demandSeries = new XYSeries("Raw Demand Energy (kWh), total = " + totalDemand);
        XYSeries solarSeries = new XYSeries("Raw Solar Energy (kWh), total = " + totalSolar);
        for (int i = 0; i < time.size(); i++)
        {
            demandSeries.add(time.get(i), rawDemand.get(i));
            solarSeries.add(time.get(i), rawSolar.get(i));
        }

        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(demandSeries);
        dataset2.addSeries(solarSeries);

        JFreeChart chart2 = ChartFactory.createXYLineChart(
                "Raw Demand & Solar Energy",
                "Time (hours)",
                "Energy (kWh)",
                dataset2,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        XYPlot plot2 = chart2.getXYPlot();
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
        renderer2.setSeriesPaint(0, Color.MAGENTA);
        renderer2.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer2.setSeriesPaint(1, Color.ORANGE);
        renderer2.setSeriesStroke(1, new BasicStroke(2.0f));
        plot2.setRenderer(renderer2);

        // Display chart in a window
        JFrame frame = new JFrame("Meter Simulator Graph");

        // Use DISPOSE so closing window does not kill JVM
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 1));
        frame.add(new ChartPanel(chart1));
        frame.add(new ChartPanel(chart2));
        frame.pack();
        frame.setVisible(true);

        // Use CountDownLatch to block test until window closes
        CountDownLatch latch = new CountDownLatch(1);
        frame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e)
            {
                latch.countDown(); // release the latch when window is closed
            }
        });

        try
        {
            latch.await(); // wait here until window is closed
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
