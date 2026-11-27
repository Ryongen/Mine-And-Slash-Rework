package com.robertx22.addons.void_crafting;

import com.robertx22.mine_and_slash.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.library_of_exile.deferred.RegObj;

public class VoidCraftingItems {

    public static RegObj<VoidSigilItem> VOID_SIGIL = Def.item(VoidSigilItem::new, "void_sigil");

    public static void init() {

    }
}
