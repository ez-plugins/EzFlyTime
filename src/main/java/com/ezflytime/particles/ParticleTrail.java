package com.ezflytime.particles;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class ParticleTrail {
    private final String effectKey;
    private final List<Location> trailPoints = new ArrayList<>();
    private final long creationTime = System.currentTimeMillis();
    private final int maxLength;

    public ParticleTrail(String effectKey, int maxLength) {
        this.effectKey = effectKey;
        this.maxLength = maxLength;
    }

    public void addPoint(Location location) {
        trailPoints.add(location.clone());
        if (trailPoints.size() > maxLength) trailPoints.remove(0);
    }

    public List<Location> getTrailPoints() {
        return new ArrayList<>(trailPoints);
    }

    public int size() {
        return trailPoints.size();
    }

    public Location getLatestPoint() {
        if (trailPoints.isEmpty()) return null;
        return trailPoints.get(trailPoints.size() - 1).clone();
    }

    public Location getPoint(int index) {
        if (index < 0 || index >= trailPoints.size()) return null;
        return trailPoints.get(index).clone();
    }

    public void clear() {
        trailPoints.clear();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > 30000;
    }

    public long getAgeMillis() {
        return System.currentTimeMillis() - creationTime;
    }

    public int getMaxLength() { return maxLength; }

    public String getEffectKey() { return effectKey; }

    @Override
    public String toString() {
        return "ParticleTrail{" +
                "effectKey='" + effectKey + '\'' +
                ", points=" + trailPoints.size() +
                ", ageMs=" + getAgeMillis() +
                '}';
    }
}
