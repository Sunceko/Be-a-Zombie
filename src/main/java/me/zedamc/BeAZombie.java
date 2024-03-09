/*
 * This file is part of Be a Zombie.
 *
 * Be a Zombie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Be a Zombie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Be a Zombie.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.zedamc;

import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;


import java.util.*;


public final class BeAZombie extends JavaPlugin implements Listener {
    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private Team zombieTeam;
    FileConfiguration Config = getConfig();
    String NoChests = Config.getString("Messages.NoChests");
    String CanUseChests = Config.getString("Zombies.Cant-Use-Chests");
    String CanRespawn = Config.getString("Zombies.RespawnAsAZombie");
    String NoEatingThat = Config.getString("Messages.DontEatThat");
    String Yummy = Config.getString("Messages.Yummy");
    String Debug = Config.getString("Debug.debug");
    public Set<UUID> Zombies = new HashSet<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        System.out.println("[BZ] Enabled");
        manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            scoreboard = manager.getNewScoreboard();
            if (scoreboard != null) {
                zombieTeam = scoreboard.registerNewTeam("Zombies");
                zombieTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            } else {
                getLogger().warning("Failed to create scoreboard!");
            }
        } else {
            getLogger().warning("Failed to get scoreboard manager!");
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                checkPlayers();
            }
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        System.out.println("[BZ] Disabled");
    }
    @EventHandler
    public boolean onDeath(PlayerDeathEvent event) {
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) event
                    .getEntity().getLastDamageCause();

        if (!(nEvent.getDamager() instanceof Zombie)) {
            return false;
        }
        if (!(Zombies.contains(event.getEntity().getUniqueId()))) {
            AddZombie(event.getEntity().getUniqueId());
            Disguise(event.getEntity());
            JoinTeam(event.getEntity().getName());
        }
        }
        return true;
    }
    @EventHandler
    public void OnChestOpen(PlayerInteractEvent event) {
        if (event.hasBlock()) {
           if (event.getClickedBlock().getType() == Material.CHEST) {
               if (Zombies.contains(event.getPlayer().getUniqueId())) {
                   if (CanUseChests.equalsIgnoreCase("true")) {
                   event.setCancelled(true);
                   event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', NoChests));
               }
               }
        }
    }
    }

    @EventHandler
    public boolean onRespawn(PlayerRespawnEvent event) {
        if (Zombies.contains(event.getPlayer().getUniqueId())) {
           if (!(CanRespawn.equalsIgnoreCase("true"))) {
               RemoveZombie(event.getPlayer().getUniqueId(), event.getPlayer());
               UnDisguise(event.getPlayer());
               LeaveTeam(event.getPlayer().getName());
               return true;
           }
            if (CanRespawn.equalsIgnoreCase("true")) {
                AddZombie(event.getPlayer().getUniqueId());
                Disguise(event.getPlayer());
                JoinTeam(event.getPlayer().getName());

            }
        }
        return false;
    }
    @EventHandler
    public void NoAttack(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            if (Zombies.contains(event.getTarget().getUniqueId())) {
                event.setCancelled(true);
                if (Debug.equalsIgnoreCase("true")) {
                    System.out.println("Zombie is not attacking you anymore " + event.getTarget().getUniqueId());
                }
            }
        }
    }
    @EventHandler
    public void Eat(PlayerItemConsumeEvent event) {
       if (Zombies.contains(event.getPlayer().getUniqueId())) {
           if (event.getItem().getType() == Material.GOLDEN_APPLE || event.getItem().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
               RemoveZombie(event.getPlayer().getUniqueId(), event.getPlayer());
               if (Debug.equalsIgnoreCase("true")) {
                   System.out.println("Yay, you are not a zombie " + event.getPlayer().getUniqueId());
               }
           } else if (event.getItem().getType() == Material.ROTTEN_FLESH) {
               int food = event.getPlayer().getFoodLevel();
               event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Yummy));
               event.getPlayer().setFoodLevel(food+6);
               if (Debug.equalsIgnoreCase("true")) {
                   System.out.println("Rotten flesh eaten " + event.getPlayer().getUniqueId());
               }

           } else {
               double Health = event.getPlayer().getHealth();
               double NewHealth = Health - 1;
               event.setCancelled(true);
               event.getPlayer().setHealth(NewHealth);
               event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', NoEatingThat));
               if (Debug.equalsIgnoreCase("true")) {
                   System.out.println("You can't eat this " + event.getPlayer().getUniqueId());
               }
       }
       }
    }

    @EventHandler
    public boolean RIPEntity(EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) event
                    .getEntity().getLastDamageCause();

            if (!(nEvent.getDamager() instanceof Player)) {
                if (Debug.equalsIgnoreCase("true")) {
                    System.out.println("Damager is a player " + nEvent.getDamager().getUniqueId());
                }
                return false;
            }
            if (event.getEntity() instanceof Zombie) {
                if (Debug.equalsIgnoreCase("true")) {
                    System.out.println("No brains from zombies " + nEvent.getDamager().getUniqueId());
                }
                return false;
            }
            Player plr = ((Player) nEvent.getDamager()).getPlayer();
            ItemStack itemStack = new ItemStack(Material.ROTTEN_FLESH);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName("Brain");
            itemStack.setItemMeta(meta);
            plr.getInventory().addItem(itemStack);
            if (Debug.equalsIgnoreCase("true")) {
                System.out.println("It's in your inventory UUID: " + plr.getUniqueId());
            }
    }
        return false;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event)
    {
        if(event.isCancelled())
            return;
        if (Zombies.contains(event.getEntity().getUniqueId())) {
        event.setFoodLevel(event.getFoodLevel() - 4);
            if (Debug.equalsIgnoreCase("true")) {
                System.out.println("You lost a hunger " + event.getEntity().getUniqueId());
            }
    }
    }
    @EventHandler
    public void Join(PlayerJoinEvent event) {
        if (Zombies.contains(event.getPlayer().getUniqueId())) {
            Disguise(event.getPlayer());
            if (Debug.equalsIgnoreCase("true")) {
                System.out.println("Player joined a game and is a zombie according to this UUID: " + event.getPlayer().getUniqueId());
            }
        }
    }

    private void checkPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Zombies.contains(player.getUniqueId())) {
                if (player.getEquipment().getHelmet() == null && player.getWorld().getTime() >= 0
                        && player.getWorld().getTime() < 12300 && player.getLocation().getBlock().getLightFromSky() >= 15
                        && !player.getWorld().hasStorm()) {
                    player.setFireTicks(20);
                    if (Debug.equalsIgnoreCase("true")) {
                        getLogger().info("OMG you are burning, UUID: " + player.getUniqueId());
                    }
                }
            }
        }
    }


    public void Disguise(Player plr) {
        MobDisguise mob = new MobDisguise(DisguiseType.ZOMBIE);
       DisguiseAPI.disguiseEntity(plr.getPlayer(), mob);
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("You are disguised " + plr.getPlayer().getUniqueId());
        }
    }

    public void UnDisguise(Player plr) {
        DisguiseAPI.undisguiseToAll(plr.getPlayer());
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("Undisguised UUID: " + plr.getPlayer().getUniqueId());
        }
    }
    public void AddZombie(UUID UUID) {
      Zombies.add(UUID);
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("Added new zombie " + UUID);
        }

    }
    public void RemoveZombie(UUID UUID, Player plr) {
        Zombies.remove(UUID);
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("Removed a zombie " + plr.getPlayer().getUniqueId());
        }
        UnDisguise(plr);
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("Undisguised successfully " + plr.getPlayer().getUniqueId());
        }
        LeaveTeam(plr.getName());
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("Done with removing a zombie UUID, left a team: " + UUID);
        }
    }

    public void JoinTeam(String plr) {
        zombieTeam.addEntry(plr);
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("You joined a team " + plr);
        }
    }
    public void LeaveTeam(String plr) {
        zombieTeam.removeEntry(plr);
        if (Debug.equalsIgnoreCase("true")) {
            System.out.println("Left a team " + plr);
        }
    }
}
