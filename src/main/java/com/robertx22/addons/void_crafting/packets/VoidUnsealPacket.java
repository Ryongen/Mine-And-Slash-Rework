package com.robertx22.addons.void_crafting.packets;

import com.robertx22.addons.void_crafting.VoidCrafting;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** client -> server: the player picked offer {@link #index} for the sealed item {@link #uid} */
public class VoidUnsealPacket extends MyPacket<VoidUnsealPacket> {

    public long uid;
    public int index;

    public VoidUnsealPacket() {
    }

    public VoidUnsealPacket(long uid, int index) {
        this.uid = uid;
        this.index = index;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "void_unseal");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        uid = buf.readLong();
        index = buf.readVarInt();
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeLong(uid);
        buf.writeVarInt(index);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        // the item may have moved since the screen opened, so find it by its seal id rather than a slot
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !StackSaving.GEARS.has(stack)) {
                continue;
            }
            GearItemData gear = StackSaving.GEARS.loadFrom(stack);
            if (gear == null || gear.affixes.seal == null || gear.affixes.seal.uid != uid) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (VoidCrafting.unseal(copy, index)) {
                inv.setItem(i, copy);
                player.serverLevel().sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.8F, 0.6F);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1F, 0.5F);
                player.sendSystemMessage(Component.literal("The seal breaks. ").withStyle(ChatFormatting.DARK_PURPLE)
                        .append(copy.getHoverName().copy()).append(Component.literal(" now bears a Void modifier.").withStyle(ChatFormatting.DARK_PURPLE)));
            }
            return;
        }
        player.sendSystemMessage(Component.literal("The sealed item has to be in your inventory to choose.").withStyle(ChatFormatting.RED));
    }

    @Override
    public MyPacket<VoidUnsealPacket> newInstance() {
        return new VoidUnsealPacket();
    }
}
