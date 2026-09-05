package com.talhanation.smallships.forge.events;

import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.entity.ship.PirateShipSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

public class PassengerEvents {

    @SubscribeEvent
    public void onPlayerInteractWithPassenger(PlayerInteractEvent.EntityInteract event){
        Player player = event.getEntity();
        Entity entity = event.getTarget();

        if(!player.isCrouching()
                && entity.isPassenger()
                && !(entity instanceof Player)
                && !(entity.getEncodeId() != null && entity.getEncodeId().contains("captain"))
                && entity.getVehicle() != null
                && entity.getVehicle() instanceof Ship){

            entity.stopRiding();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }

        if(player.isPassenger() && player.getVehicle() != null && player.getVehicle() instanceof Ship ship){
            if(ship.canAddPassenger(entity) && !(entity instanceof Player)){
                entity.startRiding(ship);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().getVehicle() instanceof Ship ship) {
            ship.stopShipFromDisconnectedDriver(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            PirateShipSpawner.tick(serverLevel);
        }
    }
}
