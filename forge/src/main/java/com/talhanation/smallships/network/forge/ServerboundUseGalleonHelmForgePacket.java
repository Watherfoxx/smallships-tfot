package com.talhanation.smallships.network.forge;

import com.talhanation.smallships.world.entity.ship.GalleonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundUseGalleonHelmForgePacket implements ForgePacket {
    private final int galleonId;

    public ServerboundUseGalleonHelmForgePacket(int galleonId) {
        this.galleonId = galleonId;
    }

    public ServerboundUseGalleonHelmForgePacket(FriendlyByteBuf buf) {
        this.galleonId = buf.readVarInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.galleonId);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.level().getEntity(this.galleonId);
            if (entity instanceof GalleonEntity galleon) {
                galleon.tryTakeHelm(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
