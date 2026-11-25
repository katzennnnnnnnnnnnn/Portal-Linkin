package com.portallink.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Remembers every portal with a signature that was seen, per world, in a small text file in the world folder.
 * One line per portal: {@code N|O  x  y  z  signature}. (Pure Java, no Minecraft classes.)
 */
final class PortalRegistry {

    /** @param nether true = Nether, false = Overworld; x/y/z = lowest interior portal block of the portal */
    record Entry(boolean nether, int x, int y, int z, String signature) {
        boolean sameAnchor(Entry o) {
            return nether == o.nether && x == o.x && y == o.y && z == o.z;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private Path file;

    /** Loads the registry of a world (replaces what was in memory). */
    synchronized void load(Path file) {
        this.file = file;
        entries.clear();
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.trim().split("\\|");
                if (p.length != 5) continue;
                try {
                    entries.add(new Entry(p[0].equals("N"), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]), p[4]));
                } catch (NumberFormatException ignored) {
                    // skip broken line
                }
            }
        } catch (IOException ignored) {
            // an unreadable file just means an empty registry
        }
    }

    synchronized void clear() {
        entries.clear();
        file = null;
    }

    /** @return true if this portal was new or its signature changed */
    synchronized boolean add(Entry entry) {
        for (int i = 0; i < entries.size(); i++) {
            Entry old = entries.get(i);
            if (old.sameAnchor(entry)) {
                if (old.signature().equals(entry.signature())) return false;
                entries.set(i, entry);
                save();
                return true;
            }
        }
        entries.add(entry);
        save();
        return true;
    }

    synchronized void remove(Entry entry) {
        if (entries.remove(entry)) save();
    }

    /** Copy of all portals in that dimension with that signature. */
    synchronized List<Entry> candidates(boolean nether, String signature) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries) {
            if (e.nether() == nether && e.signature().equals(signature)) result.add(e);
        }
        return result;
    }

    synchronized void save() {
        if (file == null) return;
        List<String> lines = new ArrayList<>();
        for (Entry e : entries) {
            lines.add((e.nether() ? "N" : "O") + "|" + e.x() + "|" + e.y() + "|" + e.z() + "|" + e.signature());
        }
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // best effort: the registry is rebuilt by scanning anyway
        }
    }
}
