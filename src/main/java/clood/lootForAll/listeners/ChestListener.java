package clood.lootForAll.listeners;

import clood.lootForAll.storage.InventoryStorage;
import clood.lootForAll.util.LocationUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Logger;

public class ChestListener implements Listener {

  private final InventoryStorage storage;
  private final Plugin plugin;
  private final Logger logger;

  public ChestListener(InventoryStorage storage, Plugin plugin, Logger logger) {
    this.storage = storage;
    this.plugin = plugin;
    this.logger = logger;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventoryOpen(InventoryOpenEvent event) {
    Inventory inv = event.getInventory();
    InventoryHolder holder = inv.getHolder();
    if (!(holder instanceof BlockState))
      return;
    BlockState state = (BlockState) holder;
    if (!(state instanceof Chest) && !(state instanceof Barrel))
      return;

    if (!(event.getPlayer() instanceof Player)) {
      if (logger != null)
        logger.fine("Inventory opener is not a Player, ignoring");
      return;
    }
    Player player = (Player) event.getPlayer();

    final UUID playerId = player.getUniqueId();
    final Location loc = state.getLocation();

    event.setCancelled(true);

    player.getScheduler().run(plugin, (task) -> {
      String locStr = LocationUtil.createLocationKey(loc);
      BlockState freshState = loc.getWorld().getBlockAt(loc).getState();
      if (!(freshState instanceof Chest) && !(freshState instanceof Barrel)) {
        if (logger != null)
          logger.fine("Block at " + locStr + " is no longer a container");
        return;
      }
      Inventory origInv = ((InventoryHolder) freshState).getInventory();

      String k = LocationUtil.createKey(loc, playerId);

      if (storage.getPerPlayerChests().containsKey(k)) {
        if (logger != null)
          logger.info("Opening existing private inventory for player " + playerId + " at " + locStr);
        player.openInventory(storage.getPerPlayerChests().get(k));
        return;
      }

      if (logger != null)
        logger.info("Creating private inventory for player " + playerId + " at " + locStr + " (size="
            + origInv.getSize() + ")");
      Inventory copy = Bukkit.createInventory(null, origInv.getSize(), Component.text("Chest"));
      copy.setContents(origInv.getContents());
      storage.getPerPlayerChests().put(k, copy);
      storage.saveAsync(k, copy);
      player.openInventory(copy);
    }, null);
  }
}
