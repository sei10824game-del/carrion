package com.carrionmod.client;

import com.carrionmod.CarrionMod;
import com.carrionmod.SentryTurret;
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

public class TurretRenderer extends MobRenderer<SentryTurret, TurretRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarrionMod.ID, "turret"), "main");
    static final ResourceLocation TEX = new ResourceLocation(CarrionMod.ID, "textures/entity/turret.png");

    public TurretRenderer(EntityRendererProvider.Context c) { super(c, new Model(c.bakeLayer(LAYER)), .6f); }
    @Override public ResourceLocation getTextureLocation(SentryTurret e) { return TEX; }

    public static LayerDefinition layer() {
        MeshDefinition m = new MeshDefinition(); PartDefinition r = m.getRoot();
        r.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -4, -7, 14, 4, 14).texOffs(0, 18).addBox(-2.5f, -14, -2.5f, 5, 10, 5), PartPose.offset(0, 24, 0));
        PartDefinition h = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 36).addBox(-5, -4, -6, 10, 8, 12)
                .texOffs(40, 36).addBox(-1.5f, -1.5f, -16, 3, 3, 10).texOffs(40, 50).addBox(-3, -6, -1, 6, 2, 3), PartPose.offset(0, 10, 0));
        return LayerDefinition.create(m, 64, 64);
    }

    public static class Model extends EntityModel<SentryTurret> {
        final ModelPart root, head;
        Model(ModelPart r) { root = r; head = r.getChild("head"); }
        @Override public void setupAnim(SentryTurret e, float a, float b, float age, float yaw, float pitch) {
            head.yRot = yaw * .0174533f; head.xRot = pitch * .0174533f;
        }
        @Override public void renderToBuffer(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float bl, float al) { root.render(ps, vc, l, o, r, g, bl, al); }
    }
}
