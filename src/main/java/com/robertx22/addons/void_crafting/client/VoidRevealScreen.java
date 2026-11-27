package com.robertx22.addons.void_crafting.client;

import com.robertx22.addons.void_crafting.VoidTooltips;
import com.robertx22.addons.void_crafting.packets.VoidUnsealPacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The Void's offer: three cards whose text starts out as runes and decodes into the affix it offers.
 * Closing the screen keeps the offers on the item, so casting it into the void again reopens them.
 */
public class VoidRevealScreen extends Screen {

    static final int CARD_W = 132;
    static final int CARD_H = 168;
    static final int GAP = 14;

    // ticks
    static final int CARD_STAGGER = 7;
    static final int CARD_RISE = 9;
    static final int DECODE_DELAY = 10;
    static final int DECODE_TIME = 26;
    static final int TITLE_DECODE = 30;

    final long uid;
    final int lvl;
    final GearRarity tier;
    final List<Card> cards = new ArrayList<>();

    int ticks = 0;
    boolean picked = false;

    record Card(MutableComponent name, List<MutableComponent> lines, long seed) {
    }

    public VoidRevealScreen(long uid, int lvl, String rar, List<String> opts) {
        super(Component.literal("The Void Whispers"));
        this.uid = uid;
        this.lvl = lvl;
        this.tier = ExileDB.GearRarities().get(rar);

        long seed = uid;
        for (String id : opts) {
            seed = seed * 31 + id.hashCode();
            if (!ExileDB.Affixes().isRegistered(id)) {
                cards.add(new Card(Component.literal("???"), List.of(), seed));
                continue;
            }
            Affix affix = ExileDB.Affixes().get(id);
            AffixData data = new AffixData(affix.type);
            data.id = id;
            data.rar = rar;
            data.p = 50; // middle of the roll; the real value is rolled on pick

            List<MutableComponent> lines = new ArrayList<>();
            data.getAllStatsWithCtx(lvl, tier).forEach(x -> lines.addAll(x.GetTooltipString()));

            String slot = affix.type.isPrefix() ? "Prefix" : "Suffix";
            MutableComponent name = affix.locName().copy().withStyle(Style.EMPTY.withColor(VoidTooltips.VOID_PURPLE));
            lines.add(0, Component.literal(slot + " pool").withStyle(ChatFormatting.DARK_GRAY));
            cards.add(new Card(name, lines, seed));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks == 1) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.6F));
        }
        for (int i = 0; i < cards.size(); i++) {
            if (ticks == cardStart(i) + DECODE_DELAY + DECODE_TIME) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F + i * 0.15F, 0.8F));
            }
        }
    }

    int cardStart(int i) {
        return 6 + i * CARD_STAGGER;
    }

    float decode(int i, float partial) {
        return Mth.clamp((ticks + partial - cardStart(i) - DECODE_DELAY) / DECODE_TIME, 0, 1);
    }

    int cardX(int i) {
        int total = cards.size() * CARD_W + (cards.size() - 1) * GAP;
        return (width - total) / 2 + i * (CARD_W + GAP);
    }

    int cardY() {
        return height / 2 - CARD_H / 2 + 10;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // darkness bleeding up from below, like looking down into the void
        g.fillGradient(0, 0, width, height, 0xB0000000, 0xE0180630);

        // title decodes from runes
        float tp = Mth.clamp((ticks + partial) / TITLE_DECODE, 0, 1);
        Component title = decoding("The Void Whispers", uid ^ 0x7E57L, tp, Style.EMPTY.withColor(VoidTooltips.VOID_PURPLE));
        g.pose().pushPose();
        g.pose().translate(width / 2F, cardY() - 44, 0);
        g.pose().scale(1.6F, 1.6F, 1);
        g.drawCenteredString(font, title, 0, 0, 0xFFFFFF);
        g.pose().popPose();

        MutableComponent sub = Component.literal("It offers ").withStyle(ChatFormatting.GRAY)
                .append(tier.coloredName())
                .append(Component.literal(" power. Choose what it leaves behind.").withStyle(ChatFormatting.GRAY));
        g.drawCenteredString(font, sub, width / 2, cardY() - 22, 0xFFFFFF);

        int hovered = hoveredCard(mouseX, mouseY);

        for (int i = 0; i < cards.size(); i++) {
            float appear = Mth.clamp((ticks + partial - cardStart(i)) / CARD_RISE, 0, 1);
            if (appear <= 0) {
                continue;
            }
            float ease = 1 - (1 - appear) * (1 - appear);
            int x = cardX(i);
            int y = cardY() + (int) ((1 - ease) * 16);
            int alpha = (int) (ease * 255);
            renderCard(g, cards.get(i), i, x, y, alpha, i == hovered, partial);
        }

        g.drawCenteredString(font, Component.literal("Close to decide later. Cast it into the void again to return.").withStyle(ChatFormatting.DARK_GRAY),
                width / 2, cardY() + CARD_H + 14, 0xFFFFFF);

        super.render(g, mouseX, mouseY, partial);
    }

    void renderCard(GuiGraphics g, Card card, int i, int x, int y, int alpha, boolean hover, float partial) {
        int a = alpha << 24;
        float pulse = (Mth.sin((ticks + partial) * 0.15F + i) + 1) / 2;

        if (hover) {
            // outer glow
            for (int r = 4; r >= 1; r--) {
                int ga = (int) ((0x30 - r * 6) * (alpha / 255F));
                g.fill(x - r, y - r, x + CARD_W + r, y + CARD_H + r, (ga << 24) | 0xB46BFF);
            }
        }
        int border = hover ? 0xB46BFF : lerpColor(0x3A1670, 0x6A2FB8, pulse);
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, a | border);
        g.fillGradient(x, y, x + CARD_W, y + CARD_H, a | 0x140A22, a | 0x07030D);

        if (alpha < 200) {
            return;
        }

        float d = decode(i, partial);
        int cx = x + CARD_W / 2;
        int ty = y + 8;

        // a line of runes as the card's sigil, always undecoded
        g.drawCenteredString(font, VoidTooltips.runes(card.seed, 9), cx, ty, 0xFFFFFF);
        ty += 14;

        Component name = d >= 1 ? card.name : decoding(card.name.getString(), card.seed, d, card.name.getStyle());
        for (FormattedCharSequence line : font.split(name, CARD_W - 12)) {
            g.drawCenteredString(font, line, cx, ty, 0xFFFFFF);
            ty += 10;
        }
        ty += 3;
        g.fill(x + 16, ty, x + CARD_W - 16, ty + 1, 0x806A2FB8);
        ty += 7;

        for (int l = 0; l < card.lines.size(); l++) {
            MutableComponent raw = card.lines.get(l);
            // lines decode a little after the name, top to bottom
            float ld = Mth.clamp(d * 1.4F - l * 0.12F, 0, 1);
            Component line = ld >= 1 ? raw : decoding(raw.getString(), card.seed + l, ld, Style.EMPTY.withColor(ChatFormatting.GRAY));
            for (FormattedCharSequence part : font.split(line, CARD_W - 12)) {
                if (ty > y + CARD_H - 12) {
                    return;
                }
                g.drawString(font, part, x + 6, ty, 0xFFFFFF);
                ty += 10;
            }
        }
    }

    /** text where the first {@code progress} share of characters is real and the rest still runes */
    static Component decoding(String text, long seed, float progress, Style real) {
        int k = (int) (text.length() * progress);
        String runes = VoidTooltips.runeString(seed, text.length());
        MutableComponent c = Component.literal(text.substring(0, k)).withStyle(real);
        if (k < text.length()) {
            // keep the real spaces so the words stay in place as they decode
            StringBuilder rest = new StringBuilder();
            for (int j = k; j < text.length(); j++) {
                rest.append(text.charAt(j) == ' ' ? ' ' : runes.charAt(j) == ' ' ? 'x' : runes.charAt(j));
            }
            c.append(Component.literal(rest.toString()).withStyle(VoidTooltips.runeStyle()));
        }
        return c;
    }

    int hoveredCard(double mx, double my) {
        int y = cardY();
        for (int i = 0; i < cards.size(); i++) {
            int x = cardX(i);
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = hoveredCard(mx, my);
        if (i >= 0 && !picked && decode(i, 0) >= 1) {
            picked = true;
            Packets.sendToServer(new VoidUnsealPacket(uid, i));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENDER_EYE_DEATH, 0.6F, 1F));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    static int lerpColor(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gr = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (gr << 8) | bl;
    }
}
