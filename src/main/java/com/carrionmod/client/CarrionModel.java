package com.carrionmod.client;

import com.carrionmod.CarrionEntity;
import com.carrionmod.CarrionMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Procedurally animated flesh mass: jawed core, 4 reaching tendrils, 6 crawling pseudopods, tail, spines and plates. */
public class CarrionModel extends EntityModel<CarrionEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CarrionMod.ID, "carrion"), "main");
    static final float[] AL = {9, 8, 7, 6, 5}, AT = {5.6f, 4.8f, 4f, 3.2f, 2.4f};
    static final float[] LL = {5, 5, 4}, LT = {6, 4.6f, 3.4f};
    static final float[] TL = {8, 7, 6, 5, 4, 3.5f}, TT = {6, 5, 4.2f, 3.4f, 2.6f, 2};
    // arms: x, y, z, rest yaw
    static final float[][] AP = {{-9, 11, -8, .55f}, {9, 11, -8, -.55f}, {-10, 10, -2, 1.25f}, {10, 10, -2, -1.25f}};
    static final float[][] LP = {{-10, 15, -8}, {10, 15, -8}, {-11, 15, 0}, {11, 15, 0}, {-10, 15, 8}, {10, 15, 8}};
    static final float[][] SP = {{0, 7, -6}, {-6, 8, -2}, {6, 8, -2}, {0, 6, 2}, {-6, 8, 6}, {6, 8, 6}, {0, 7, 10}};
    static final float[][] PL = {{-8.5f, 13, -8}, {8.5f, 13, -8}, {-8.5f, 13, 0}, {8.5f, 13, 0}, {-8.5f, 13, 8}, {8.5f, 13, 8}};

    final ModelPart root, body, hump, head, upper, lower;
    final ModelPart[][] arm = new ModelPart[4][5], leg = new ModelPart[6][3];
    final ModelPart[] tail = new ModelPart[6], spine = new ModelPart[7], plate = new ModelPart[6];
    private float plateSm;

    public CarrionModel(ModelPart r) {
        root = r; body = r.getChild("body"); hump = r.getChild("hump"); head = r.getChild("head");
        upper = head.getChild("upper"); lower = head.getChild("lower");
        for (int i = 0; i < 4; i++) chain(r.getChild("arm" + i), arm[i]);
        for (int i = 0; i < 6; i++) chain(r.getChild("leg" + i), leg[i]);
        chain(r.getChild("tail"), tail);
        for (int i = 0; i < 7; i++) spine[i] = r.getChild("spine" + i);
        for (int i = 0; i < 6; i++) plate[i] = r.getChild("plate" + i);
    }

    private static void chain(ModelPart first, ModelPart[] out) {
        out[0] = first;
        for (int i = 1; i < out.length; i++) out[i] = out[i - 1].getChild("s" + i);
    }

    private static void addChain(PartDefinition parent, String name, float[] len, float[] th, PartPose pose, boolean tip) {
        PartDefinition cur = parent.addOrReplaceChild(name, seg(th[0], len[0]), pose);
        for (int i = 1; i < len.length; i++)
            cur = cur.addOrReplaceChild("s" + i, seg(th[i], len[i]), PartPose.offset(0, len[i - 1], 0));
        if (tip) cur.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(100, 96).addBox(-.8f, 0, -.8f, 1.6f, 5, 1.6f), PartPose.offset(0, len[len.length - 1], 0));
    }

    private static CubeListBuilder seg(float t, float l) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(-t / 2, 0, -t / 2, t, l, t);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-11, -13, -13, 22, 13, 26), PartPose.offset(0, 20, 0));
        r.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(0, 50).addBox(-7, -9, -7, 14, 9, 14)
                .texOffs(100, 108).addBox(-5.5f, -10.5f, -1, 2.5f, 2, 2.5f).addBox(3, -10.5f, -1, 2.5f, 2, 2.5f)
                .addBox(-1.2f, -10.5f, 3, 2.5f, 2, 2.5f).addBox(-1.2f, -10.5f, -4.5f, 2.5f, 2, 2.5f), PartPose.offset(0, 8, 5));
        PartDefinition h = r.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0, 16, -12.5f));
        CubeListBuilder up = CubeListBuilder.create().texOffs(0, 96).addBox(-8, -6, -9, 16, 6, 10)
                .texOffs(100, 108).addBox(-6.5f, -8, -8, 4, 2.5f, 3.5f).addBox(2.5f, -8, -8, 4, 2.5f, 3.5f)
                .addBox(-8.8f, -5, -6, 1.4f, 2.2f, 2.2f).addBox(7.4f, -5, -6, 1.4f, 2.2f, 2.2f).addBox(-1.5f, -7.6f, -3, 3, 2, 2.5f)
                .texOffs(100, 96);
        for (float x : new float[]{-7, -3.5f, 0, 3.5f, 7}) up.addBox(x - 1, 0, -9, 2, 3.8f, 2);
        up.addBox(-8.5f, 0, -4, 1.6f, 3, 2).addBox(6.9f, 0, -4, 1.6f, 3, 2);
        h.addOrReplaceChild("upper", up, PartPose.ZERO);
        CubeListBuilder lo = CubeListBuilder.create().texOffs(60, 96).addBox(-7, 0, -9, 14, 4, 9).texOffs(100, 96);
        for (float x : new float[]{-5, -1.7f, 1.7f, 5}) lo.addBox(x - 1, -3.5f, -9, 2, 3.5f, 2);
        h.addOrReplaceChild("lower", lo, PartPose.ZERO);
        for (int i = 0; i < 4; i++) addChain(r, "arm" + i, AL, AT, PartPose.offset(AP[i][0], AP[i][1], AP[i][2]), true);
        for (int i = 0; i < 6; i++) addChain(r, "leg" + i, LL, LT, PartPose.offset(LP[i][0], LP[i][1], LP[i][2]), false);
        addChain(r, "tail", TL, TT, PartPose.offset(0, 15, 12), true);
        for (int i = 0; i < 7; i++)
            r.addOrReplaceChild("spine" + i, CubeListBuilder.create().texOffs(104, 70).addBox(-1.5f, -9, -1.5f, 3, 9, 3), PartPose.offset(SP[i][0], SP[i][1], SP[i][2]));
        for (int i = 0; i < 6; i++)
            r.addOrReplaceChild("plate" + i, CubeListBuilder.create().texOffs(80, 70).addBox(-1.5f, -5, -4, 3, 10, 8), PartPose.offset(PL[i][0], PL[i][1], PL[i][2]));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static float ease(float x) { x = Mth.clamp(x, 0, 1); return x * x * (3 - 2 * x); }
    /** 0..1 progress of p between a and b, eased. */
    private static float ph(float p, float a, float b) { return ease((p - a) / (b - a)); }
    private static float lerp(float t, float a, float b) { return a + (b - a) * t; }

    @Override
    public void setupAnim(CarrionEntity e, float limb, float limbAmt, float age, float yawHead, float pitchHead) {
        int atk = e.getAtk();
        float p = e.attackProgress(age);
        float bob = Mth.sin(age * .09f);
        // ---- per-frame pose variables (rest values) ----
        float bodySY = 1 + .03f * bob, bodySXZ = 1 - .015f * bob, bodyZS = 1, tilt = 0, offY = 0, offZ = 0, jaw = .08f + .04f * Mth.sin(age * .13f);
        float humpS = 1 + .05f * Mth.sin(age * .11f + 1), shake = 0, spineE = .18f, plateE = 0, tailUp = 0, crouch = 0;
        float[] yaw = new float[4], base = new float[4], curl = new float[4], str = new float[4];
        float[] wav = {.35f, .35f, .3f, .3f};
        for (int i = 0; i < 4; i++) { yaw[i] = AP[i][3]; base[i] = -.7f; curl[i] = -.42f; str[i] = 1; }
        float legSpread = 0;

        switch (atk) {
            case CarrionEntity.LASH -> {
                float wind = ph(p, 0, 9), s1 = ph(p, 9, 14), r1 = ph(p, 14, 20), w2 = ph(p, 17, 23), s2 = ph(p, 23, 28), r2 = ph(p, 28, 36);
                tilt = -.18f * wind * (1 - s1) + .25f * s1 * (1 - r1) - .2f * w2 * (1 - s2) + .28f * s2 * (1 - r2);
                offZ = -2f * s1 * (1 - r1) - 2.5f * s2 * (1 - r2);
                for (int i = 0; i < 4; i++) {
                    float side = (i % 2 == 0) ? 1 : -1;        // left arms (+) sweep first, right arms (-) second
                    float yawOut = side * (1.7f + .3f * (i / 2)), yawIn = -side * .8f;
                    float y, b, c, s;
                    if (side > 0) {
                        y = lerp(wind, yaw[i], yawOut); y = lerp(s1, y, yawIn); y = lerp(r1, y, side * .5f);
                        c = lerp(wind, -.42f, -.9f); c = lerp(s1, c, 0); c = lerp(r1, c, -.7f);
                        s = 1 + 1.1f * s1 * (1 - r1);
                    } else {
                        y = lerp(w2, yaw[i], yawOut); y = lerp(s2, y, yawIn); y = lerp(r2, y, side * .5f);
                        c = lerp(w2, -.42f, -.9f); c = lerp(s2, c, 0); c = lerp(r2, c, -.7f);
                        s = 1 + 1.1f * s2 * (1 - r2);
                    }
                    yaw[i] = y; curl[i] = c; str[i] = s;
                    base[i] = -1.35f + .15f * Mth.sin(age * .5f + i);
                }
                jaw = .5f * (s1 + s2) - .3f * (r1 + r2) * .5f + .1f;
                spineE = .5f;
            }
            case CarrionEntity.GRAB, CarrionEntity.CONSUME -> {
                boolean eat = atk == CarrionEntity.CONSUME;
                float reach = ph(p, 0, 12), pull = ph(p, 12, eat ? 24 : 28), hold = eat ? 0 : ph(p, 28, 40), slam = eat ? 0 : ph(p, 40, 47), rec = ph(p, eat ? 70 : 52, eat ? 78 : 66);
                for (int i = 0; i < 2; i++) { // the two front arms do the grabbing
                    float side = i == 0 ? 1 : -1;
                    yaw[i] = lerp(reach, AP[i][3], side * .25f); yaw[i] = lerp(pull, yaw[i], side * .12f);
                    base[i] = lerp(reach, -.7f, -1.5f);
                    if (!eat) { base[i] = lerp(pull, base[i], -2.7f); base[i] = lerp(slam, base[i], -1.2f); }
                    else base[i] = lerp(pull, base[i], -.5f);
                    curl[i] = lerp(reach, -.42f, 0); curl[i] = lerp(pull, curl[i], eat ? -1.1f : -.15f);
                    str[i] = lerp(reach, 1, 1.9f); str[i] = lerp(pull, str[i], eat ? 1.2f : 1.5f);
                    if (!eat) str[i] += .12f * Mth.sin(p * 1.4f) * hold * (1 - slam);
                    if (eat && p > 24 && p < 70) curl[i] += .25f * Mth.sin(p * 1.6f + i);
                    base[i] = lerp(rec, base[i], -.7f); curl[i] = lerp(rec, curl[i], -.42f); str[i] = lerp(rec, str[i], 1); yaw[i] = lerp(rec, yaw[i], AP[i][3]);
                }
                for (int i = 2; i < 4; i++) { // rear arms brace against the ground
                    float side = i == 2 ? 1 : -1;
                    yaw[i] = side * 1.75f; base[i] = lerp(reach, -.7f, -.25f); curl[i] = lerp(reach, -.42f, .35f); str[i] = 1 + .25f * reach;
                    base[i] = lerp(rec, base[i], -.7f);
                }
                crouch = 2.5f * reach * (1 - rec);
                offZ = -3f * slam * (1 - rec) * (eat ? 0 : 1);
                tilt = (-.12f * hold + .35f * slam) * (1 - rec);
                if (eat) {
                    float chew = (p > 22 && p < 70) ? .5f + .5f * Mth.sin(p * 1.0f) : 0;
                    jaw = lerp(reach, .1f, .7f) * (1 - rec); jaw = Math.max(jaw, chew);
                    humpS = 1 + .22f * ph(p, 70, 74) * (1 - ph(p, 74, 78)) + .06f * chew;
                    bodySXZ = 1 + .05f * chew;
                    shake = chew * .35f;
                } else jaw = .75f * ph(p, 40, 47) + .3f * hold - .6f * rec;
                spineE = .5f;
            }
            case CarrionEntity.WEB -> {
                float inh = ph(p, 0, 14), fire = ph(p, 14, 18), rel = ph(p, 22, 38);
                bodySY = 1 + .14f * inh * (1 - fire); bodySXZ = 1 - .07f * inh + .05f * fire * (1 - rel);
                offZ = 2.5f * fire * (1 - rel); tilt = (.22f * inh - .35f * fire) * (1 - rel) * 1;
                jaw = (.9f * inh) * (1 - rel) + .15f * fire;
                tailUp = 1.2f * inh * (1 - rel);
                for (int i = 0; i < 4; i++) {
                    float side = i % 2 == 0 ? 1 : -1;
                    yaw[i] = lerp(inh, AP[i][3], side * (.55f + .45f * (i / 2))); base[i] = lerp(inh, -.7f, i < 2 ? -2.3f : -1.2f);
                    curl[i] = lerp(inh, -.42f, -.2f); str[i] = 1 + .3f * inh;
                    yaw[i] = lerp(rel, yaw[i], AP[i][3]); base[i] = lerp(rel, base[i], -.7f); curl[i] = lerp(rel, curl[i], -.42f); str[i] = lerp(rel, str[i], 1);
                }
                humpS = 1 + .15f * inh * (1 - fire);
            }
            case CarrionEntity.SPIT -> {
                float inh = ph(p, 0, 16), f1 = ph(p, 18, 20) * (1 - ph(p, 20, 23)), f2 = ph(p, 25, 27) * (1 - ph(p, 27, 30)), rel = ph(p, 28, 34);
                float puff = inh * (1 - ph(p, 18, 20)) + (f1 + f2) * -.2f;
                humpS = 1 + .5f * puff; bodySXZ = 1 + .08f * puff; bodySY = 1 + .1f * puff;
                tilt = -.3f * inh * (1 - rel) + .25f * (f1 + f2);
                jaw = (.35f + .65f * inh) * (1 - rel) + .15f * (f1 + f2);
                offZ = 1.5f * (f1 + f2);
                for (int i = 0; i < 4; i++) {
                    float side = i % 2 == 0 ? 1 : -1;
                    yaw[i] = lerp(inh, AP[i][3], side * (1.1f + .3f * (i / 2))); base[i] = lerp(inh, -.7f, -1.6f);
                    curl[i] = lerp(inh, -.42f, -.15f); str[i] = 1 + .45f * inh;
                    yaw[i] = lerp(rel, yaw[i], AP[i][3]); base[i] = lerp(rel, base[i], -.7f); curl[i] = lerp(rel, curl[i], -.42f); str[i] = lerp(rel, str[i], 1);
                }
                spineE = .6f * inh;
            }
            case CarrionEntity.LUNGE -> {
                float coil = ph(p, 0, 14), leap = ph(p, 14, 18), rec = ph(p, 32, 46);
                crouch = 4.5f * coil * (1 - leap) + 0;
                bodySY = 1 - .38f * coil * (1 - leap) + .1f * leap * (1 - rec); bodySXZ = 1 + .16f * coil * (1 - leap);
                bodyZS = 1 + .45f * leap * (1 - rec);
                tilt = (.4f * coil * (1 - leap) - .35f * leap) * (1 - rec);
                jaw = (.5f * coil + .5f * leap) * (1 - rec);
                for (int i = 0; i < 4; i++) {
                    float side = i % 2 == 0 ? 1 : -1;
                    float yc = lerp(coil, AP[i][3], side * 1.6f), bc = lerp(coil, -.7f, 1.9f), cc = lerp(coil, -.42f, -.9f), sc = 1 + .1f * coil;
                    float yl = side * .22f * (1 + i / 2), bl = -1.57f, cl = 0f, sl = 2f;
                    yaw[i] = lerp(leap, yc, yl); base[i] = lerp(leap, bc, bl); curl[i] = lerp(leap, cc, cl); str[i] = lerp(leap, sc, sl);
                    yaw[i] = lerp(rec, yaw[i], AP[i][3]); base[i] = lerp(rec, base[i], -.7f); curl[i] = lerp(rec, curl[i], -.42f); str[i] = lerp(rec, str[i], 1);
                }
                tailUp = -.6f * leap * (1 - rec);
                spineE = .4f;
                legSpread = .5f * coil * (1 - leap);
            }
            case CarrionEntity.BURST -> {
                float con = ph(p, 0, 18), bst = ph(p, 18, 23), rel = ph(p, 26, 50);
                bodySXZ = 1 - .22f * con * (1 - bst) + .28f * bst * (1 - rel); bodySY = 1 + .14f * con * (1 - bst) - .2f * bst * (1 - rel);
                crouch = 2f * con * (1 - bst);
                jaw = .95f * bst * (1 - rel);
                shake = (.25f * con) * (1 - bst) + .2f * bst * (1 - rel);
                spineE = .18f + 1.5f * Math.max(con * .4f, bst) * (1 - rel);
                plateE = .6f * bst * (1 - rel);
                for (int i = 0; i < 4; i++) {
                    float ang = (i + .5f) * Mth.HALF_PI + .35f;
                    float yc = lerp(con, AP[i][3], AP[i][3] * 2.2f), bc = lerp(con, -.7f, -2.7f), cc = lerp(con, -.42f, -1.7f);
                    float yb = ang - Mth.PI, bb = -1.52f;
                    yaw[i] = lerp(bst, yc, yb); base[i] = lerp(bst, bc, bb); curl[i] = lerp(bst, cc, 0); str[i] = 1 + 1.15f * bst;
                    yaw[i] = lerp(rel, yaw[i], AP[i][3]); base[i] = lerp(rel, base[i], -.7f); curl[i] = lerp(rel, curl[i], -.42f); str[i] = lerp(rel, str[i], 1);
                }
                legSpread = .6f * bst * (1 - rel);
            }
            case CarrionEntity.ROAR -> {
                float up = ph(p, 0, 16), rel = ph(p, 36, 48);
                tilt = -.65f * up * (1 - rel); offY = -2.5f * up * (1 - rel); offZ = 4f * up * (1 - rel);
                jaw = 1f * up * (1 - rel);
                float rg = (p > 16 && p < 36) ? 1 : 0;
                shake = .5f * rg; bodySXZ = 1 + .08f * rg;
                spineE = .18f + 1.2f * up * (1 - rel);
                tailUp = 1.1f * up * (1 - rel);
                for (int i = 0; i < 4; i++) {
                    float side = i % 2 == 0 ? 1 : -1;
                    yaw[i] = lerp(up, AP[i][3], side * (.5f + .3f * (i / 2))); base[i] = lerp(up, -.7f, -2.9f - .15f * (i / 2));
                    curl[i] = lerp(up, -.42f, -.12f); str[i] = 1 + .55f * up;
                    yaw[i] = lerp(rel, yaw[i], AP[i][3]); base[i] = lerp(rel, base[i], -.7f); curl[i] = lerp(rel, curl[i], -.42f); str[i] = lerp(rel, str[i], 1);
                }
            }
            case CarrionEntity.MIND -> {
                float up = ph(p, 0, 12), rel = ph(p, 34, 46);
                float spin = age * .12f;
                for (int i = 1; i < 4; i++) { // crown of tendrils above, slowly circling
                    yaw[i] = lerp(up, AP[i][3], spin + i * 2.09f); base[i] = lerp(up, -.7f, 2.9f); curl[i] = lerp(up, -.42f, .12f * Mth.sin(age * .3f + i)); str[i] = 1 + .5f * up;
                }
                yaw[0] = lerp(up, AP[0][3], 0); base[0] = lerp(up, -.7f, -1.5f); curl[0] = lerp(up, -.42f, 0); str[0] = 1 + 1.4f * up;
                for (int i = 0; i < 4; i++) { yaw[i] = lerp(rel, yaw[i], AP[i][3]); base[i] = lerp(rel, base[i], -.7f); curl[i] = lerp(rel, curl[i], -.42f); str[i] = lerp(rel, str[i], 1); }
                bodySY = 1 + .05f * Mth.sin(age * .5f) * up; jaw = .25f * up * (1 - rel);
            }
            case CarrionEntity.HARDEN -> {
                float fold = ph(p, 0, 14), rel = ph(p, 26, 36);
                for (int i = 0; i < 4; i++) {
                    float side = i % 2 == 0 ? 1 : -1;
                    yaw[i] = lerp(fold, AP[i][3], side * (i < 2 ? .5f : 1.3f)); base[i] = lerp(fold, -.7f, 2.7f); curl[i] = lerp(fold, -.42f, .3f); str[i] = 1;
                    base[i] = lerp(rel, base[i], -.7f); curl[i] = lerp(rel, curl[i], -.42f); yaw[i] = lerp(rel, yaw[i], AP[i][3]);
                }
                crouch = 2f * fold * (1 - rel); bodySY = 1 - .1f * fold * (1 - rel); shake = .2f * ph(p, 12, 18) * (1 - rel);
            }
            default -> {}
        }
        // carapace smoothing: extends during HARDEN, stays while hardened
        float plateTarget = Math.max(plateE, e.isHardened() ? 1 : (atk == CarrionEntity.HARDEN ? ph(p, 10, 16) : 0));
        plateSm += (plateTarget - plateSm) * .15f; plateE = plateSm;
        if (e.isHardened()) { for (int i = 0; i < 4; i++) { base[i] = lerp(.8f, base[i], 2.5f); curl[i] = lerp(.8f, curl[i], .3f); } }
        // ---- rest-state ripples ----
        float walk = limb * .55f, amt = Mth.clamp(limbAmt * 1.5f, 0, 1);
        float wob = atk == 0 ? 1 : .4f;
        // apply body
        float sh = shake * Mth.sin(age * 2.1f) * .06f;
        body.setPos(0, 20 + offY + crouch * .5f, offZ); body.yScale = bodySY; body.xScale = bodySXZ; body.zScale = bodySXZ * bodyZS; body.xRot = tilt * .5f; body.zRot = sh; body.yRot = sh;
        hump.setPos(0, 8 + offY + crouch * .5f - (bodySY - 1) * -12, 5 + offZ); hump.xScale = hump.zScale = humpS; hump.yScale = humpS * (.5f + .5f * bodySY); hump.xRot = tilt * .5f;
        head.setPos(0, 16 + offY + crouch * .5f, -12.5f + offZ + Math.min(0, -(bodyZS - 1) * 10)); head.xRot = tilt * .6f;
        upper.xRot = -jaw * .62f; lower.xRot = jaw * .78f;
        head.xRot += Mth.sin(age * .07f) * .02f;
        // tentacles
        for (int i = 0; i < 4; i++) {
            ModelPart[] a = arm[i];
            a[0].setPos(AP[i][0] * bodySXZ, AP[i][1] + offY + crouch * .5f, AP[i][2] + offZ + (bodyZS - 1) * -6);
            a[0].yRot = yaw[i] + sh; a[0].xRot = base[i] + tilt * .3f; a[0].zRot = 0;
            a[0].yScale = str[i]; a[0].xScale = a[0].zScale = 1;
            for (int j = 0; j < 5; j++) {
                float w = Mth.sin(age * .13f * (1 + .1f * i) - j * .8f + i * 1.7f) * wav[i] * wob * (.35f + .2f * j);
                if (j > 0) a[j].xRot = curl[i] * (j == 1 ? 1.2f : 1) + w * .5f;
                if (j > 0) a[j].zRot = Mth.sin(age * .1f + j * .9f + i * 2.3f) * .18f * wob;
                else a[0].zRot = w * .35f;
            }
        }
        for (int i = 0; i < 6; i++) {
            ModelPart[] l = leg[i];
            float side = LP[i][0] < 0 ? 1 : -1;
            float ph0 = walk + i * 1.7f + (i % 2) * Mth.PI;
            float swing = Mth.sin(ph0) * .75f * amt, lift = Math.max(0, Mth.cos(ph0)) * .5f * amt;
            l[0].setPos(LP[i][0] * bodySXZ, LP[i][1] + offY + crouch * .5f, LP[i][2] + offZ * .6f);
            l[0].xRot = swing + (Mth.sin(age * .08f + i) * .05f); l[0].yRot = 0; l[0].zRot = side * (1.1f - lift + legSpread);
            l[0].yScale = 1;
            l[1].xRot = -swing * .4f; l[1].zRot = -side * (.8f - lift * .5f);
            l[2].xRot = swing * .3f; l[2].zRot = -side * (.45f + lift * .5f);
        }
        for (int j = 0; j < 6; j++) {
            if (j == 0) { tail[0].setPos(0, 15 + offY + crouch * .5f, 12 + offZ); tail[0].xRot = 1.75f - tailUp + tilt * .3f; tail[0].yRot = Mth.sin(age * .08f) * .25f; tail[0].zRot = 0; }
            else { tail[j].xRot = -.12f - tailUp * .25f + Mth.sin(age * .12f - j) * .08f; tail[j].yRot = Mth.sin(age * .12f - j * .9f) * (.22f + .06f * j) * (1 + amt); tail[j].zRot = 0; }
        }
        for (int i = 0; i < 7; i++) {
            spine[i].setPos(SP[i][0], SP[i][1] + offY + crouch * .5f - (bodySY - 1) * -12, SP[i][2] + offZ);
            spine[i].yScale = spineE; spine[i].xScale = spine[i].zScale = 1;
            spine[i].zRot = SP[i][0] * .035f * (spineE - .18f) * 4; spine[i].xRot = (SP[i][2] > 3 ? .08f : -.08f) * (spineE - .18f) * 3;
        }
        for (int i = 0; i < 6; i++) {
            float side = PL[i][0] < 0 ? -1 : 1;
            plate[i].setPos(PL[i][0] + side * 4.2f * plateE, PL[i][1] + offY + crouch * .5f, PL[i][2] + offZ);
            plate[i].zRot = side * .25f * plateE; plate[i].yScale = plate[i].zScale = 1; plate[i].xScale = 1;
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
