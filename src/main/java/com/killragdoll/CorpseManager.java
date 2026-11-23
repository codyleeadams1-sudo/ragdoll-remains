package com.killragdoll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Client-side only. When a mob dies, its vanilla death animation is hidden and a
 * physics-driven clone (the "corpse") takes its place.
 */
public final class CorpseManager {
    /** How long a corpse stays, in ticks (20 = 1 second). */
    public static final int LIFETIME_TICKS = 20 * 60;
    /** Max corpses at once; oldest are removed first. */
    public static final int MAX_CORPSES = 50;

    private static final Map<Integer, Corpse> CORPSES = new LinkedHashMap<>();
    private static final Random RNG = new Random();
    private static int nextId = 1_000_000_000;

    public static final class Corpse {
        public final ClientLevel level;
        public final Mob entity;
        public double roll, pitch, prevRoll, prevPitch, rollVel, pitchVel;
        public int age;

        Corpse(ClientLevel level, Mob entity) {
            this.level = level;
            this.entity = entity;
        }
    }

    private CorpseManager() {}

    public static Corpse get(Entity e) {
        return CORPSES.get(e.getId());
    }

    public static boolean isCorpse(Entity e) {
        return CORPSES.containsKey(e.getId());
    }

    public static void onMobDeath(LivingEntity victim, ClientLevel level) {
        if (!(victim instanceof Mob) || victim instanceof EnderDragon) return;
        if (isCorpse(victim)) return;

        Entity created = victim.getType().create(level, EntitySpawnReason.LOAD);
        if (!(created instanceof Mob clone)) return;

        try {
            List<SynchedEntityData.DataValue<?>> values = victim.getEntityData().getNonDefaultValues();
            if (values != null) clone.getEntityData().assignValues(values);
        } catch (Throwable ignored) {
        }

        clone.setPose(Pose.STANDING);
        clone.setHealth(clone.getMaxHealth());
        clone.setNoAi(true);
        clone.setInvisible(false);

        clone.setPos(victim.getX(), victim.getY(), victim.getZ());
        clone.setYRot(victim.getYRot());
        clone.setXRot(victim.getXRot());
        clone.setYBodyRot(victim.yBodyRot);
        clone.setYHeadRot(victim.yHeadRot);
        clone.setOldPosAndRot();
        clone.yBodyRotO = victim.yBodyRot;
        clone.yHeadRotO = victim.yHeadRot;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack s = victim.getItemBySlot(slot);
                if (!s.isEmpty()) clone.setItemSlot(slot, s.copy());
            } catch (Throwable ignored) {
            }
        }

        // Hide the vanilla dying mob (client-side only).
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                victim.setItemSlot(slot, ItemStack.EMPTY);
            } catch (Throwable ignored) {
            }
        }
        victim.setInvisible(true);

        // Launch velocity: carry over knockback, kick away from the local player.
        Vec3 dm = victim.getDeltaMovement();
        Player me = Minecraft.getInstance().player;
        if (me != null && me.distanceToSqr(victim) < 100) {
            Vec3 away = new Vec3(victim.getX() - me.getX(), 0, victim.getZ() - me.getZ());
            if (away.lengthSqr() > 1.0E-4) dm = dm.add(away.normalize().scale(0.2));
        }
        dm = dm.add(0, 0.2, 0);
        clone.setDeltaMovement(dm);

        clone.setId(nextId++);
        level.addEntity(clone);

        Corpse c = new Corpse(level, clone);
        double sign = RNG.nextBoolean() ? 1 : -1;
        c.roll = c.prevRoll = sign * 1.0;
        c.rollVel = sign * (10 + RNG.nextDouble() * 15);
        c.pitchVel = (RNG.nextDouble() - 0.5) * 16;
        CORPSES.put(clone.getId(), c);

        while (CORPSES.size() > MAX_CORPSES) {
            Iterator<Corpse> it = CORPSES.values().iterator();
            Corpse oldest = it.next();
            it.remove();
            if (!oldest.entity.isRemoved()) oldest.entity.discard();
        }
    }

    public static void tick(ClientLevel level) {
        Iterator<Corpse> it = CORPSES.values().iterator();
        while (it.hasNext()) {
            Corpse c = it.next();
            if (c.level != level || c.entity.isRemoved() || c.age++ > LIFETIME_TICKS) {
                if (!c.entity.isRemoved()) c.entity.discard();
                it.remove();
                continue;
            }
            step(c, level);
        }
    }

    private static void step(Corpse c, ClientLevel level) {
        Mob e = c.entity;
        Vec3 dm = e.getDeltaMovement();
        boolean water = e.isInWater();

        dm = new Vec3(dm.x, dm.y - (water ? 0.01 : 0.08), dm.z);
        if (water) dm = dm.scale(0.8);

        // Player pushing
        AABB box = e.getBoundingBox();
        for (Player p : level.players()) {
            if (p.isSpectator()) continue;
            if (p.getBoundingBox().inflate(0.1, 0, 0.1).intersects(box)) {
                Vec3 away = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
                if (away.lengthSqr() < 1.0E-4) {
                    away = new Vec3(RNG.nextDouble() - 0.5, 0, RNG.nextDouble() - 0.5);
                }
                away = away.normalize();
                double speed = p.getDeltaMovement().horizontalDistance();
                double s = 0.03 + speed * 0.5;
                dm = dm.add(away.scale(s));
                c.rollVel += (RNG.nextDouble() - 0.5) * 4;
            }
        }
        double h = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
        if (h > 0.6) dm = new Vec3(dm.x * 0.6 / h, dm.y, dm.z * 0.6 / h);

        double vyBefore = dm.y;
        e.setDeltaMovement(dm);
        e.move(MoverType.SELF, dm);
        dm = e.getDeltaMovement();
        boolean ground = e.onGround();

        if (ground && vyBefore < -0.35) {
            dm = new Vec3(dm.x, -vyBefore * 0.25, dm.z);
            c.rollVel += (RNG.nextDouble() - 0.5) * 20;
            c.pitchVel += (RNG.nextDouble() - 0.5) * 10;
        }
        double fr = ground ? 0.7 : 0.98;
        dm = new Vec3(dm.x * fr, dm.y * 0.98, dm.z * fr);
        e.setDeltaMovement(dm);

        // Rotation
        c.prevRoll = c.roll;
        c.prevPitch = c.pitch;
        if (ground || water) {
            double tr = Math.floor(c.roll / 180.0) * 180.0 + 90.0;
            c.rollVel += (tr - c.roll) * 0.2;
            c.rollVel *= 0.6;
            double tp = Math.round(c.pitch / 360.0) * 360.0;
            c.pitchVel += (tp - c.pitch) * 0.2;
            c.pitchVel *= 0.6;
        } else {
            c.rollVel *= 0.99;
            c.pitchVel *= 0.99;
        }
        c.roll += c.rollVel;
        c.pitch += c.pitchVel;
    }

    public static float lerp(float pt, double prev, double cur) {
        return Mth.lerp(pt, (float) prev, (float) cur);
    }
}
