package com.squezsaz.lfarmer.model;

public class PlayerStats {

    private long cropsHarvested;
    private long mobsCollected;
    private long itemsSold;
    private double moneyEarned;
    private double fuelUsed;

    public PlayerStats() {
        this(0, 0, 0, 0.0, 0.0);
    }

    public PlayerStats(long cropsHarvested, long mobsCollected, long itemsSold, double moneyEarned, double fuelUsed) {
        this.cropsHarvested = cropsHarvested;
        this.mobsCollected = mobsCollected;
        this.itemsSold = itemsSold;
        this.moneyEarned = moneyEarned;
        this.fuelUsed = fuelUsed;
    }

    public long getCropsHarvested() {
        return cropsHarvested;
    }

    public void setCropsHarvested(long cropsHarvested) {
        this.cropsHarvested = cropsHarvested;
    }

    public void addCropsHarvested(long amount) {
        this.cropsHarvested += amount;
    }

    public long getMobsCollected() {
        return mobsCollected;
    }

    public void setMobsCollected(long mobsCollected) {
        this.mobsCollected = mobsCollected;
    }

    public void addMobsCollected(long amount) {
        this.mobsCollected += amount;
    }

    public long getItemsSold() {
        return itemsSold;
    }

    public void setItemsSold(long itemsSold) {
        this.itemsSold = itemsSold;
    }

    public void addItemsSold(long amount) {
        this.itemsSold += amount;
    }

    public double getMoneyEarned() {
        return moneyEarned;
    }

    public void setMoneyEarned(double moneyEarned) {
        this.moneyEarned = moneyEarned;
    }

    public void addMoneyEarned(double amount) {
        this.moneyEarned += amount;
    }

    public double getFuelUsed() {
        return fuelUsed;
    }

    public void setFuelUsed(double fuelUsed) {
        this.fuelUsed = fuelUsed;
    }

    public void addFuelUsed(double amount) {
        this.fuelUsed += amount;
    }

    public void merge(PlayerStats other) {
        if (other == null) return;
        this.cropsHarvested += other.cropsHarvested;
        this.mobsCollected += other.mobsCollected;
        this.itemsSold += other.itemsSold;
        this.moneyEarned += other.moneyEarned;
        this.fuelUsed += other.fuelUsed;
    }
}
