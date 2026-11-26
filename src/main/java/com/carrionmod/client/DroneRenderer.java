package com.carrionmod.client;

import com.carrionmod.CarrionMod;
import com.carrionmod.SentryDrone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DroneRenderer extends MobRenderer<SentryDrone, DroneRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarrionMod.ID, "drone"), "main");
    static final ResourceLocation TEX = new ResourceLocation(CarrionMod.ID, "textures/entity/turret.png");
    public DroneRenderer(EntityRendererProvider.Context c) { super(c, new Model(c.bakeLayer(LAYER)), .3f); }
    @Override public ResourceLocation getTextureLocation(SentryDrone e) { return TEX; }

    public static LayerDefinition layer() {
        MeshDefinition m = new MeshDefinition(); PartDefinition r = m.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 36).addBox(-4, -2.5f, -5, 8, 5, 10).texOffs(40, 36).addBox(-1.5f, -1, -8, 3, 3, 3), PartPose.offset(0, 18, 0));
        float[][] rp = {{-7, 16, -5}, {7, 16, -5}, {-7, 16, 5}, {7, 16, 5}};
        for (int i = 0; i < 4; i++) r.addOrReplaceChild("rotor" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-4, 0, -.7f, 8, .6f, 1.4f).addBox(-.7f, 0, -4, 1.4f, .6f, 8).texOffs(0, 18).addBox(-.6f, .6f, -.6f, 1.2f, 4, 1.2f), PartPose.offset(rp[i][0], rp[i][1], rp[i][2]));
        return LayerDefinition.create(m, 64, 64);
    }

    public static class Model extends EntityModel<SentryDrone> {
        final ModelPart root; final ModelPart[] rotor = new ModelPart[4];
        Model(ModelPart r) { root = r; for (int i = 0; i < 4; i++) rotor[i] = r.getChild("rotor" + i); }
        @Override public void setupAnim(SentryDrone e, float a, float b, float age, float yaw, float pitch) {
            for (int i = 0; i < 4; i++) rotor[i].yRot = age * (i % 2 == 0 ? 2.4f : -2.4f);
            root.xRot = 0; root.getChild("body").xRot = pitch * .0174533f * .5f;
        }
        @Override public void renderToBuffer(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float bl, float al) { root.render(ps, vc, l, o, r, g, bl, al); }
    }
}
