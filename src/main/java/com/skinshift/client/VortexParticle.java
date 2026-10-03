package com.skinshift.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Частица эффекта смены скина: три визуальных стиля (вихрь/спираль/взрыв) на одном классе. */
public class VortexParticle extends TextureSheetParticle {
    public enum Mode { VORTEX, SPIRAL, EXPLOSION }

    private final Mode mode;
    private final double cx;
    private final double cz;
    private final float rise;
    private final float r0;
    private float angle;
    private final float baseSize;
    private final int strand; // для SPIRAL: вторая нить смещена на пол-оборота

    protected VortexParticle(ClientLevel level, double x, double y, double z,
                             double startAngle, double riseIn, double packedRadius,
                             SpriteSet sprites, Mode mode) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.mode = mode;
        this.cx = x;
        this.cz = z;
        this.angle = (float) startAngle;
        this.rise = (float) riseIn;

        // индекс цвета палитры упакован десятками в целую часть radius (см. ClientFx.spawnParticles)
        int colorIdx = Mth.clamp((int) (packedRadius / 10.0), 0, ClientConfig.COLORS.length - 1);
        float radius = (float) (packedRadius - colorIdx * 10.0);
        this.r0 = radius;
        this.strand = this.random.nextInt(2);

        this.lifetime = mode == Mode.EXPLOSION ? (14 + this.random.nextInt(8)) : (24 + this.random.nextInt(14));
        this.baseSize = 0.16f + this.random.nextFloat() * 0.12f;
        this.quadSize = baseSize;
        this.hasPhysics = false;

        float[] c = ClientConfig.COLORS[colorIdx];
        this.setColor(c[0], c[1], c[2]);
        this.pickSprite(sprites);
        this.setPos(x + Mth.cos(angle) * r0, y, z + Mth.sin(angle) * r0);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float f = this.age / (float) this.lifetime;
        double ny;
        float r;
        float a;

        switch (mode) {
            case SPIRAL -> {
                this.angle += 0.55f;
                a = angle + (strand == 1 ? Mth.PI : 0f);
                r = r0 * 0.6f * (1.0f - 0.5f * f);
                ny = this.y + this.rise * 1.4;
            }
            case EXPLOSION -> {
                this.angle += 0.05f;
                a = angle;
                r = r0 * (0.4f + 1.4f * f); // разлетается наружу
                ny = this.y + this.rise * (1.0 - f) * 2.0;
            }
            default -> { // VORTEX
                this.angle += 0.30f;
                a = angle;
                r = r0 * (1.0f - 0.35f * f);
                ny = this.y + this.rise;
            }
        }

        this.setPos(cx + Mth.cos(a) * r, ny, cz + Mth.sin(a) * r);
        this.alpha = f < 0.65f ? 1.0f : 1.0f - (f - 0.65f) / 0.35f;
        this.quadSize = baseSize * (1.0f - 0.4f * f);
    }

    /** Полная яркость независимо от освещения. */
    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Mode mode;

        public Provider(SpriteSet sprites, Mode mode) {
            this.sprites = sprites;
            this.mode = mode;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double startAngle, double rise, double packedRadius) {
            return new VortexParticle(level, x, y, z, startAngle, rise, packedRadius, sprites, mode);
        }
    }
}
