package com.akmaral.benchmark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Dependency-free PNG plotting; all plotted points come from measured Result records. */
final class Plots {
    private static final Color[] COLORS = {new Color(27, 100, 181), new Color(220, 90, 35),
            new Color(26, 135, 98), new Color(145, 74, 177)};

    private Plots() { }

    static void write(Benchmark.Result[] results, Benchmark.Result[] bonus, Path directory) throws IOException {
        Files.createDirectories(directory);
        for (String workload : new String[]{"W1", "W2", "W3", "W4"}) {
            draw(results, workload, directory.resolve(workload + ".png"));
        }
        draw(bonus, "B", directory.resolve("buildheap.png"));
    }

    private static void draw(Benchmark.Result[] all, String workload, Path file) throws IOException {
        String[] keys = new String[4];
        int seriesCount = 0;
        for (Benchmark.Result result : all) {
            if (!result.workload().equals(workload)) continue;
            String key = label(result);
            boolean known = false;
            for (int i = 0; i < seriesCount; i++) if (keys[i].equals(key)) known = true;
            if (!known) keys[seriesCount++] = key;
        }
        BufferedImage image = new BufferedImage(1600, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, 1600, 1080);
        g.setColor(new Color(22, 38, 56)); g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String title = switch (workload) {
            case "W1" -> "W1 | 10,000 random accesses";
            case "W2" -> "W2 | 1,000 searches (50% present)";
            case "W3" -> "W3 | 1,000 insertions + 1,000 removals";
            case "W4" -> "W4 | Priority processing: n inserts + n extracts";
            default -> "Bonus | Floyd buildHeap vs repeated insert";
        };
        g.drawString(title, 50, 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 19));
        g.drawString("Seed 42 | Median of 5 runs after 10 warm-ups | Instrumented Java operations", 50, 81);
        for (int s = 0; s < seriesCount; s++) {
            int x = 50 + (s % 2) * 710;
            int y = 118 + (s / 2) * 30;
            g.setColor(COLORS[s]); g.setStroke(new BasicStroke(4)); g.drawLine(x, y - 6, x + 35, y - 6);
            g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.drawString(keys[s], x + 45, y);
        }
        for (int metric = 0; metric < 4; metric++) {
            int x = 50 + (metric % 2) * 785;
            int y = 182 + (metric / 2) * 424;
            panel(g, all, workload, keys, seriesCount, metric, x, y);
        }
        g.setColor(Color.DARK_GRAY); g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("x: logarithmic n. Time: logarithmic ms. Counters: log10(1 + count), so zero remains visible.", 50, 1058);
        g.dispose();
        if (!ImageIO.write(image, "png", file.toFile())) throw new IOException("PNG writer unavailable");
    }

    private static void panel(Graphics2D g, Benchmark.Result[] all, String workload,
                              String[] keys, int seriesCount, int metric, int ox, int oy) {
        String[] names = {"Time (ms, median)", "Steps (array reads / next hops)",
                "Moves (element relocations / links)", "Comparisons (element values)"};
        g.setColor(new Color(22, 38, 56)); g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.drawString(names[metric], ox, oy + 20);
        int left = ox + 95, top = oy + 48, width = 595, height = 270;
        double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
        for (Benchmark.Result result : all) if (result.workload().equals(workload)) {
            double v = transform(value(result, metric), metric);
            low = Math.min(low, v); high = Math.max(high, v);
        }
        low = metric == 0 ? Math.floor(low) : 0;
        high = Math.max(low + 1, Math.ceil(high));
        int tickStep = Math.max(1, (int) Math.ceil((high - low) / 5));
        high = low + Math.ceil((high - low) / tickStep) * tickStep;
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        for (double tick = low; tick <= high; tick += tickStep) {
            int y = top + height - (int) ((tick - low) / (high - low) * height);
            g.setColor(new Color(226, 231, 237)); g.drawLine(left, y, left + width, y);
            g.setColor(Color.DARK_GRAY);
            double actual = metric == 0 ? Math.pow(10, tick) : Math.pow(10, tick) - 1;
            String label = actual == 0 ? "0" : String.format(Locale.ROOT, "%.0e", actual);
            g.drawString(label, left - 82, y + 5);
        }
        for (int power = 2; power <= 5; power++) {
            int x = left + (power - 2) * width / 3;
            g.setColor(new Color(226, 231, 237)); g.drawLine(x, top, x, top + height);
            g.setColor(Color.DARK_GRAY); g.drawString("1e" + power, x - 16, top + height + 26);
        }
        g.setColor(Color.DARK_GRAY); g.drawString("n (initial elements)", left + 215, top + height + 56);
        for (int s = 0; s < seriesCount; s++) {
            int previousX = -1, previousY = -1;
            g.setColor(COLORS[s]);
            g.setStroke(s < 2 ? new BasicStroke(3) : new BasicStroke(3, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND, 0, new float[]{9, 5}, 0));
            for (Benchmark.Result result : all) {
                if (!result.workload().equals(workload) || !label(result).equals(keys[s])) continue;
                int x = left + (int) ((Math.log10(result.n()) - 2) / 3 * width);
                int y = top + height - (int) ((transform(value(result, metric), metric) - low) / (high - low) * height);
                if (previousX >= 0) g.drawLine(previousX, previousY, x, y);
                g.fillOval(x - 5, y - 5, 10, 10);
                previousX = x; previousY = y;
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    private static String label(Benchmark.Result r) {
        return r.structure() + (r.variant().equals("-") ? "" : " / " + r.variant());
    }

    private static double value(Benchmark.Result r, int metric) {
        return switch (metric) {
            case 0 -> r.timeMs();
            case 1 -> r.counts().steps();
            case 2 -> r.counts().moves();
            default -> r.counts().comparisons();
        };
    }

    private static double transform(double value, int metric) {
        return metric == 0 ? Math.log10(Math.max(value, 1e-9)) : Math.log10(1 + value);
    }
}
