package com.robertx22.addons.void_crafting.client;

import net.minecraft.client.Minecraft;

import java.util.List;

// client-only entry point, so the packet class never touches Screen classes on a dedicated server
public class VoidClient {

    public static void openReveal(long uid, int lvl, String rar, List<String> opts) {
        Minecraft.getInstance().setScreen(new VoidRevealScreen(uid, lvl, rar, opts));
    }
}
