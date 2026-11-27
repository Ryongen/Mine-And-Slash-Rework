package com.robertx22.addons.void_crafting;

import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VoidTooltips {

    // vanilla's enchanting table glyphs (Standard Galactic Alphabet)
    public static final ResourceLocation RUNE_FONT = new ResourceLocation("minecraft", "alt");

    public static final TextColor VOID_PURPLE = TextColor.fromRgb(0xB46BFF);
    public static final TextColor VOID_DEEP = TextColor.fromRgb(0x6A2FB8);

    public static Style runeStyle() {
        return Style.EMPTY.withFont(RUNE_FONT).withColor(VOID_DEEP);
    }

    /** stable gibberish for a seed, drawn in rune glyphs */
    public static MutableComponent runes(long seed, int length) {
        return Component.literal(runeString(seed, length)).withStyle(runeStyle());
    }

    public static String runeString(long seed, int length) {
        Random r = new Random(seed);
        StringBuilder sb = new StringBuilder();
        int word = 0;
        for (int i = 0; i < length; i++) {
            if (word > 2 && r.nextInt(4) == 0) {
                sb.append(' ');
                word = 0;
            } else {
                sb.append((char) ('a' + r.nextInt(26)));
                word++;
            }
        }
        return sb.toString();
    }

    /**
     * Gear tooltip lines for Void modifiers. {@code withStats} is false in the merged tooltip, where the
     * void affix stats are already summed in with everything else.
     */
    public static List<Component> gearLines(GearItemData gear, boolean withStats) {
        List<Component> list = new ArrayList<>();

        if (withStats && gear.affixes.vo != null && !gear.affixes.vo.isEmpty()) {
            list.add(Component.literal("Void Modifier").withStyle(Style.EMPTY.withColor(VOID_PURPLE)));
            for (AffixData affix : gear.affixes.vo) {
                if (!affix.isValid()) {
                    continue;
                }
                affix.getAllStatsWithCtx(gear.getLevel(), gear.getRarity())
                        .forEach(x -> list.addAll(x.GetTooltipString()));
            }
            list.add(Component.empty());
        }

        var seal = gear.affixes.seal;
        if (seal != null) {
            list.add(Component.literal("Sealed Void Modifier").withStyle(Style.EMPTY.withColor(VOID_PURPLE)));
            list.add(runes(seal.uid, 22));
            if (seal.hasSpoken()) {
                list.add(Component.literal("The Void has spoken. Cast it in again to choose.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                list.add(Component.literal("Cast it into the void beneath the End to unseal.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            list.add(Component.empty());
        }

        return list;
    }
}
