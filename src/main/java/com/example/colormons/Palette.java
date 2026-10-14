package com.example.colormons;

/** Shared bits of the UI: group names, colours and the little swatch strip. */
public class Palette {
    public static final String[] GROUP_NAMES = {"Primary", "Secondary", "Tertiary", "Eyes", "Outline", "Custom"};
    /** Colour used when a group has no colour set yet, just so the UI has something to draw. */
    public static final int[] FALLBACK = {0xFFCC33, 0xCC3333, 0x2A2A2A, 0x33BBFF, 0x111111, 0xBB66FF};
    public static final String[] FINISH_NAMES = {"Default", "Glossy", "Matte", "Shiny"};

    public static int shown(MonColors mc, int group) {
        return mc.colors[group] >= 0 ? mc.colors[group] : FALLBACK[group];
    }
}
