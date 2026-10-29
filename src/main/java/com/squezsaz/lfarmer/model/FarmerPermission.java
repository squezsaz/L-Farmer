package com.squezsaz.lfarmer.model;

public class FarmerPermission {

    private boolean canOpen;
    private boolean canWithdraw;
    private boolean canSell;
    private boolean canUpgrade;

    public FarmerPermission() {
        this(true, false, false, false);
    }

    public FarmerPermission(boolean canOpen, boolean canWithdraw, boolean canSell, boolean canUpgrade) {
        this.canOpen = canOpen;
        this.canWithdraw = canWithdraw;
        this.canSell = canSell;
        this.canUpgrade = canUpgrade;
    }

    public boolean canOpen() {
        return canOpen;
    }

    public void setCanOpen(boolean canOpen) {
        this.canOpen = canOpen;
    }

    public boolean canWithdraw() {
        return canWithdraw;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public boolean canSell() {
        return canSell;
    }

    public void setCanSell(boolean canSell) {
        this.canSell = canSell;
    }

    public boolean canUpgrade() {
        return canUpgrade;
    }

    public void setCanUpgrade(boolean canUpgrade) {
        this.canUpgrade = canUpgrade;
    }
}
