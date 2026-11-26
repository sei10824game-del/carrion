package com.carrionmod.client;

import com.carrionmod.CarrionMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CarrionMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CarrionModel.LAYER, CarrionModel::createBodyLayer);
        e.registerLayerDefinition(ShotRenderer.LAYER, ShotRenderer::layer);
        e.registerLayerDefinition(TurretRenderer.LAYER, TurretRenderer::layer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CarrionMod.CARRION.get(), CarrionRenderer::new);
        e.registerEntityRenderer(CarrionMod.SHOT.get(), ShotRenderer::new);
        e.registerEntityRenderer(CarrionMod.TURRET.get(), TurretRenderer::new);
    }
}
