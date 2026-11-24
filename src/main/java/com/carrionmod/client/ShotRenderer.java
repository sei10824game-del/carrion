package com.carrionmod.client;

import com.carrionmod.CarrionMod;
import com.carrionmod.CarrionShot;
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
import com.mojang.math.Axis;

public class ShotRenderer extends EntityRenderer<CarrionShot> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarrionMod.ID, "shot"), "main");
    static final ResourceLocation TEX = new ResourceLocation(CarrionMod.ID, "textures/entity/shot.png");
    private final ModelPart web, spit, spine;

    public ShotRenderer(EntityRendererProvider.Context c) {
        super(c);
        ModelPart r = c.bakeLayer(LAYER);
        web = r.getChild("web"); spit = r.getChild("spit"); spine = r.getChild("spine");
    }

    public static LayerDefinition layer() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        r.addOrReplaceChild("web", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5).texOffs(0, 0).addBox(-1.5f, -1.5f, -4.5f, 3, 3, 9), PartPose.ZERO);
        r.addOrReplaceChild("spit", CubeListBuilder.create().texOffs(0, 16).addBox(-3, -3, -3, 6, 6, 6).addBox(-1.5f, -1.5f, 3, 3, 3, 3), PartPose.ZERO);
        r.addOrReplaceChild("spine", CubeListBuilder.create().texOffs(0, 32).addBox(-1.2f, -1.2f, -7, 2.4f, 2.4f, 10).addBox(-.6f, -.6f, -10, 1.2f, 1.2f, 3), PartPose.ZERO);
        return LayerDefinition.create(m, 64, 64);
    }

    @Override public ResourceLocation getTextureLocation(CarrionShot e) { return TEX; }

    @Override
    public void render(CarrionShot e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.translate(0, .2, 0);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot()) - 180));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        float age = e.tickCount + pt;
        ModelPart p = switch (e.kind()) { case CarrionShot.WEB -> web; case CarrionShot.SPIT -> spit; default -> spine; };
        if (e.kind() != CarrionShot.SPINE) {
            float k = 1 + .12f * Mth.sin(age * 1.3f);
            ps.scale(1 / k, k, 1 / k * .9f + .1f);
        }
        ps.scale(.0625f * 1.0f, .0625f, .0625f);
        ps.scale(-1, -1, 1);
        p.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }
}
