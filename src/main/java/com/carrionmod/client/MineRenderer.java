package com.carrionmod.client;

import com.carrionmod.CarrionMod;
import com.carrionmod.StickyMine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MineRenderer extends EntityRenderer<StickyMine> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarrionMod.ID, "mine"), "main");
    static final ResourceLocation TEX = new ResourceLocation(CarrionMod.ID, "textures/entity/turret.png");
    private final ModelPart body, light;

    public MineRenderer(EntityRendererProvider.Context c) { super(c); ModelPart r = c.bakeLayer(LAYER); body = r.getChild("body"); light = r.getChild("light"); }

    public static LayerDefinition layer() {
        MeshDefinition m = new MeshDefinition(); PartDefinition r = m.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -2, -4, 8, 3, 8), PartPose.ZERO);
        r.addOrReplaceChild("light", CubeListBuilder.create().texOffs(36, 36).addBox(-1.5f, -3.5f, -1.5f, 3, 1.5f, 3), PartPose.ZERO);
        return LayerDefinition.create(m, 64, 64);
    }

    @Override public ResourceLocation getTextureLocation(StickyMine e) { return TEX; }

    @Override public void render(StickyMine e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose(); ps.translate(0, .1, 0); ps.scale(-.0625f * 1.6f, -.0625f * 1.6f, .0625f * 1.6f);
        var vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        body.render(ps, vc, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        boolean armed = e.armedOnly();
        boolean blink = armed || ((e.tickCount / (e.fuse() > 40 ? 5 : 2)) % 2 == 0);
        this.light.render(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, armed ? .2f : 1f, armed ? 1f : .15f, .15f, blink ? 1f : .25f);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }
}
