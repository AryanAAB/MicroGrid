package org.example.microgrid.meter;

import org.example.microgrid.constants.Constants;
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
        MeterSimulator meter = new MeterSimulator("testMeter", 5.0, 3.0);
        assertEquals(0.0, meter.getImportEnergy(), 0.001); // initially 0
        assertEquals(0.0, meter.getExportEnergy(), 0.001); // initially 0

        // Invalid constructor
        assertThrows(IllegalArgumentException.class, () -> new MeterSimulator("m1", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new MeterSimulator("m2", 1, -1));

        // Setters
        meter.setAverageDemandKw(7.0);
        meter.setPeakSolarKw(4.0);
    }

    @Test
    public void testPowerAndEnergy()
    {
        MeterSimulator meter = new MeterSimulator("sim1", 3, 5.0);

        // Simulate 24 hours in steps
        int steps = (int)(Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS); // 1-min steps
        List<Double> importList = new ArrayList<>();
        List<Double> exportList = new ArrayList<>();
        List<Double> timeList = new ArrayList<>();

        Instant now = meter.getReadingTimestamp();

        for (int i = 0; i < steps; i++)
        {
            meter.readEnergy();

            double importPower = meter.getImportEnergy();
            double exportPower = meter.getExportEnergy();
            Instant time = meter.getReadingTimestamp();

            importList.add(importPower);
            exportList.add(exportPower);
            timeList.add(1.0 * Duration.between(now, time).toSeconds() / Constants.SEC_IN_HOUR);
        }

        // After simulation, energy should be positive
        assertTrue(meter.getImportEnergy() > 0);
        assertTrue(meter.getExportEnergy() >= 0);

        // Plot the results
        plotEnergyGraph(timeList, importList, exportList);
    }

    private void plotEnergyGraph(List<Double> time, List<Double> importEnergy, List<Double> exportEnergy)
    {
        XYSeries importSeries = new XYSeries("Import Energy (kWh)");
        XYSeries exportSeries = new XYSeries("Export Energy (kWh)");

        for (int i = 0; i < time.size(); i++)
        {
            importSeries.add(time.get(i), importEnergy.get(i));
            exportSeries.add(time.get(i), exportEnergy.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(importSeries);
        dataset.addSeries(exportSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Simulated Meter Energy",
                "Time (hours)",
                "Energy (kWh)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        // Display chart in a window
        JFrame frame = new JFrame("Meter Simulator Graph");

        // Use DISPOSE so closing window does not kill JVM
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new ChartPanel(chart));
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
