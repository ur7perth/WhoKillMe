package com.whokillme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WhoKillMeScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;
    private static final int PER_PAGE = 5;

    private static final int BG = 0xF01B2230;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int MUTED = 0xFF8B98A9;
    private static final int RED = 0xFFF28B82;
    private static final int GREEN = 0xFF81D9A0;
    private static final int TEAL = 0xFF5EC8D8;

    private TextFieldWidget search;
    private ButtonWidget prev;
    private ButtonWidget next;
    private ButtonWidget reset;
    private final List<ButtonWidget> rows = new ArrayList<>();
    private List<String> filtered = new ArrayList<>();
    private String query = "";
    private String selected = null;
    private int page = 0;
    private int px;
    private int py;

    public WhoKillMeScreen() {
        super(Text.literal("WhoKillMe?"));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        rows.clear();

        search = new TextFieldWidget(textRenderer, px + 12, py + 36, 150, 18, Text.literal("Search"));
        search.setMaxLength(16);
        search.setPlaceholder(Text.literal("Type a player name..."));
        search.setText(query);
        search.setChangedListener(s -> {
            query = s;
            page = 0;
            rebuild();
        });
        addDrawableChild(search);
        setInitialFocus(search);

        prev = ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            rebuild();
        }).dimensions(px + 12, py + H - 32, 30, 20).build();
        addDrawableChild(prev);

        next = ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            rebuild();
        }).dimensions(px + 132, py + H - 32, 30, 20).build();
        addDrawableChild(next);

        reset = ButtonWidget.builder(Text.literal("Reset"), b -> {
            if (selected != null) {
                WhoKillMeData.get().reset(selected);
            }
        }).dimensions(px + 178, py + 120, 150, 20).build();
        addDrawableChild(reset);

        rebuild();
    }

    private List<String> names() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String me = mc.player == null ? "" : mc.player.getName().getString();
        String q = query.trim().toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        boolean exact = false;
        for (String n : DeathTracker.onlineNames()) {
            if (n.equalsIgnoreCase(me)) continue;
            if (out.contains(n)) continue;
            if (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(n);
            }
            if (n.equalsIgnoreCase(query.trim())) exact = true;
        }
        if (!q.isEmpty() && !exact && !query.trim().equalsIgnoreCase(me)) {
            out.add(query.trim());
        }
        return out;
    }

    private void rebuild() {
        for (ButtonWidget b : rows) {
            remove(b);
        }
        rows.clear();

        filtered = names();

        int pages = Math.max(1, (filtered.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE && start + i < filtered.size(); i++) {
            String n = filtered.get(start + i);
            boolean isSelected = selected != null && selected.equalsIgnoreCase(n);
            Text label = Text.literal(n).formatted(isSelected ? Formatting.GREEN : Formatting.WHITE);
            ButtonWidget b = ButtonWidget.builder(label, btn -> choose(n))
                    .dimensions(px + 12, py + 60 + i * 22, 150, 20)
                    .build();
            rows.add(b);
            addDrawableChild(b);
        }

        prev.active = page > 0;
        next.active = page < pages - 1;
        reset.visible = selected != null;
        reset.active = selected != null;
    }

    private void choose(String name) {
        String resolved = name;
        for (String n : DeathTracker.onlineNames()) {
            if (n.equalsIgnoreCase(name)) {
                resolved = n;
                break;
            }
        }
        if (selected != null && selected.equalsIgnoreCase(resolved)) {
            selected = null;
        } else {
            selected = resolved;
            WhoKillMeData.get().entry(resolved);
            WhoKillMeData.save();
        }
        rebuild();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && !query.trim().isEmpty() && !filtered.isEmpty()) {
            String pick = filtered.get(0);
            for (String n : filtered) {
                if (n.equalsIgnoreCase(query.trim())) {
                    pick = n;
                    break;
                }
            }
            choose(pick);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);

        ctx.fill(px, py, px + W, py + H, BG);
        ctx.fill(px, py, px + W, py + 3, TEAL);
        ctx.fill(px + 170, py + 34, px + 171, py + H - 12, 0x33FFFFFF);

        ctx.drawCenteredTextWithShadow(textRenderer, "WhoKillMe?", px + W / 2, py + 10, TEAL);
        ctx.drawTextWithShadow(textRenderer, "Online players", px + 12, py + 24, MUTED);

        int pages = Math.max(1, (filtered.size() + PER_PAGE - 1) / PER_PAGE);
        String pageText = (page + 1) + "/" + pages;
        ctx.drawCenteredTextWithShadow(textRenderer, pageText, px + 87, py + H - 26, MUTED);

        if (filtered.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "No players found", px + 12, py + 66, MUTED);
        }

        int rx = px + 178;
        ctx.drawTextWithShadow(textRenderer, "Score", rx, py + 24, MUTED);

        if (selected == null) {
            ctx.drawTextWithShadow(textRenderer, "Click a player name", rx, py + 40, MUTED);
            ctx.drawTextWithShadow(textRenderer, "to see the score", rx, py + 52, MUTED);
            return;
        }

        WhoKillMeData.Entry e = WhoKillMeData.get().find(selected);
        int kills = e == null ? 0 : e.iKilled;
        int deaths = e == null ? 0 : e.killedMe;

        ctx.drawTextWithShadow(textRenderer, selected, rx, py + 38, TEXT);

        String ks = String.valueOf(kills);
        String sep = " - ";
        String ds = String.valueOf(deaths);
        int total = textRenderer.getWidth(ks) + textRenderer.getWidth(sep) + textRenderer.getWidth(ds);
        int cx = rx + 75;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, py + 58, 0);
        ctx.getMatrices().scale(2f, 2f, 1f);
        int sx = -total / 2;
        ctx.drawTextWithShadow(textRenderer, ks, sx, 0, GREEN);
        sx += textRenderer.getWidth(ks);
        ctx.drawTextWithShadow(textRenderer, sep, sx, 0, MUTED);
        sx += textRenderer.getWidth(sep);
        ctx.drawTextWithShadow(textRenderer, ds, sx, 0, RED);
        ctx.getMatrices().pop();

        ctx.drawTextWithShadow(textRenderer, "You killed them: " + kills, rx, py + 88, GREEN);
        ctx.drawTextWithShadow(textRenderer, "They killed you: " + deaths, rx, py + 100, RED);

        ctx.drawTextWithShadow(textRenderer, "Click the name again", rx, py + 150, MUTED);
        ctx.drawTextWithShadow(textRenderer, "to unselect", rx, py + 162, MUTED);
    }
}
