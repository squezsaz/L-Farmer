package com.squezsaz.lfarmer.model;

public class FarmerLevel {

    private final int level;
    private final double cost;
    private final double value;

    public FarmerLevel(int level, double cost, double value) {
        this.level = level;
        this.cost = cost;
        this.value = value;
    }

    public int getLevel() {
        return level;
    }

    public double getCost() {
        return cost;
    }

    public double getValue() {
        return value;
    }
}
