package clood.lootForAll.listeners;

import clood.lootForAll.storage.InventoryStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;
import java.util.logging.Logger;

public class PlayerListener implements Listener {

  private final InventoryStorage storage;
  private final Logger logger;

  public PlayerListener(InventoryStorage storage, Logger logger) {
    this.storage = storage;
    this.logger = logger;
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    String uuid = event.getPlayer().getUniqueId().toString();
    int removed = 0;
    Iterator<String> it = storage.getPerPlayerChests().keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      if (key.endsWith(":" + uuid)) {
        it.remove();
        removed++;
      }
    }
    storage.deletePlayerInventoriesAsync(uuid);
    if (logger != null)
      logger.info("Player " + uuid + " quit — removed " + removed + " per-player inventories from cache");
  }
}
