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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.entity.EntityExplodeEvent;

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

            if (skull.getOwningPlayer() != null && skull.getOwningPlayer().getUniqueId().equals(player.getUniqueId())) {
                if (player.isSneaking()) {  // Check if player is sneaking
                    Inventory graveInventory = graveInventories.get(block.getLocation());
                    if (graveInventory != null) {
                        // Loop through the items in the grave
                        for (ItemStack item : graveInventory.getContents()) {
                            if (item != null) {
                                // Check if the item is armor and can be equipped
                                switch (item.getType()) {
                                    case NETHERITE_HELMET:
                                    case DIAMOND_HELMET:
                                    case GOLDEN_HELMET:
                                    case IRON_HELMET:
                                    case CHAINMAIL_HELMET:
                                    case LEATHER_HELMET:
                                        equipIfSlotEmpty(player, item, EquipmentSlot.HEAD);
                                        break;
                                    case NETHERITE_CHESTPLATE:
                                    case DIAMOND_CHESTPLATE:
                                    case GOLDEN_CHESTPLATE:
                                    case IRON_CHESTPLATE:
                                    case CHAINMAIL_CHESTPLATE:
                                    case LEATHER_CHESTPLATE:
                                        equipIfSlotEmpty(player, item, EquipmentSlot.CHEST);
                                        break;
                                    case NETHERITE_LEGGINGS:
                                    case DIAMOND_LEGGINGS:
                                    case GOLDEN_LEGGINGS:
                                    case IRON_LEGGINGS:
                                    case CHAINMAIL_LEGGINGS:
                                    case LEATHER_LEGGINGS:
                                        equipIfSlotEmpty(player, item, EquipmentSlot.LEGS);
                                        break;
                                    case NETHERITE_BOOTS:
                                    case DIAMOND_BOOTS:
                                    case GOLDEN_BOOTS:
                                    case IRON_BOOTS:
                                    case CHAINMAIL_BOOTS:
                                    case LEATHER_BOOTS:
                                        equipIfSlotEmpty(player, item, EquipmentSlot.FEET);
                                        break;
                                    default:
                                        // If not armor, or armor slots are filled, try to add to the inventory
                                        HashMap<Integer, ItemStack> noSpaceFor = player.getInventory().addItem(item);
                                        if (!noSpaceFor.isEmpty()) {
                                            // If the player's inventory is full, leave the rest in the grave
                                            return;
                                        }
                                        break;
                                }
                                graveInventory.remove(item); // Remove the item from the grave inventory
                            }
                        }

                        // Check if the grave inventory is empty, if so, remove the grave
                        if (isInventoryEmpty(graveInventory)) {
                            block.setType(Material.AIR);
                            graveInventories.remove(block.getLocation());
                        }
                    }
                } else {
                    // If the player is not sneaking, they are just viewing the grave's inventory
                    Inventory graveInventory = graveInventories.get(block.getLocation());
                    if (graveInventory != null) {
                        player.openInventory(graveInventory);
                    }
                }
            }
        }
    }

    private void equipIfSlotEmpty(Player player, ItemStack item, EquipmentSlot slot) {
        ItemStack currentItem = player.getEquipment().getItem(slot);

        if (currentItem == null || currentItem.getType() == Material.AIR) {
            player.getEquipment().setItem(slot, item);
        } else {
            // If the slot is occupied, add the item to the player's inventory
            player.getInventory().addItem(item);
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Iterate through the list of blocks that were affected by the explosion
        for (int i = 0; i < event.blockList().size(); i++) {
            Block block = event.blockList().get(i);

            // Check if the exploded block is a player's grave
            if (block.getType() == Material.PLAYER_HEAD && graveInventories.containsKey(block.getLocation())) {
                // Prevent the explosion from affecting this specific block
                event.blockList().remove(i--); // Decrease i because we just removed an element from the list
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
