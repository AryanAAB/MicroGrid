package org.example.microgrid.sim;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.lan.LAN;
import org.example.microgrid.lan.Policy.NetMeteringPolicy;
import org.example.microgrid.lan.Policy.NetP2PPolicy;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SimulationFrame extends JFrame
{
    // Top LAN and Grid (NetMeteringPolicy)
    private final LAN topLAN;
    private final GridPanel topGridPanel;

    // Bottom LAN and Grid (NetP2PPolicy)
    private final LAN bottomLAN;
    private final GridPanel bottomGridPanel;

    private final JButton startBtn = new JButton("Start");

    private Timer simTimer;
    private ZonedDateTime currentTime = ZonedDateTime.of(
            LocalDate.now(),
            LocalTime.MIDNIGHT,
            ZoneId.systemDefault()
    );
    private int stepCounter = 0;
    private static final double FRACTION_OF_DAY_PER_STEP = 1.0 / (24 * 60);
    private static final int STEP_MS = 10;
    private static final int MAX_STEPS = (int) (Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS);

    private int counter = 1;
    private long countDays = 1;

    private final JLabel timeLabel = new JLabel();
    private final JLabel dayLabel = new JLabel();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public SimulationFrame()
    {
        super("Dual Microgrid Simulation");

        // --- Initialize Top Grid and LAN ---
        Grid topGrid = new Grid(10, 3);
        topLAN = new LAN(topGrid, new NetMeteringPolicy());
        topGridPanel = new GridPanel(topLAN);

        // --- Initialize Bottom Grid and LAN ---
        Grid bottomGrid = new Grid(10, 3);
        bottomLAN = new LAN(bottomGrid, new NetP2PPolicy());
        bottomGridPanel = new GridPanel(bottomLAN);

        // Labels for policies
        JLabel topPolicyLabel = new JLabel("Top Grid: Net Metering", SwingConstants.CENTER);
        topPolicyLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel bottomPolicyLabel = new JLabel("Bottom Grid: P2P + Net Metering", SwingConstants.CENTER);
        bottomPolicyLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Create panels combining label + grid panel
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(topPolicyLabel, BorderLayout.NORTH);
        topContainer.add(topGridPanel, BorderLayout.CENTER);

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.add(bottomPolicyLabel, BorderLayout.NORTH);
        bottomContainer.add(bottomGridPanel, BorderLayout.CENTER);

        // Buttons for adding houses
        JButton addConsumerBtn = new JButton("Add Consumer");
        JButton addProsumerBtn = new JButton("Add Prosumer(Low Production)");
        JButton addProducerBtn = new JButton("Add Prosumer(High Production)");

        addConsumerBtn.addActionListener(e -> addConsumer());
        addProsumerBtn.addActionListener(e -> addProsumer());
        addProducerBtn.addActionListener(e -> addProducer());

        startBtn.addActionListener(e -> startStopSimulation());

        JLabel topBuyLabel = new JLabel("Top Grid Buy: ₹" + topGrid.buyPrice());
        JLabel topSellLabel = new JLabel("Top Grid Sell: ₹" + topGrid.sellPrice());

        updateTimeLabel();

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(topBuyLabel);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(topSellLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(addConsumerBtn);
        topPanel.add(addProsumerBtn);
        topPanel.add(addProducerBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(startBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(timeLabel);
        topPanel.add(dayLabel);

        // Split pane for top and bottom grids
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContainer, bottomContainer);
        splitPane.setResizeWeight(0.5); // equally divide space
        splitPane.setDividerSize(5);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }


    private void updateTimeLabel()
    {
        LocalTime localTime = LocalTime.ofInstant(currentTime.toInstant(), ZoneId.systemDefault());

        timeLabel.setText("Time: " + localTime.format(TIME_FORMATTER));
        dayLabel.setText("Day: " + countDays);
    }

    private void simulationStep()
    {
        // Advance both LANs
        topLAN.step(currentTime.toInstant(), stepCounter * FRACTION_OF_DAY_PER_STEP);
        bottomLAN.step(currentTime.toInstant(), stepCounter * FRACTION_OF_DAY_PER_STEP);

        stepCounter++;

        if (stepCounter % 15 == 0)
        {
            topLAN.runMarketCycle();
            bottomLAN.runMarketCycle();
        }

        currentTime = currentTime.plusSeconds(60); // advance 1 minute

        if(currentTime.getHour() == 0 && currentTime.getMinute() == 0)
            countDays++;

        updateTimeLabel();
        topGridPanel.repaint();
        bottomGridPanel.repaint();

        stepCounter %= MAX_STEPS;
    }

    private void addConsumer()
    {
        House h1 = new House(
                "C" + counter,
                0.0, 1.5, 0.3, 8.0, 6.0
        );
        House h2 = new House(
                "C" + counter++,
                0.0, 1.5, 0.3, 8.0, 6.0
        );

        topLAN.addHouse(h1);
        topGridPanel.addHouse(h1);
        bottomLAN.addHouse(h2);
        bottomGridPanel.addHouse(h2);
    }

    private void addProducer()
    {
        House h1 = new House(
                "P" + counter,
                2.0, 0.2, 0.0, 5.0, 8.0
        );

        House h2 = new House(
                "P" + counter++,
                2.0, 0.2, 0.0, 5.0, 8.0
        );

        topLAN.addHouse(h1);
        topGridPanel.addHouse(h1);
        bottomLAN.addHouse(h2);
        bottomGridPanel.addHouse(h2);
    }

    private void addProsumer()
    {
        House h1 = new House(
                "PS" + counter,
                2.0, 0.4, 0.3, 6.0, 5.0
        );
        House h2 = new House(
                "PS" + counter++,
                2.0, 0.4, 0.3, 6.0, 5.0
        );

        topLAN.addHouse(h1);
        topGridPanel.addHouse(h1);
        bottomLAN.addHouse(h2);
        bottomGridPanel.addHouse(h2);
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
        SwingUtilities.invokeLater(SimulationFrame::new);
    }
}