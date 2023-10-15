package net.daechler.deathchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
    private HashMap<String, Location> playerGraves;
    private HashMap<Location, Inventory> graveInventories;

    @Override
    public void onEnable() {
        getLogger().info("DeathChest has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        graveInventories = new HashMap<>();
        playerGraves = new HashMap<>();
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathChest has been disabled!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        Block block = findNearestAirBlock(deathLocation);
        block.setType(Material.PLAYER_HEAD);

        Skull skull = (Skull) block.getState();
        skull.setOwningPlayer(player);
        skull.update();

        Inventory graveInventory = Bukkit.createInventory(null, 54, "Grave");
        for(ItemStack item : event.getDrops()) {
            graveInventory.addItem(item);
        }
        event.getDrops().clear();

        Location graveLocation = block.getLocation();
        graveInventories.put(graveLocation, graveInventory);
        playerGraves.put(player.getName(), graveLocation);
    }

    private Block findNearestAirBlock(Location location) {
        Block block = location.getBlock();
        while (block.getType() != Material.AIR && block.getY() < 256) {
            block = block.getRelative(BlockFace.UP);
        }
        return block;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        getLogger().info("PlayerInteractEvent triggered");
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        getLogger().info("InventoryCloseEvent triggered");
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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        getLogger().info("BlockBreakEvent triggered");
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
            event.setDropItems(false);
        }
    }

    private boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack is : inv.getContents()) {
            if (is != null)
                return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        getLogger().info("Command received");
        if (command.getName().equalsIgnoreCase("grave") && sender instanceof Player) {
            Player player = (Player) sender;
            Location grave = playerGraves.get(player.getName());
            if (grave != null) {
                Location tpLocation = grave.clone().add(0.5, 1, 0.5); // Adding 0.5 to x and z to center player, 1 to y to place the player on top of the grave.
                tpLocation.setPitch(player.getLocation().getPitch());
                tpLocation.setYaw(player.getLocation().getYaw());
                player.teleport(tpLocation);
                player.sendMessage(ChatColor.GREEN + "You have been teleported to your grave.");
            } else {
                player.sendMessage(ChatColor.RED + "No grave found to teleport to.");
            }
            return true;
        }
        return false;
    }
}
