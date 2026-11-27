package com.robertx22.addons.void_crafting;

import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.VoidSeal;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Void crafting, a prototype of PoE2-style desecration tied to the void_mobs addon:
 * <ol>
 *     <li>a Void Sigil (dropped by void mobs) is put on a gear piece, which gets a sealed Void modifier</li>
 *     <li>the sealed piece is thrown into the void below the End, and comes back to its thrower with 3 offers</li>
 *     <li>the player picks one, and it becomes a Void modifier on the item</li>
 * </ol>
 * For now the offers come from the normal prefix/suffix pools, rolled one tier higher than usual.
 * A dedicated void affix pool comes later.
 */
public class VoidCrafting {

    public static final int OPTIONS = 3;
    public static final int MAX_VOID_AFFIXES = 1;

    /** null when the sigil can go on this item, otherwise why it can't */
    public static MutableComponent whyCantSeal(ItemStack stack) {
        GearItemData gear = StackSaving.GEARS.loadFrom(stack);
        if (gear == null) {
            return Component.literal("The Void only answers to gear.");
        }
        if (gear.isUnique()) {
            return Component.literal("A unique item refuses the Void.");
        }
        if (ExileStack.of(stack).isCorrupted() || gear.affixes.isCorrupted()) {
            return Component.literal("Corrupted items can't be sealed.");
        }
        if (gear.affixes.seal != null) {
            return Component.literal("This item already carries a sealed Void modifier.");
        }
        if (gear.affixes.vo != null && gear.affixes.vo.size() >= MAX_VOID_AFFIXES) {
            return Component.literal("This item already bears a Void modifier.");
        }
        return null;
    }

    public static void seal(ItemStack stack) {
        GearItemData gear = StackSaving.GEARS.loadFrom(stack);

        VoidSeal seal = new VoidSeal();
        seal.uid = ThreadLocalRandom.current().nextLong();

        // roll the tier the way a normal affix would, then push it one higher: void mods are meant to feel strong
        AffixData tier = new AffixData(Affix.AffixSlot.prefix);
        tier.randomizeTier(gear.getRarity());
        var rar = tier.getRarity();
        if (rar.hasHigherRarity() && !rar.getHigherRarity().is_unique_item) {
            rar = rar.getHigherRarity();
        }
        seal.rar = rar.GUID();

        gear.affixes.seal = seal;
        StackSaving.GEARS.saveTo(stack, gear);
    }

    public static boolean isSealed(ItemStack stack) {
        GearItemData gear = StackSaving.GEARS.loadFrom(stack);
        return gear != null && gear.affixes.seal != null;
    }

    /**
     * The void picks its offers once, on the first cast. Later casts return the same ones, so throwing
     * an item in again can't reroll them.
     */
    public static VoidSeal hearWhispers(ItemStack stack) {
        GearItemData gear = StackSaving.GEARS.loadFrom(stack);
        VoidSeal seal = gear.affixes.seal;

        if (!seal.hasSpoken()) {
            List<Affix> pool = new ArrayList<>(ExileDB.Affixes().getFilterWrapped(x ->
                    (x.type == Affix.AffixSlot.prefix || x.type == Affix.AffixSlot.suffix) && gear.canGetAffix(x)).list);

            List<String> picked = new ArrayList<>();
            while (picked.size() < OPTIONS && !pool.isEmpty()) {
                Affix a = weightedPick(pool);
                pool.remove(a);
                picked.add(a.GUID());
            }
            seal.opts = picked;
            StackSaving.GEARS.saveTo(stack, gear);
        }
        return seal;
    }

    /** turns offer {@code index} into a Void modifier. Returns false if this item has no such offer */
    public static boolean unseal(ItemStack stack, int index) {
        GearItemData gear = StackSaving.GEARS.loadFrom(stack);
        if (gear == null || gear.affixes.seal == null || !gear.affixes.seal.hasSpoken()) {
            return false;
        }
        VoidSeal seal = gear.affixes.seal;
        if (index < 0 || index >= seal.opts.size() || !ExileDB.Affixes().isRegistered(seal.opts.get(index))) {
            return false;
        }

        Affix affix = ExileDB.Affixes().get(seal.opts.get(index));
        AffixData data = new AffixData(affix.type);
        data.id = affix.GUID();
        data.rar = seal.rar;
        data.RerollNumbers();

        if (gear.affixes.vo == null) {
            gear.affixes.vo = new ArrayList<>();
        }
        gear.affixes.vo.add(data);
        gear.affixes.seal = null;
        StackSaving.GEARS.saveTo(stack, gear);
        return true;
    }

    private static Affix weightedPick(List<Affix> list) {
        int total = list.stream().mapToInt(x -> Math.max(1, x.weight)).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Affix a : list) {
            roll -= Math.max(1, a.weight);
            if (roll < 0) {
                return a;
            }
        }
        return list.get(list.size() - 1);
    }
}
