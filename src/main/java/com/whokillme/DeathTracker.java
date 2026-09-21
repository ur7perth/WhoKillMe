package com.whokillme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class DeathTracker {

    private static final Set<String> KNOWN = new LinkedHashSet<>();

    private DeathTracker() {
    }

    public static List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return out;
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().getName();
            if (n != null && !n.isEmpty()) out.add(n);
        }
        return out;
    }

    public static void refreshKnown() {
        KNOWN.addAll(onlineNames());
    }

    public static void clearKnown() {
        KNOWN.clear();
    }

    public static void onMessage(Text message, boolean overlay) {
        if (overlay) return;
        try {
            scan(message);
        } catch (Exception ignored) {
        }
    }

    private static void scan(Text text) {
        if (text.getContent() instanceof TranslatableTextContent t) {
            if (t.getKey().startsWith("death.")) {
                handleDeath(t);
                return;
            }
            for (Object arg : t.getArgs()) {
                if (arg instanceof Text inner) scan(inner);
            }
        }
        for (Text sibling : text.getSiblings()) {
            scan(sibling);
        }
    }

    private static void handleDeath(TranslatableTextContent t) {
        Object[] args = t.getArgs();
        if (args.length < 2) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String me = mc.player.getName().getString();

        refreshKnown();
        WhoKillMeData d = WhoKillMeData.get();

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(me);
        candidates.addAll(onlineNames());
        candidates.addAll(KNOWN);
        for (WhoKillMeData.Entry e : d.stats.values()) {
            if (e != null && e.name != null && !e.name.isEmpty()) candidates.add(e.name);
        }

        String victim = resolve(plain(args[0]), candidates);
        String killer = resolve(plain(args[1]), candidates);
        if (victim == null || killer == null) return;
        if (victim.equalsIgnoreCase(killer)) return;

        if (victim.equalsIgnoreCase(me)) {
            d.entry(killer).killedMe++;
            WhoKillMeData.save();
        } else if (killer.equalsIgnoreCase(me)) {
            d.entry(victim).iKilled++;
            WhoKillMeData.save();
        }
    }

    private static String plain(Object o) {
        if (o instanceof Text text) return text.getString();
        return String.valueOf(o);
    }

    private static String resolve(String text, Collection<String> names) {
        String best = null;
        for (String n : names) {
            if (matches(text, n) && (best == null || n.length() > best.length())) {
                best = n;
            }
        }
        return best;
    }

    private static boolean matches(String text, String name) {
        if (text == null || name == null || name.isEmpty()) return false;
        Pattern p = Pattern.compile(
                "(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])",
                Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }
}
