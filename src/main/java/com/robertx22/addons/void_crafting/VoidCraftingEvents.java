package com.robertx22.addons.void_crafting;

import com.robertx22.addons.void_crafting.packets.VoidWhisperPacket;
import com.robertx22.library_of_exile.main.ApiForgeEvents;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.VoidSeal;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.orbs_of_crafting.misc.ClickContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class VoidCraftingEvents {

    // matched by registry id so mmorpg never depends on the void_mobs jar
    public static final String VOID_MOBS_MODID = "void_mobs";
    public static final Set<String> VOID_BOSSES = Set.of("void_sovereign", "void_queen");

    // prototype rates, generous so it's easy to test
    public static final float MOB_SIGIL_CHANCE = 0.15F;

    public static void init() {

        // putting a sigil on a gear piece seals a Void modifier into it
        ApiForgeEvents.registerForgeEvent(ItemStackedOnOtherEvent.class, e -> {
            Player player = e.getPlayer();
            if (player.level().isClientSide) {
                return;
            }
            ClickContext ctx = ClickContext.of(e);
            if (!ctx.isValid() || !(ctx.currency.getItem() instanceof VoidSigilItem)) {
                return;
            }
            if (!StackSaving.GEARS.has(ctx.target)) {
                return;
            }
            e.setCanceled(true);

            if (ctx.refuseIfStacked()) {
                return;
            }
            var why = VoidCrafting.whyCantSeal(ctx.target);
            if (why != null) {
                player.sendSystemMessage(why.withStyle(ChatFormatting.RED));
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1, 1);
                return;
            }

            ItemStack sealed = ctx.target.copy();
            VoidCrafting.seal(sealed);
            ctx.consumeCurrency(1);
            ctx.updateTarget(sealed);

            player.level().playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1, 0.6F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.6F, 0.5F);
        });

        // void mobs drop sigils
        ApiForgeEvents.registerForgeEvent(LivingDeathEvent.class, e -> {
            LivingEntity mob = e.getEntity();
            if (mob.level().isClientSide || !(e.getSource().getEntity() instanceof Player)) {
                return;
            }
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
            if (id == null || !id.getNamespace().equals(VOID_MOBS_MODID)) {
                return;
            }
            var rand = ThreadLocalRandom.current();
            int amount;
            if (VOID_BOSSES.contains(id.getPath())) {
                amount = 2 + rand.nextInt(2);
            } else {
                amount = rand.nextFloat() < MOB_SIGIL_CHANCE ? 1 : 0;
            }
            if (amount > 0) {
                mob.spawnAtLocation(new ItemStack(VoidCraftingItems.VOID_SIGIL.get(), amount));
            }
        });

        // a sealed item that falls out of the End is heard by the Void: it comes back to whoever threw
        // it, carrying three offers. Vanilla discards items 64 blocks below the world, which is where this hooks in.
        ApiForgeEvents.registerForgeEvent(EntityLeaveLevelEvent.class, e -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity item)) {
                return;
            }
            if (item.getRemovalReason() != Entity.RemovalReason.DISCARDED) {
                return;
            }
            Level level = item.level();
            if (level.dimension() != Level.END || item.getY() >= level.getMinBuildHeight()) {
                return;
            }
            ItemStack stack = item.getItem();
            if (stack.isEmpty() || !VoidCrafting.isSealed(stack)) {
                return;
            }
            // no thrower around (offline, or it fell from a broken chest): vanilla behaviour, it's gone
            if (!(item.getOwner() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack heard = stack.copy();
            VoidSeal seal = VoidCrafting.hearWhispers(heard);
            // read before handing it back: placeItemBackInInventory moves the stack out, leaving `heard` empty
            int lvl = StackSaving.GEARS.loadFrom(heard).getLevel();
            player.getInventory().placeItemBackInInventory(heard);

            ServerLevel sl = player.serverLevel();
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 80, 0.7, 1.0, 0.7, 0.04);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 12, 0.5, 0.8, 0.5, 0.02);
            sl.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.5F, 0.5F);

            if (!seal.hasSpoken()) {
                player.sendSystemMessage(Component.literal("The Void finds nothing in this item it can offer.").withStyle(ChatFormatting.DARK_PURPLE));
                return;
            }
            Packets.sendToClient(player, new VoidWhisperPacket(seal.uid, lvl, seal.rar, seal.opts));
        });
    }
}
