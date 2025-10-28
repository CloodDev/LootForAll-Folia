package clood.lootForAll.listeners;

import clood.lootForAll.storage.InventoryStorage;
import clood.lootForAll.util.LocationUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.logging.Logger;

public class BlockListener implements Listener {

  private final InventoryStorage storage;
  private final Logger logger;

  public BlockListener(InventoryStorage storage, Logger logger) {
    this.storage = storage;
    this.logger = logger;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    BlockState state = event.getBlock().getState();
    if (!(state instanceof Chest) && !(state instanceof Barrel))
      return;

    Location loc = state.getLocation();
    if (hasPerPlayerInventories(loc)) {
      event.setCancelled(true);
      Player player = event.getPlayer();
      player.sendMessage(Component.text("§cYou cannot break this chest! It contains per-player loot."));
      if (logger != null)
        logger.info(
            "Prevented player " + player.getName() + " from breaking chest at " + LocationUtil.createLocationKey(loc));
    }
  }

  private boolean hasPerPlayerInventories(Location loc) {
    String locPrefix = LocationUtil.createLocationKey(loc) + ":";
    for (String key : storage.getPerPlayerChests().keySet()) {
      if (key.startsWith(locPrefix)) {
        return true;
      }
    }
    return false;
  }
}
