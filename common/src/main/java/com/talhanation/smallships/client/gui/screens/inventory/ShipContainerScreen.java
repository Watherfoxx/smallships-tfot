package com.talhanation.smallships.client.gui.screens.inventory;

import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.config.SmallShipsConfig;
import com.talhanation.smallships.math.Kalkuel;
import com.talhanation.smallships.world.entity.ship.ContainerShip;
import com.talhanation.smallships.world.inventory.ShipContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ShipContainerScreen extends AbstractContainerScreen<ShipContainerMenu> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(SmallShipsMod.MOD_ID,"textures/gui/ship_inventory.png" );
    public static final int FONT_COLOR = 4210752;
    private final int rowCount;
    private final int pageCount;
    private final int pageIndex;
    private final ContainerShip containerShip;
    private final int offset = 40;

    public ShipContainerScreen(ShipContainerMenu shipContainerMenu, Inventory inventory, Component component) {
        super(shipContainerMenu, inventory, component);
        this.imageHeight = 114 + this.getMenu().getRowCount() * 18;
        this.imageWidth = 256;
        this.inventoryLabelY = this.imageHeight - 94;
        this.containerShip = shipContainerMenu.getContainerShip();

        this.rowCount = this.getMenu().getRowCount();
        this.pageCount = this.getMenu().getPageCount();
        this.pageIndex = this.getMenu().getPageIndex();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int k = offset + (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(RESOURCE_LOCATION, k, l, 0, 0, this.imageWidth, this.rowCount * 18 + 17);
        guiGraphics.blit(RESOURCE_LOCATION, k, l + this.rowCount * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    protected void init() {
        this.leftPos = offset + (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        if (this.minecraft == null || this.minecraft.player == null) {
            SmallShipsMod.LOGGER.error("Minecraft client or LocalPlayer is null?! Couldn't render page buttons.");
            return;
        }

        if (this.pageCount > 1) {
            Button backward = this.addRenderableWidget(new Button.Builder(Component.literal("<"),
                    button -> this.getMenu().clickMenuButton(this.minecraft.player, -1))
                    .pos(leftPos + 115, topPos + 125).size(12, 12)
                    .build());

            backward.active = this.pageIndex + 1 > 1;

            Button forward = this.addRenderableWidget(new Button.Builder(Component.literal(">"),
                    button -> this.getMenu().clickMenuButton(this.minecraft.player, 1))
                    .pos(leftPos + 157, topPos + 125)
                    .size(12, 12)
                    .build());
            forward.active = this.pageIndex + 1 < this.pageCount;
        }
    }
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        super.renderLabels(guiGraphics, i, j);
        int currentPassengers = this.containerShip.getPassengers().size();
        int maxPassengers = this.containerShip.getMaxPassengers();

        int dmg = (int) (this.containerShip.getDamage() * 100 / this.containerShip.getAttributes().maxHealth);

        String unit;
        int maxSpeed;
        int currentSpeed;
        switch (SmallShipsConfig.Client.shipModSpeedUnit.get()){
            default -> {
                unit = "km/h";
                maxSpeed = (Mth.ceil(Kalkuel.getKilometerPerHour(this.containerShip.maxSpeed)));
                currentSpeed = (Mth.ceil(Kalkuel.getKilometerPerHour(this.containerShip.getSpeed())));
            }
            case 1 -> {
                unit = "m/s";
                maxSpeed = (Mth.ceil(Kalkuel.getMeterPerSecond(this.containerShip.maxSpeed)));
                currentSpeed = (Mth.ceil(Kalkuel.getMeterPerSecond(this.containerShip.getSpeed())));
            }
            case 2 -> {
                unit = "knots";
                maxSpeed = (Mth.ceil(Kalkuel.getKnots(this.containerShip.maxSpeed)));
                currentSpeed = (Mth.ceil(Kalkuel.getKnots(this.containerShip.getSpeed())));
            }
            case 3 -> {
                unit = "mph";
                maxSpeed = (Mth.ceil(Kalkuel.getMilesPerHour(this.containerShip.maxSpeed)));
                currentSpeed = (Mth.ceil(Kalkuel.getMilesPerHour(this.containerShip.getSpeed())));
            }
        }

        int leftPos = 260;
        int leftPos2 = 323;
        int topPos = 38;
        int gap = 14;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.7F, 0.7F, 1F);
        guiGraphics.drawString(font, "Crew:", leftPos, topPos + gap * 0, FONT_COLOR, false);
        guiGraphics.drawString(font, "Speed " + unit + ":", leftPos, topPos + gap * 1, FONT_COLOR, false);
        guiGraphics.drawString(font, "Damage:", leftPos, topPos + gap * 2, FONT_COLOR, false);

        guiGraphics.drawString(font, currentPassengers + "/" + maxPassengers, leftPos2, topPos + gap * 0, FONT_COLOR, false);
        guiGraphics.drawString(font, currentSpeed + "/" + maxSpeed, leftPos2, topPos + gap * 1, FONT_COLOR, false);
        guiGraphics.drawString(font, dmg + "%", leftPos2, topPos + gap * 2, FONT_COLOR, false);

        guiGraphics.pose().popPose();

        if (this.pageCount > 1) guiGraphics.drawString(font, "" +  (this.pageIndex + 1) + "/"  + this.pageCount, (int) (133 - (float)(Mth.floor(Math.log10(this.pageCount))) * 6), this.rowCount*18+19, FONT_COLOR, false);
    }
}
