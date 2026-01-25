package com.talhanation.smallships.network.fabric;

import com.talhanation.smallships.network.ModPackets;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ServerboundSyncShipRotationFabricPacket implements FabricPacket, ServerPlayNetworking.PlayChannelHandler {
    private final float yaw;

    @SuppressWarnings("unused")
    public ServerboundSyncShipRotationFabricPacket(float yaw) {
        this.yaw = yaw;
    }

    @SuppressWarnings("unused")
    public ServerboundSyncShipRotationFabricPacket(FriendlyByteBuf buf) {
        this.yaw = buf.readFloat();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(yaw);
    }

    @Override
    public ResourceLocation getId() {
        return ModPackets.id("server_sync_ship_rotation");
    }

    @SuppressWarnings("unused")
    @Override
    public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        if (player.getVehicle() instanceof Ship ship && ship.getDriver() == player) {
            ship.setYRot(yaw);
            ship.setYHeadRot(yaw);
            ship.setRot(yaw, ship.getXRot());
        }
    }
}
