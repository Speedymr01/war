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

        // Check if projectile is within the head region vertically
        double projectileY = projectileLocation.getY();
        if (projectileY < headRegionStart || projectileY > victimMaxY) {
            return false;
        }
        
        // Check if projectile is within the victim's bounding box horizontally (with small tolerance)
        double tolerance = 0.5; // Allow some margin for hit detection
        double projectileX = projectileLocation.getX();
        double projectileZ = projectileLocation.getZ();
        
        return projectileX >= victimBox.getMinX() - tolerance && projectileX <= victimBox.getMaxX() + tolerance &&
               projectileZ >= victimBox.getMinZ() - tolerance && projectileZ <= victimBox.getMaxZ() + tolerance;
    }

    public static boolean isMeleeHeadshot(Player attacker, LivingEntity victim, double headFraction) {
        if (attacker == null || victim == null) {
            return false;
        }

        if (headFraction <= 0.0) return false;

        headFraction = Math.max(0.0, Math.min(1.0, headFraction));

        Location eyeLocation = attacker.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();
        double maxDistance = 4.0; // Slightly increased for better detection

        // Perform raytrace to find hit position
        RayTraceResult result = attacker.getWorld().rayTraceEntities(
            eyeLocation,
            lookDirection,
            maxDistance,
            0.1, // Ray size - slightly larger for more reliable detection
            entity -> entity.getUniqueId().equals(victim.getUniqueId())
        );

        if (result == null || result.getHitEntity() == null) {
            // Fallback: if raytrace fails but victim is close, check if looking at head height
            double distance = eyeLocation.distance(victim.getEyeLocation());
            if (distance <= maxDistance) {
                // Check if attacker is looking roughly at victim's head level
                BoundingBox victimBox = victim.getBoundingBox();
                double victimMinY = victimBox.getMinY();
                double victimMaxY = victimBox.getMaxY();
                double headRegionHeight = victimMaxY - victimMinY;
                double headRegionStart = victimMaxY - (headRegionHeight * headFraction);
                
                // Project attacker's look direction and check Y level
                Vector toVictim = victim.getEyeLocation().toVector().subtract(eyeLocation.toVector());
                double dotProduct = lookDirection.dot(toVictim.normalize());
                
                // If looking at victim (dot product > 0.8 means roughly facing them)
                if (dotProduct > 0.8) {
                    // Check if the line from attacker to victim intersects head region
                    double victimEyeY = victim.getEyeLocation().getY();
                    return victimEyeY >= headRegionStart;
                }
            }
            return false;
        }

        // Use raytrace hit position
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
