package com.talhanation.smallships.network.forge;

import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSyncShipRotationForgePacket implements ForgePacket {
    private final float yaw;

    @SuppressWarnings("unused")
    public ServerboundSyncShipRotationForgePacket(float yaw) {
        this.yaw = yaw;
    }

    @SuppressWarnings("unused")
    public ServerboundSyncShipRotationForgePacket(FriendlyByteBuf buf) {
        this.yaw = buf.readFloat();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(yaw);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getVehicle() instanceof Ship ship && ship.getDriver() == player) {
                ship.setYRot(yaw);
                ship.setYHeadRot(yaw);
                ship.setRot(yaw, ship.getXRot());
            }
            ctx.get().setPacketHandled(true);
        });
    }
}
