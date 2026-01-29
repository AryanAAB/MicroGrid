package org.example.microgrid.sim;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.lan.LAN;
import org.example.microgrid.lan.Policy.NetP2PPolicy;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class SimulationFrame extends JFrame
{
    private final LAN lan;
    private final GridPanel gridPanel;

    private final JButton startBtn = new JButton("Start");

    private Timer simTimer;
    private ZonedDateTime currentTime = ZonedDateTime.of(
            LocalDate.now(),             // today
            LocalTime.MIDNIGHT,          // 12:00 AM
            ZoneId.systemDefault()       // local timezone (e.g., IST)
    );
    private int stepCounter = 0;

    private static final double FRACTION_OF_DAY_PER_STEP = 1.0 / (24 * 60);
    private static final int STEP_MS = 10;
    private static final int MAX_STEPS = (int) (Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS);

    private int counter = 1;

    // NEW: JLabel for showing time
    private final JLabel timeLabel = new JLabel();

    // Formatter for displaying time
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public SimulationFrame()
    {
        super("Microgrid Simulation");

        Grid grid = new Grid(10, 3);
        lan = new LAN(grid, new NetP2PPolicy());

        gridPanel = new GridPanel(lan);

        JButton addConsumerBtn = new JButton("Add Consumer");
        JButton addProsumerBtn = new JButton("Add Prosumer");
        JButton addProducerBtn = new JButton("Add Producer");

        addConsumerBtn.addActionListener(e -> addConsumer());
        addProsumerBtn.addActionListener(e -> addProsumer());
        addProducerBtn.addActionListener(e -> addProducer());

        startBtn.addActionListener(e -> startStopSimulation());

        JLabel buyPriceLabel = new JLabel("Buying from Grid: ₹" + grid.buyPrice());
        JLabel sellPriceLabel = new JLabel("Selling to Grid: ₹" + grid.sellPrice());

        // NEW: initial time display
        updateTimeLabel();

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(buyPriceLabel);
        top.add(Box.createHorizontalStrut(15)); // spacing
        top.add(sellPriceLabel);
        top.add(Box.createHorizontalStrut(30));
        top.add(addConsumerBtn);
        top.add(addProsumerBtn);
        top.add(addProducerBtn);
        top.add(startBtn);
        top.add(Box.createHorizontalStrut(30));
        top.add(timeLabel); // ADD time label

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void updateTimeLabel()
    {
        LocalTime localTime = LocalTime.ofInstant(currentTime.toInstant(), ZoneId.systemDefault());
        timeLabel.setText("Time: " + localTime.format(TIME_FORMATTER));
    }

    private void simulationStep()
    {
        lan.step(currentTime.toInstant(), stepCounter * FRACTION_OF_DAY_PER_STEP);
        stepCounter++;

        if (stepCounter % 15 == 0)
            lan.runMarketCycle();

        currentTime = currentTime.plusSeconds(60); // advance 1 minute
        updateTimeLabel(); // update time display
        gridPanel.repaint();

        stepCounter %= MAX_STEPS;
    }

    private void addConsumer()
    {
        House h = new House(
                "C" + counter++,
                0.0,
                1.5,
                0.3,
                8.0,
                6.0
        );

        lan.addHouse(h);
        gridPanel.addHouse(h);
    }

    private void addProducer()
    {
        House h = new House(
                "P" + counter++,
                2.0,
                0.2,
                0.0,
                5.0,
                8.0
        );

        lan.addHouse(h);
        gridPanel.addHouse(h);
    }

    private void addProsumer()
    {
        House h = new House(
                "PS" + counter++,
                2.0,
                1.0,
                0.2,
                6.0,
                5.0
        );

        lan.addHouse(h);
        gridPanel.addHouse(h);
    }

    private void startStopSimulation()
    {
        if (simTimer != null && simTimer.isRunning())
        {
            simTimer.stop();
            startBtn.setText("Start");
        }
        else
        {
            simTimer = new Timer(STEP_MS, e -> simulationStep());
            simTimer.start();
            startBtn.setText("Stop");
        }
    }

    public static void main(String[] args)
    {
        new SimulationFrame();
    }
}