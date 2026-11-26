package com.carrionmod.client;

import com.carrionmod.CarrionMod;
import com.carrionmod.SentryMech;
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
import net.minecraft.util.Mth;

public class MechRenderer extends MobRenderer<SentryMech, MechRenderer.Model> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarrionMod.ID, "mech"), "main");
    static final ResourceLocation TEX = new ResourceLocation(CarrionMod.ID, "textures/entity/turret.png");
    public MechRenderer(EntityRendererProvider.Context c) { super(c, new Model(c.bakeLayer(LAYER)), .7f); }
    @Override public ResourceLocation getTextureLocation(SentryMech e) { return TEX; }

    public static LayerDefinition layer() {
        MeshDefinition m = new MeshDefinition(); PartDefinition r = m.getRoot();
        r.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 36).addBox(-6, -14, -4, 12, 14, 8), PartPose.offset(0, 14, 0));
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3, -5, -3, 6, 5, 6).texOffs(36, 36).addBox(-1.5f, -3.5f, -3.4f, 3, 1.5f, 1), PartPose.offset(0, 0, 0));
        r.addOrReplaceChild("legL", CubeListBuilder.create().texOffs(0, 0).addBox(-2, 0, -2, 4, 14, 4), PartPose.offset(-3.5f, 10, 0));
        r.addOrReplaceChild("legR", CubeListBuilder.create().texOffs(0, 0).addBox(-2, 0, -2, 4, 14, 4), PartPose.offset(3.5f, 10, 0));
        r.addOrReplaceChild("gun", CubeListBuilder.create().texOffs(40, 36).addBox(-2, -2, -16, 4, 4, 16).texOffs(40, 50).addBox(-3, -3, -4, 6, 6, 4), PartPose.offset(8, 6, -2));
        r.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(0, 0).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-8, 2, 0));
        r.addOrReplaceChild("plate0", CubeListBuilder.create().texOffs(0, 36).addBox(-7, -13, -6, 14, 9, 2), PartPose.offset(0, 14, 0));
        r.addOrReplaceChild("plate1", CubeListBuilder.create().texOffs(0, 36).addBox(-10, -16, -4, 5, 4, 8), PartPose.offset(0, 14, 0));
        r.addOrReplaceChild("plate2", CubeListBuilder.create().texOffs(0, 36).addBox(5, -16, -4, 5, 4, 8), PartPose.offset(0, 14, 0));
        return LayerDefinition.create(m, 64, 64);
    }

    public static class Model extends EntityModel<SentryMech> {
        final ModelPart root, legL, legR, gun, arm, head; final ModelPart[] plate = new ModelPart[3];
        Model(ModelPart r) { root = r; legL = r.getChild("legL"); legR = r.getChild("legR"); gun = r.getChild("gun"); arm = r.getChild("arm"); head = r.getChild("head");
            for (int i = 0; i < 3; i++) plate[i] = r.getChild("plate" + i); }
        @Override public void setupAnim(SentryMech e, float limb, float amt, float age, float yaw, float pitch) {
            legL.xRot = Mth.cos(limb * .6f) * 1.1f * amt; legR.xRot = -legL.xRot;
            arm.xRot = -legL.xRot * .5f;
            head.yRot = yaw * .0174533f; head.xRot = pitch * .0174533f; head.y = 0;
            boolean aim = e.getTarget() != null;
            gun.xRot = aim ? pitch * .0174533f : .3f; gun.yRot = aim ? yaw * .0174533f * .3f : 0; gun.zRot = aim ? age * 1.6f * 0 : 0;
            for (int i = 0; i < 3; i++) plate[i].visible = i < e.plates();
        }
        @Override public void renderToBuffer(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) { root.render(ps, vc, l, o, r, g, b, a); }
    }
}
