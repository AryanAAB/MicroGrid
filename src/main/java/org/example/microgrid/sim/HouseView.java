package org.example.microgrid.sim;

import org.example.microgrid.house.House;

import java.awt.*;

public class HouseView
{
    private static final int WIDTH = 60;
    private static final int HEIGHT = 50;
    private static final int ROOF_HEIGHT = 25;
    private final House house;
    private Point pos;

    public HouseView(House house, Point pos)
    {
        this.house = house;
        this.pos = pos;
    }

    public void draw(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // ---- color logic (simple but intuitive) ----
        // greener → more solar, redder → more consumer
        float solar = (float) house.getTotalProduction();
        float load = (float) house.getTotalConsumption();

        float red = (solar + load <= 1e-5) ? 0 : load / (solar + load);
        float green = (solar + load <= 1e-5) ? 0 : solar / (solar + load);

        Color bodyColor = new Color(red, green, 0.4f);

        int x = pos.x;
        int y = pos.y;

        // ---- roof ----
        Polygon roof = new Polygon(
                new int[]{x, x + WIDTH / 2, x + WIDTH},
                new int[]{y + ROOF_HEIGHT, y, y + ROOF_HEIGHT},
                3
        );

        g2.setColor(new Color(120, 60, 20));
        g2.fillPolygon(roof);
        g2.setColor(Color.BLACK);
        g2.drawPolygon(roof);

        // ---- house body ----
        g2.setColor(bodyColor);
        g2.fillRect(x, y + ROOF_HEIGHT, WIDTH, HEIGHT);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y + ROOF_HEIGHT, WIDTH, HEIGHT);

        // ---- door ----
        g2.setColor(new Color(90, 50, 20));
        g2.fillRect(x + WIDTH / 2 - 6, y + ROOF_HEIGHT + HEIGHT - 18, 12, 18);

        // ---- label ----
        g2.setColor(Color.BLACK);
        g2.drawString(
                house.getHouseId(),
                x + 10,
                y + ROOF_HEIGHT + HEIGHT + 15
        );
    }

    public void setPosition(Point pos)
    {
        this.pos = pos;
    }

    public House house()
    {
        return this.house;
    }

    public boolean contains(Point p)
    {
        Rectangle bounds = new Rectangle(
                pos.x,
                pos.y,
                WIDTH,
                HEIGHT + ROOF_HEIGHT
        );
        return bounds.contains(p);
    }
}
