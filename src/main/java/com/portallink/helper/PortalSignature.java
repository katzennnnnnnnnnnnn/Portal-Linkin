package com.portallink.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The "key" of a portal: which glazed terracotta blocks sit in its four frame corners.
 * Two portals are linked when their signatures are equal. Order and position of the corners do not matter,
 * so a portal can be rotated or built in the other dimension in any orientation. No glazed corner = no signature.
 * (Pure Java, no Minecraft classes.)
 */
final class PortalSignature {

    private static final String SUFFIX = "_glazed_terracotta";

    private PortalSignature() {
    }

    /** @param cornerBlockPaths registry paths of the four corner blocks, e.g. "cyan_glazed_terracotta", "obsidian" */
    static String of(List<String> cornerBlockPaths) {
        List<String> names = new ArrayList<>();
        for (String path : cornerBlockPaths) {
            if (path != null && path.endsWith(SUFFIX) && path.length() > SUFFIX.length()) {
                names.add(path.substring(0, path.length() - SUFFIX.length()));
            }
        }
        Collections.sort(names);
        return String.join("+", names);
    }

    /** "light_blue+red" -> "light blue + red" for chat messages. */
    static String pretty(String signature) {
        return signature.replace('_', ' ').replace("+", " + ");
    }
}
