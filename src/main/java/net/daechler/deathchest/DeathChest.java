package net.daechler.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class DeathChest extends JavaPlugin implements Listener {
    private HashMap<Location, Inventory> graveInventories;

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "DeathChest has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        graveInventories = new HashMap<>();
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "DeathChest has been disabled!");
    }

    // When player dies, store items in the skull.
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Block block = player.getWorld().getBlockAt(player.getLocation());
        block.setType(Material.PLAYER_HEAD);

        Skull skull = (Skull) block.getState();
        skull.setOwningPlayer(player);
        skull.update();

        Inventory graveInventory = Bukkit.createInventory(null, 54, "Grave");
        for(ItemStack item : event.getDrops()) {
            graveInventory.addItem(item);
        }
        event.getDrops().clear();

        graveInventories.put(block.getLocation(), graveInventory);
    }

    // When player interacts with the skull, open the inventory.
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return; // Only handle right click (interact) action

        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.PLAYER_HEAD) {
            Player player = event.getPlayer();
            Skull skull = (Skull) block.getState();
            if(skull.getOwningPlayer().getUniqueId().equals(player.getUniqueId())) {
                Inventory graveInventory = graveInventories.get(block.getLocation());
                if (graveInventory != null) {
                    player.openInventory(graveInventory);
                }
            }
        }
    }

    // When player closes the inventory, check if it is empty, if so, remove the grave and hashmap entry
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory closedInventory = event.getInventory();
        Location graveLocation = null;

        for (Location location : graveInventories.keySet()) {
            if (graveInventories.get(location).equals(closedInventory)) {
                graveLocation = location;
                break;
            }
        }

        if (graveLocation != null && isInventoryEmpty(closedInventory)) {
            Block block = graveLocation.getBlock();
            if (block.getType() == Material.PLAYER_HEAD) {
                block.setType(Material.AIR);
            }
            graveInventories.remove(graveLocation);
        }
    }

    // When player breaks a block, check if it is a grave. If so, drop the items, remove the hashmap entry, and cancel the event.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location blockLocation = block.getLocation();

        if (block.getType() == Material.PLAYER_HEAD && graveInventories.containsKey(blockLocation)) {
            Inventory graveInventory = graveInventories.get(blockLocation);
            for (ItemStack item : graveInventory.getContents()) {
                if (item != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            graveInventories.remove(blockLocation);
            event.setDropItems(false);  // Prevent the block from dropping items when it's broken
        }
    }

    // Helper function to check if an inventory is empty
    private boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack is : inv.getContents()) {
            if (is != null)
                return false;
        }
        return true;
    }
}
w