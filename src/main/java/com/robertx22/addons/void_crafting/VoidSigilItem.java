package com.robertx22.addons.void_crafting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// prototype: name and tooltip are literals so testing it doesn't need a lang regen
public class VoidSigilItem extends Item {

    public VoidSigilItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Void Sigil").withStyle(ChatFormatting.DARK_PURPLE);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(VoidTooltips.runes(0x5161L, 14));
        tip.add(Component.literal("Place on a gear piece to seal a Void modifier inside it.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Cast the sealed piece into the void beneath the End").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("and the Void will offer it a choice.").withStyle(ChatFormatting.GRAY));
    }
}
