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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GridPanel extends JPanel
{
    // ---- Layout constants ----
    private static final int HOUSE_WIDTH = 60;
    private static final int HOUSE_HEIGHT = 90;

    private static final int X_GAP = 30;
    private static final int Y_GAP = 40;

    private static final int START_X = 90;
    private static final int START_Y = 30;

    // ---- State ----
    private final LAN lan;
    private final List<HouseView> views = new ArrayList<>();
    private final Map<String, Rectangle> houseBounds = new HashMap<>();
    private Consumer<House> onSelect;

    public GridPanel(LAN lan)
    {
        this.lan = lan;
        setBackground(Color.WHITE);

        // Mouse selection
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                for (HouseView v : views)
                {
                    if (v.contains(e.getPoint()))
                    {
                        if (onSelect != null)
                            onSelect.accept(v.house());
                        repaint();
                        return;
                    }
                }
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
        houseBounds.put(house.getHouseId(), r);
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
            houseBounds.put(views.get(i).house().getHouseId(),
                    new Rectangle(p.x, p.y, HOUSE_WIDTH, HOUSE_HEIGHT));
        }

        repaint();
    }

    private Point p2pHub()
    {
        return new Point(getWidth() - 60, getHeight() / 2);
    }

    private Point gridPoint()
    {
        return new Point(40, getHeight() / 2);
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

        // reset stroke to default for houses
        g2.setStroke(new BasicStroke(1.0f));

        for (HouseView v : views)
            v.draw(g2);       // layer 3: houses on top

        g2.dispose();
    }
}