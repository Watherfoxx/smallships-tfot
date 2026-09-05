package com.talhanation.smallships.network.fabric;

import com.talhanation.smallships.network.ModPackets;
import com.talhanation.smallships.world.entity.ship.GalleonEntity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

public class ServerboundUseGalleonHelmFabricPacket implements FabricPacket, ServerPlayNetworking.PlayChannelHandler {
    private final int galleonId;

    public ServerboundUseGalleonHelmFabricPacket(int galleonId) {
        this.galleonId = galleonId;
    }

    public ServerboundUseGalleonHelmFabricPacket(FriendlyByteBuf buf) {
        this.galleonId = buf.readVarInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.galleonId);
    }

    @Override
    public ResourceLocation getId() {
        return ModPackets.id("server_use_galleon_helm");
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
                        FriendlyByteBuf buf, PacketSender responseSender) {
        Entity entity = player.level().getEntity(this.galleonId);
        if (entity instanceof GalleonEntity galleon) {
            galleon.tryTakeHelm(player);
        }
    }
}
