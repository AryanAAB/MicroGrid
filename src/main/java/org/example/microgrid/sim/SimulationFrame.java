package org.example.microgrid.sim;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.lan.LAN;
import org.example.microgrid.lan.policy.NetMeteringPolicy;
import org.example.microgrid.lan.policy.NetP2PPolicy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class SimulationFrame extends JFrame
{
    // Top LAN and Grid (NetMeteringPolicy)
    private final LAN topLAN;
    private final ArrayList<House> topHouses = new ArrayList<>();
    private final GridPanel topGridPanel;

    // Bottom LAN and Grid (NetP2PPolicy)
    private final LAN bottomLAN;
    private final ArrayList<House> bottomHouses = new ArrayList<>();
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
    private static final int STEP_MS = 1;
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

        JLabel topBuyLabel = new JLabel("Grid Buy: ₹" + topGrid.buyPrice());
        JLabel topSellLabel = new JLabel("Grid Sell: ₹" + topGrid.sellPrice());

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
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                // Stop simulation if running
                if (simTimer != null && simTimer.isRunning())
                    simTimer.stop();

                // Print final results
                printFinalResults();

                // Now safely exit
                dispose();
                System.exit(0);
            }
        });


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

        if (currentTime.getHour() == 0 && currentTime.getMinute() == 0)
            countDays++;

        updateTimeLabel();
        topGridPanel.repaint();
        bottomGridPanel.repaint();

        stepCounter %= MAX_STEPS;
    }

    private double rand(double min, double max)
    {
        return min + Math.random() * (max - min);
    }

    private void register(House top, House bottom)
    {
        topHouses.add(top);
        bottomHouses.add(bottom);

        topLAN.addHouse(top);
        topGridPanel.addHouse(top);

        bottomLAN.addHouse(bottom);
        bottomGridPanel.addHouse(bottom);
    }

    private void addConsumer()
    {
        String id = "C" + counter++;

        double avgConsumption = rand(1.0, 2.5);
        double sellThreshold = rand(0.2, 0.5);
        double costPrice = rand(8.0, 9.0);
        double sellPrice = rand(6.0, 8.0);

        House top = new House(id, 0.0, avgConsumption, sellThreshold, costPrice, sellPrice);
        House bottom = new House(id, 0.0, avgConsumption, sellThreshold, costPrice, sellPrice);

        register(top, bottom);
    }

    private void addProducer()
    {
        String id = "P" + counter++;

        double peakSolar = rand(2, 3.0);
        double avgConsumption = rand(0.1, 0.4);
        double sellThreshold = rand(0.0, 0.2);
        double costPrice = rand(8.0, 9.0);
        double sellPrice = rand(6.0, 8.0);

        House top = new House(id,
                peakSolar,
                avgConsumption,
                sellThreshold,
                costPrice,
                sellPrice
        );

        House bottom = new House(id,
                peakSolar,
                avgConsumption,
                sellThreshold,
                costPrice,
                sellPrice
        );

        register(top, bottom);
    }

    private void addProsumer()
    {
        String id = "PR" + counter++;

        double peakSolar = rand(2.0, 2.5);
        double avgConsumption = rand(0.4, 0.5);
        double sellThreshold = 0.2;
        double costPrice = rand(8.0, 9.0);
        double sellPrice = rand(6.0, 8.0);

        House top = new House(id,
                peakSolar,
                avgConsumption,
                sellThreshold,
                costPrice,
                sellPrice
        );

        House bottom = new House(id,
                peakSolar,
                avgConsumption,
                sellThreshold,
                costPrice,
                sellPrice
        );

        register(top, bottom);
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

    private void printFinalResults()
    {
        System.out.println("\n===== SIMULATION ENDED =====");

        System.out.println("\n--- TOP GRID (Net Metering) ---");
        for (House h : topHouses)
        {
            double netAmount = topLAN.getBill(h.getHouseId()).getNetBill();
            double p2pNetAmount = bottomLAN.getBill(h.getHouseId()).getNetBill();

            System.out.printf(
                    "House %s : Net Metering %s ₹%.2f P2P %s ₹%.2f%n",
                    h.getHouseId(),
                    (netAmount <= 0 ? "Revenue" : "Cost"),
                    Math.abs(netAmount),
                    (netAmount <= 0 ? "Revenue" : "Cost"),
                    Math.abs(p2pNetAmount)
            );
        }

        System.out.println("==============================\n");
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(SimulationFrame::new);
    }
}