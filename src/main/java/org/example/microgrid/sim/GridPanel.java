package org.example.microgrid.sim;

import org.example.microgrid.house.House;
import org.example.microgrid.lan.Bill;
import org.example.microgrid.lan.LAN;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GridPanel extends JPanel
{
    // ---- Layout constants ----
    private static final int HOUSE_WIDTH = 60;
    private static final int HOUSE_HEIGHT = 90;

    private static final int X_GAP = 30;
    private static final int Y_GAP = 40;

    private static final int START_X = 90;
    private static final int START_Y = 30;

    private final List<HouseView> views = new ArrayList<>();

    public GridPanel(LAN lan)
    {
        // ---- State ----
        setBackground(Color.WHITE);

        // Mouse selection
        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                for (HouseView v : views)
                {
                    if (v.contains(e.getPoint()))
                    {
                        Bill bill = lan.getBill(v.house().getHouseId());
                        if (bill != null)
                        {
                            double netBill = bill.getNetBill();
                            String revenueOrCost = netBill <= 0 ? "Revenue" : "Cost";

                            String tooltip = String.format(
                                    "<html>House: %s<br/>" +
                                            "P2P Buy: %.2f kWh (₹%.2f)<br/>" +
                                            "P2P Sell: %.2f kWh (₹%.2f)<br/>" +
                                            "Grid Import: %.2f kWh<br/>" +
                                            "Grid Export: %.2f kWh<br/>" +
                                            "%s: ₹%.2f</html>",
                                    v.house().getHouseId(),
                                    bill.getP2PBuyAmount(), bill.getP2PCost(),
                                    bill.getP2PSellAmount(), bill.getP2PRevenue(),
                                    bill.getGridImported(),
                                    bill.getGridExported(),
                                    revenueOrCost,
                                    Math.abs(netBill) // always show positive value
                            );
                            setToolTipText(tooltip);
                            return;
                        }
                    }
                }
                setToolTipText(null);
            }
        });

        // Reflow houses when panel is resized
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                relayout();
            }
        });
    }

    public void addHouse(House house)
    {
        Point p = computePosition(views.size());

        // Check vertical overflow
        if (p.y + HOUSE_HEIGHT > getHeight())
        {
            JOptionPane.showMessageDialog(
                    this,
                    "No more space to place houses.",
                    "Grid Full",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Rectangle r = new Rectangle(p.x, p.y, HOUSE_WIDTH, HOUSE_HEIGHT);
        views.add(new HouseView(house, p));
        repaint();
    }

    // ---- Layout logic ----
    private void relayout()
    {
        for (int i = 0; i < views.size(); i++)
        {
            Point p = computePosition(i);
            views.get(i).setPosition(p);
        }

        repaint();
    }

    private Point computePosition(int index)
    {
        int panelWidth = getWidth();

        // Happens before first layout
        if (panelWidth <= 0)
            panelWidth = 800;

        int usableWidth = panelWidth - 2 * START_X;
        int cols = Math.max(
                1,
                usableWidth / (HOUSE_WIDTH + X_GAP)
        );

        int col = index % cols;
        int row = index / cols;

        int x = START_X + col * (HOUSE_WIDTH + X_GAP);
        int y = START_Y + row * (HOUSE_HEIGHT + Y_GAP);

        return new Point(x, y);
    }

    // ---- Painting ----
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setStroke(new BasicStroke(1.0f));

        for (HouseView v : views)
            v.draw(g2);

        g2.dispose();
    }
}