package org.example.microgrid;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.house.House;
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
import java.time.Instant;
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
                1.0,
                8.0,
                6.0
        );

        assertEquals(8.0, house.getCostPrice());
        assertEquals(6.0, house.getSellingPrice());
        assertEquals(0.0, house.getIntervalConsumption());
        assertEquals(0.0, house.getIntervalProduction());
    }

    @Test
    public void rejectNegativeValues()
    {
        House house = new House("H2", 2.0, 2.0, 1.0, 5.0, 4.0);

        assertThrows(IllegalArgumentException.class,
                () -> house.setPeakSolarKw(-1.0));

        assertThrows(IllegalArgumentException.class,
                () -> house.setConsumption(-1.0));

        assertThrows(IllegalArgumentException.class,
                () -> house.setCostPrice(-1.0));

        assertThrows(IllegalArgumentException.class,
                () -> house.setSellingPrice(-1.0));
    }

    @Test
    public void meterSnapshotIsNonNullAndHasValidTimestamp()
    {
        House house = new House(
                "H6",
                3.0,
                3.0,
                1.0,
                5.0,
                5.0
        );

        house.step(Instant.now(), 0);

        assertTrue(house.getIntervalConsumption() >= 0);
        assertTrue(house.getIntervalProduction() >= 0);
    }

    @Test
    public void plotNetEnergyOverOneDay()
    {
        House house = new House(
                "H-PLOT",
                2.0,
                0.5,
                0.2,
                5.0,
                6.0
        );

        // 48 hours, 1-minute steps
        int steps = (int) (Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS);

        XYSeries netEnergySeries = new XYSeries("Net Energy (Export - Import)");

        Instant start = Instant.now();
        Instant current = start;

        double fractionOfDay =
                Constants.STEP_TO_SECONDS / (double) Constants.SEC_IN_DAY;

        for (int i = 0; i < 2 * steps; i++)
        {
            house.step(current, (i % steps) * fractionOfDay);

            double hours =
                    (double) (current.getEpochSecond() - start.getEpochSecond())
                            / Constants.SEC_IN_HOUR;

            double netEnergy =
                    house.getIntervalProduction()
                            - house.getIntervalConsumption();

            netEnergySeries.add(hours, netEnergy);

            // reset every 15-minute interval
            if ((i + 1) % (15 * 60 / Constants.STEP_TO_SECONDS) == 0)
            {
                house.resetIntervalStats();
            }

            current = current.plusSeconds((long) (Constants.STEP_TO_SECONDS));
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
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
