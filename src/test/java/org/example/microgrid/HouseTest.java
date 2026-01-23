package org.example.microgrid;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.house.House;
import org.example.microgrid.meter.Meter;
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
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class HouseTest
{
    @Test
    public void initializesStateCorrectly()
    {
        House house = new House(
                "H1",
                3.0,
                2.0,
                8.0,
                6.0
        );

        assertEquals(8.0, house.getCostPrice());
        assertEquals(6.0, house.getSellingPrice());
        assertEquals(0.0, house.getData().demand());
        assertEquals(0.0, house.getData().supply());
    }

    @Test
    public void rejectNegativeValues()
    {
        House house = new House("H2", 2.0, 2.0, 5.0, 4.0);

        assertThrows(IllegalArgumentException.class,
                () -> house.setProduction(-1.0));

        assertThrows(IllegalArgumentException.class,
                () -> house.setConsumption(-1.0));

        assertThrows(IllegalArgumentException.class,
                () -> house.setCostPrice(-1.0));

        assertThrows(IllegalArgumentException.class,
                () -> house.setSellingPrice(-1.0));
    }

    @Test
    public void makesEnergyCumulative()
    {
        House house = new House(
                "H3",
                4.0,   // production
                2.0,   // consumption
                6.0,
                4.0
        );

        double prevImport = 0.0;
        double prevExport = 0.0;

        for (int i = 0; i < 20; i++)
        {
            house.step();

            Meter.MeterData data = house.getData();

            double currImport = data.demand();
            double currExport = data.supply();

            assertTrue(currImport >= prevImport,
                    "Import energy must never decrease");

            assertTrue(currExport >= prevExport,
                    "Export energy must never decrease");

            prevImport = currImport;
            prevExport = currExport;
        }
    }

    @Test
    public void meterSnapshotIsNonNullAndHasValidTimestamp()
    {
        House house = new House(
                "H6",
                3.0,
                3.0,
                5.0,
                5.0
        );

        house.step();

        Meter.MeterData data = house.getData();

        assertNotNull(data);
        assertTrue(data.demand() >= 0);
        assertTrue(data.supply() >= 0);
        assertTrue(data.timestamp() > 0);
    }

    @Test
    public void plotNetEnergyOverOneDay()
    {
        House house = new House(
                "H-PLOT",
                2,
                0.5,
                7.0,
                5.0
        );

        // Simulate 48 hours in steps
        int steps = (int) (2 * Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS); // 1-min steps

        XYSeries netEnergySeries = new XYSeries("Net Energy (Export - Import)");

        long startTimestamp = -1;

        for (int i = 0; i < steps; i++)
        {
            house.step();

            Meter.MeterData data = house.getData();

            if (startTimestamp < 0)
                startTimestamp = data.timestamp();

            double hours =
                    (data.timestamp() - startTimestamp)
                            / Constants.SEC_IN_HOUR;

            double netEnergy = data.supply() - data.demand();

            netEnergySeries.add(hours, netEnergy);
        }

        plotNetEnergy(netEnergySeries);
    }

    private void plotNetEnergy(XYSeries netEnergySeries)
    {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(netEnergySeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "House Net Energy (1 Day)",
                "Time (hours)",
                "Net Energy (kWh)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setSeriesShapesVisible(0, false);

        plot.setRenderer(renderer);

        JFrame frame = new JFrame("House Net Energy Plot");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);

        CountDownLatch latch = new CountDownLatch(1);
        frame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e)
            {
                latch.countDown();
            }
        });

        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
