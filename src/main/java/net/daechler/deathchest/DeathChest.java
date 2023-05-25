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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class DeathChest extends JavaPlugin implements Listener {
    private HashMap<String, Inventory> graveInventories;

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

        graveInventories.put(block.getLocation().toString(), graveInventory);
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
                Inventory graveInventory = graveInventories.get(block.getLocation().toString());
                if (graveInventory != null) {
                    player.openInventory(graveInventory);
                }
            }
        }
    }
}
