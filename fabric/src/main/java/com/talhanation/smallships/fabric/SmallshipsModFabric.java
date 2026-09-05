package com.talhanation.smallships.fabric;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.config.fabric.SmallShipsConfigImpl;
import com.talhanation.smallships.fabric.events.PassengerEvents;
import com.talhanation.smallships.network.ModPackets;
import com.talhanation.smallships.world.entity.ModEntityTypes;
import com.talhanation.smallships.world.inventory.ModMenuTypes;
import com.talhanation.smallships.world.item.ModItems;
import com.talhanation.smallships.world.item.ShipItemDurabilityUpdater;
import com.talhanation.smallships.world.sound.ModSoundTypes;
import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.entity.ship.PirateShipSpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class SmallshipsModFabric implements ModInitializer {
    @SuppressWarnings("InstantiationOfUtilityClass")
    @Override
    public void onInitialize() {
        new SmallShipsConfigImpl();
        new SmallShipsMod();
        new ModEntityTypes();
        new ModMenuTypes();
        new ModItems();
        ShipItemDurabilityUpdater.markItemsReady();
        new ModSoundTypes();

        ModPackets.registerPackets();

        UseEntityCallback.EVENT.register(new PassengerEvents());
        ServerTickEvents.END_WORLD_TICK.register(PirateShipSpawner::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player.getVehicle() instanceof Ship ship) {
                ship.stopShipFromDisconnectedDriver(handler.player);
            }
        });
    }
}
