package com.whokillme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public final class WhoKillMeHud {

    private static final int BG = 0xB3161C28;
    private static final int DIVIDER = 0x33FFFFFF;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int MUTED = 0xFF8B98A9;
    private static final int RED = 0xFFF28B82;
    private static final int GREEN = 0xFF81D9A0;
    private static final int TEAL = 0xFF5EC8D8;

    private static final int PAD = 5;
    private static final int LINE = 11;
    private static final int TOP = 50;
    private static final String SEP = " - ";

    private record Row(String label, int labelColor, String v1, int c1, String v2, int c2) {
    }

    private WhoKillMeHud() {
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;
        WhoKillMeData d = WhoKillMeData.get();
        int sw = ctx.getScaledWindowWidth();

        // Left side: players who killed me
        List<WhoKillMeData.Entry> killers = new ArrayList<>();
        for (WhoKillMeData.Entry e : d.stats.values()) {
            if (e != null && e.killedMe > 0) killers.add(e);
        }
        killers.sort((a, b) -> Integer.compare(b.killedMe, a.killedMe));

        List<Row> left = new ArrayList<>();
        for (int i = 0; i < Math.min(8, killers.size()); i++) {
            WhoKillMeData.Entry e = killers.get(i);
            left.add(new Row(e.name, TEXT, "x" + e.killedMe, RED, null, 0));
        }
        if (left.isEmpty()) {
            left.add(new Row("Nobody yet", MUTED, null, 0, null, 0));
        }
        drawPanel(ctx, tr, sw, false, "Killed Me", RED, left);

        // Right side: players I chose
        List<Row> right = new ArrayList<>();
        for (String n : d.tracked) {
            WhoKillMeData.Entry e = d.find(n);
            int k = e == null ? 0 : e.iKilled;
            int dd = e == null ? 0 : e.killedMe;
            right.add(new Row(n, TEXT, String.valueOf(k), GREEN, String.valueOf(dd), RED));
        }
        if (right.isEmpty()) {
            String keyName = WhoKillMeClient.openKey == null
                    ? "?"
                    : WhoKillMeClient.openKey.getBoundKeyLocalizedText().getString();
            right.add(new Row("Press [" + keyName + "] to pick", MUTED, null, 0, null, 0));
        }
        drawPanel(ctx, tr, sw, true, "Tracked (Me - Them)", TEAL, right);
    }

    private static void drawPanel(DrawContext ctx, TextRenderer tr, int screenWidth,
                                  boolean rightSide, String title, int accent, List<Row> rows) {
        int w = tr.getWidth(title);
        for (Row r : rows) {
            int rw = 0;
            if (r.v1() != null) {
                rw = tr.getWidth(r.v1());
                if (r.v2() != null) rw += tr.getWidth(SEP) + tr.getWidth(r.v2());
                rw += 12;
            }
            w = Math.max(w, tr.getWidth(r.label()) + rw);
        }
        w = Math.max(110, w + PAD * 2);

        int h = PAD + LINE + 5 + rows.size() * LINE + PAD;
        int x = rightSide ? screenWidth - w - 4 : 4;
        int y = TOP;

        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 2, accent);
        ctx.drawTextWithShadow(tr, title, x + PAD, y + PAD + 1, accent);
        ctx.fill(x + PAD, y + PAD + LINE + 2, x + w - PAD, y + PAD + LINE + 3, DIVIDER);

        int ry = y + PAD + LINE + 5;
        for (Row r : rows) {
            ctx.drawTextWithShadow(tr, r.label(), x + PAD, ry, r.labelColor());
            if (r.v1() != null) {
                int total = tr.getWidth(r.v1());
                if (r.v2() != null) total += tr.getWidth(SEP) + tr.getWidth(r.v2());
                int cx = x + w - PAD - total;
                ctx.drawTextWithShadow(tr, r.v1(), cx, ry, r.c1());
                if (r.v2() != null) {
                    cx += tr.getWidth(r.v1());
                    ctx.drawTextWithShadow(tr, SEP, cx, ry, MUTED);
                    cx += tr.getWidth(SEP);
                    ctx.drawTextWithShadow(tr, r.v2(), cx, ry, r.c2());
                }
            }
            ry += LINE;
        }
    }
}
