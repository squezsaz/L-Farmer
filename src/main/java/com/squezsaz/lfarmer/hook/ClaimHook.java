package com.squezsaz.lfarmer.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface ClaimHook {

    String getName();

    boolean isAvailable();

    boolean canPlace(Player player, Location location);

    default String getPlaceDenyReason(Player player, Location location) {
        return canPlace(player, location) ? null : "messages.claim_deny_place";
    }

    boolean canAccess(Player player, Location location);

    UUID getClaimOwner(Location location);
}
