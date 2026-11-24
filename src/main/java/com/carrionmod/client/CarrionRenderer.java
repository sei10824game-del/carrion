package com.carrionmod.client;

import com.carrionmod.CarrionEntity;
import com.carrionmod.CarrionMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CarrionRenderer extends MobRenderer<CarrionEntity, CarrionModel> {
    static final ResourceLocation TEX = new ResourceLocation(CarrionMod.ID, "textures/entity/carrion.png");
    static final ResourceLocation GLOW = new ResourceLocation(CarrionMod.ID, "textures/entity/carrion_glow.png");

    public CarrionRenderer(EntityRendererProvider.Context c) {
        super(c, new CarrionModel(c.bakeLayer(CarrionModel.LAYER)), 1.5f);
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return RenderType.eyes(GLOW); }
        });
    }

    @Override public ResourceLocation getTextureLocation(CarrionEntity e) { return TEX; }
    @Override protected float getFlipDegrees(CarrionEntity e) { return 0; }

    @Override protected void scale(CarrionEntity e, PoseStack ps, float pt) {
        float s = e.scale();
        float dt = e.deathTime > 0 ? Math.min(1, (e.deathTime + pt) / 20f) : 0; // melt on death
        ps.scale(s * (1 + .5f * dt), s * (1 - .92f * dt), s * (1 + .5f * dt));
        shadowRadius = 1.5f * s;
    }

    @Override
    public void render(CarrionEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(e, yaw, pt, ps, buf, light);
        int id = e.getTether();
        if (id < 0 || e.getTetherKind() == 0) return;
        Entity t = e.level().getEntity(id);
        if (t == null) return;
        double s = e.scale();
        float age = e.tickCount + pt;
        Vec3 from = new Vec3(0, .85 * s, 0).add(Vec3.directionFromRotation(0, Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot)).scale(1.6 * s));
        Vec3 to = new Vec3(Mth.lerp(pt, t.xOld, t.getX()) - Mth.lerp(pt, e.xOld, e.getX()),
                Mth.lerp(pt, t.yOld, t.getY()) - Mth.lerp(pt, e.yOld, e.getY()) + t.getBbHeight() * .5,
                Mth.lerp(pt, t.zOld, t.getZ()) - Mth.lerp(pt, e.zOld, e.getZ()));
        int kind = e.getTetherKind();
        float reach = 1;
        if (kind == CarrionEntity.T_FLESH) reach = Mth.clamp(e.attackProgress(age) / 10f, 0, 1);
        if (kind == CarrionEntity.T_MIND) reach = Mth.clamp(e.attackProgress(age) / 8f, 0, 1);
        Vec3 end = from.add(to.subtract(from).scale(reach));
        VertexConsumer vc = buf.getBuffer(RenderType.leash());
        Matrix4f m = ps.last().pose();
        switch (kind) {
            case CarrionEntity.T_WEB -> { // taut glistening strands
                for (int i = 0; i < 4; i++) strand(vc, m, from, end, light, .02f, .93f, .93f, .97f, 0.15f + .05f * i, i * 1.57f, age, 0.2f);
            }
            case CarrionEntity.T_FLESH -> { // braided fleshy tendril
                for (int i = 0; i < 3; i++) strand(vc, m, from, end, light, .075f, .52f, .06f, .09f, .22f * (float) s, i * 2.09f + age * .2f, age, 1f);
                strand(vc, m, from, end, light, .05f, .78f, .15f, .15f, 0f, 0, age, 0);
            }
            default -> { // pulsing violet thread
                float pulse = .5f + .5f * Mth.sin(age * .6f);
                for (int i = 0; i < 2; i++) strand(vc, m, from, end, light, .04f, .5f + .3f * pulse, .15f, .75f + .2f * pulse, .12f, i * 3.14f + age * .3f, age, 1f);
            }
        }
    }

    /** Camera-independent crossed ribbon along a sagging, writhing curve. */
    private static void strand(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, int light, float w, float r, float g, float bl, float amp, float phase, float age, float writhe) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 dir = d.scale(1 / len);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        int n = Math.max(8, (int) (len * 4));
        for (int pass = 0; pass < 2; pass++) {
            Vec3 ax = pass == 0 ? side : up;
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n;
                float env = Mth.sin(t * Mth.PI);
                double ph = phase + t * 9 - age * .35f * writhe;
                Vec3 p = a.add(d.scale(t))
                        .add(side.scale(Math.sin(ph) * amp * env * writhe)).add(up.scale(Math.cos(ph) * amp * env * writhe - Math.sin(t * Math.PI) * .25 * (1 - writhe * .6) * Math.min(1, len / 8)));
                Vec3 o = ax.scale(w * (1 - .35f * t));
                vc.vertex(m, (float) (p.x + o.x), (float) (p.y + o.y), (float) (p.z + o.z)).color(r, g, bl, 1f).uv2(light).endVertex();
                vc.vertex(m, (float) (p.x - o.x), (float) (p.y - o.y), (float) (p.z - o.z)).color(r, g, bl, 1f).uv2(light).endVertex();
            }
        }
    }
}
