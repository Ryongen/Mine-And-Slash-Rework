package com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts;

import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;

import java.util.ArrayList;
import java.util.List;

/**
 * A Void modifier that hasn't been chosen yet (see addons/void_crafting). It gives no stats. Once the
 * item has been cast into the End's void the offered affixes are stored in {@link #opts}, so closing
 * the choice screen and casting it in again shows the same three instead of rerolling them.
 */
public class VoidSeal {

    // matches the pick packet to this exact item, wherever it ends up in the inventory
    public long uid;

    // tier the chosen affix gets
    public String rar = IRarity.COMMON_ID;

    // affix ids the void offered, empty until it has
    public List<String> opts = new ArrayList<>();

    public VoidSeal() {
    }

    public boolean hasSpoken() {
        return opts != null && !opts.isEmpty();
    }
}
