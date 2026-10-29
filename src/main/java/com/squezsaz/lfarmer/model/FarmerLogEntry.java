package com.squezsaz.lfarmer.model;

import java.util.UUID;

public class FarmerLogEntry {

    private final long id;
    private final long timestamp;
    private final String dateStr;
    private final UUID playerUuid;
    private final String playerName;
    private final String actorName;
    private final String action;
    private final String details;
    private final String serverName;
    private final String locationStr;

    public FarmerLogEntry(long id, long timestamp, String dateStr, UUID playerUuid, String playerName, String actorName, String action, String details, String serverName, String locationStr) {
        this.id = id;
        this.timestamp = timestamp;
        this.dateStr = dateStr;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.actorName = actorName;
        this.action = action;
        this.details = details;
        this.serverName = serverName;
        this.locationStr = locationStr;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDateStr() {
        return dateStr;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getActorName() {
        return actorName;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public String getServerName() {
        return serverName;
    }

    public String getLocationStr() {
        return locationStr;
    }
}
