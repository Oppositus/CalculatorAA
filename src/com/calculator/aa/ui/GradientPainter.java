package com.calculator.aa.ui;

import java.awt.*;

public class GradientPainter {
    private Color color0;
    private Color color50;
    private Color color100;

    public enum ColorName {Begin, Middle, End}

    public GradientPainter() {
        color0 = SettingsDialog.parseColor("gradient.from", "255,0,0");
        color50 = SettingsDialog.parseColor("gradient.middle", "255,255,0");
        color100 = SettingsDialog.parseColor("gradient.to", "0,255,0");
    }

    public GradientPainter(Color c0, Color c50, Color c100) {
        color0 = c0;
        color50 = c50;
        color100 = c100;
    }

    public void updateColor(ColorName name, Color c) {
        switch (name) {
            case Begin:
                color0 = c;
                break;
            case Middle:
                color50 = c;
                break;
            case End:
                color100 = c;
                break;
        }
    }

    public void paintBackground(Component c, Graphics g, Rectangle clip) {
        int x = 0;
        int y = 0;
        int w = c.getWidth();
        int h = c.getHeight();

        if (clip != null) {
            x = clip.x;
            y = clip.y;
            w = clip.width;
            h = clip.height;
        }

        g.setClip(clip);
        for (int i = 0; i < w; i++) {
            g.setColor(getColor(((double)i) / ((double)w)));
            g.drawLine(x + i, y, x + i, y + h);
        }
        g.setClip(null);
    }

    public Color getPointColor(ColorName color) {
        switch (color) {
            case Begin:
                return color0;
            case Middle:
                return color50;
            case End:
                return color100;
        }

        return color0;
    }

    public Color getColor(double percent) {
        Color from;
        Color to;
        double percentHalf;

        if (percent < 0.5) {
            from = color0;
            to = color50;
            percentHalf = percent / 0.5;
        } else {
            from = color50;
            to = color100;
            percentHalf = (percent - 0.5) / 0.5;
        }

        int red = from.getRed();
        int green = from.getGreen();
        int blue = from.getBlue();
        int dr = to.getRed() - red;
        int dg = to.getGreen() - green;
        int db = to.getBlue() - blue;

        return new Color(
                red + (int)(dr * percentHalf),
                green + (int)(dg * percentHalf),
                blue + (int)(db * percentHalf));
    }
}
