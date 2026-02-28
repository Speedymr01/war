package com.tdm;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Unified headshot detection system for both ranged and melee combat.
 * Works with all LivingEntities (players and mobs).
 *
 * Head region is defined as the top 25% of the entity's bounding box by default.
 */
public class HeadshotDetector {

    public static boolean isRangedHeadshot(Location projectileLocation, LivingEntity victim, double headFraction) {
        if (projectileLocation == null || victim == null) {
            return false;
        }

        if (headFraction <= 0.0) return false;

        headFraction = Math.max(0.0, Math.min(1.0, headFraction));

        BoundingBox victimBox = victim.getBoundingBox();
        double victimMinY = victimBox.getMinY();
        double victimMaxY = victimBox.getMaxY();
        double headRegionHeight = victimMaxY - victimMinY;
        double headRegionStart = victimMaxY - (headRegionHeight * headFraction);

        return projectileLocation.getY() >= headRegionStart;
    }

    public static boolean isMeleeHeadshot(Player attacker, LivingEntity victim, double headFraction) {
        if (attacker == null || victim == null) {
            return false;
        }

        if (headFraction <= 0.0) return false;

        headFraction = Math.max(0.0, Math.min(1.0, headFraction));

        Location eyeLocation = attacker.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();
        double maxDistance = 3.5;

        RayTraceResult result = attacker.getWorld().rayTraceEntities(
            eyeLocation,
            lookDirection,
            maxDistance,
            entity -> entity.getUniqueId().equals(victim.getUniqueId())
        );

        if (result == null || result.getHitEntity() == null) {
            return false;
        }

        Location hitLocation = result.getHitPosition().toLocation(victim.getWorld());
        BoundingBox victimBox = victim.getBoundingBox();
        double victimMinY = victimBox.getMinY();
        double victimMaxY = victimBox.getMaxY();
        double headRegionHeight = victimMaxY - victimMinY;
        double headRegionStart = victimMaxY - (headRegionHeight * headFraction);

        return hitLocation.getY() >= headRegionStart;
    }

    public static boolean isRangedHeadshot(Location projectileLocation, LivingEntity victim) {
        return isRangedHeadshot(projectileLocation, victim, 0.25);
    }

    public static boolean isMeleeHeadshot(Player attacker, LivingEntity victim) {
        return isMeleeHeadshot(attacker, victim, 0.25);
    }

    public static double[] getHeadRegionYBounds(LivingEntity entity) {
        BoundingBox box = entity.getBoundingBox();
        double minY = box.getMinY();
        double maxY = box.getMaxY();
        double headRegionHeight = maxY - minY;
        double headRegionStart = maxY - (headRegionHeight * 0.25);

        return new double[]{headRegionStart, maxY};
    }
}
