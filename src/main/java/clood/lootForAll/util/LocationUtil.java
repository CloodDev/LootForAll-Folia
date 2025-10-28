package clood.lootForAll.util;

import org.bukkit.Location;

import java.util.UUID;

public class LocationUtil {

  public static String createKey(Location loc, UUID player) {
    return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" +
        loc.getBlockZ() + ":" + player.toString();
  }

  public static String createLocationKey(Location loc) {
    return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
  }
}
