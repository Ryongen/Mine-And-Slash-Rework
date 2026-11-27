package com.robertx22.addons.void_crafting.packets;

import com.robertx22.addons.void_crafting.client.VoidClient;
import com.robertx22.library_of_exile.main.MyPacket;
import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** server -> client: the Void's offers for a sealed item, opens the choice screen */
public class VoidWhisperPacket extends MyPacket<VoidWhisperPacket> {

    public long uid;
    public int lvl;
    public String rar = "";
    public List<String> opts = new ArrayList<>();

    public VoidWhisperPacket() {
    }

    public VoidWhisperPacket(long uid, int lvl, String rar, List<String> opts) {
        this.uid = uid;
        this.lvl = lvl;
        this.rar = rar;
        this.opts = new ArrayList<>(opts);
    }

    @Override
    public ResourceLocation getIdentifier() {
        return new ResourceLocation(SlashRef.MODID, "void_whisper");
    }

    @Override
    public void loadFromData(FriendlyByteBuf buf) {
        uid = buf.readLong();
        lvl = buf.readVarInt();
        rar = buf.readUtf();
        int n = buf.readVarInt();
        opts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            opts.add(buf.readUtf());
        }
    }

    @Override
    public void saveToData(FriendlyByteBuf buf) {
        buf.writeLong(uid);
        buf.writeVarInt(lvl);
        buf.writeUtf(rar);
        buf.writeVarInt(opts.size());
        opts.forEach(buf::writeUtf);
    }

    @Override
    public void onReceived(ExilePacketContext ctx) {
        VoidClient.openReveal(uid, lvl, rar, opts);
    }

    @Override
    public MyPacket<VoidWhisperPacket> newInstance() {
        return new VoidWhisperPacket();
    }
}
