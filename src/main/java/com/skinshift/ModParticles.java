package com.skinshift;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SkinShiftMod.MODID);

    /** Три визуальных стиля эффекта смены скина, выбираются в настройках мода. */
    public static final RegistryObject<SimpleParticleType> VORTEX =
            PARTICLES.register("vortex", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPIRAL =
            PARTICLES.register("spiral", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> EXPLOSION =
            PARTICLES.register("explosion_fx", () -> new SimpleParticleType(true));

    private ModParticles() {}
}
