package mcplug.singledog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.swing.CellEditor;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
public class Main extends JavaPlugin implements Listener{
    private final Map<Player, BossBar> bars = Maps.newHashMap();
    private final Map<Entity, Posture> entityPostures = Maps.newHashMap();
    private final Table<Entity, Entity, DamageRecord> behaviourRecords = HashBasedTable.create();
    
    @Override  
    public void onEnable(){  
        Bukkit.getPluginManager().registerEvents(this, this);
    }  
    
    // cleanup
    @EventHandler
    public void PlayerDeathEvent(EntityDeathEvent event) {
        entityPostures.remove(event.getEntity());
    }
    
    @EventHandler
    public void PlayerDeathEvent(PlayerQuitEvent event) {
        entityPostures.remove(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event){
        Entity victim = event.getEntity();
        // ignore victim except monsters and players
        if (!(victim instanceof Monster || victim instanceof Player))
            return;
        
        Entity damager = event.getDamager();
        // ignore damager except monsters and players
        if (!(damager instanceof Monster || damager instanceof Player))
            return;
        
        long date = System.currentTimeMillis();
        UUID uider = event.getDamager().getUniqueId();
        UUID uidee = event.getEntity().getUniqueId();
        
        // capature postures for victim and damager
        Posture postureVictim = entityPostures.get(victim);
        if (postureVictim == null)
            entityPostures.put(victim, postureVictim = new Posture());
        
        Posture postureDamager = entityPostures.get(damager);
        if (postureDamager == null)
            entityPostures.put(damager, postureDamager = new Posture());
        
        /*
         * Knockback handling
         */
        // cleanup chance-missed records
        behaviourRecords.cellSet().removeIf(cell -> cell.getValue().time + 400 < date);
        
        // capature and consume corresponding record
        DamageRecord record = behaviourRecords.remove(victim, damager);
        
        // successful knockback
        if (record != null) {
            // update postures for victim and damager
            postureVictim.AddPosture(record.damage);
            postureDamager.AddPosture(-record.damage);
            
            if (postureVictim.IsBreak()) {
                // broke posture
                event.setDamage(event.getDamage() * (postureVictim.Post() / 20));
            } else {
                // not break yet
                postureVictim.AddPosture(event.getDamage());
                event.setDamage(event.getDamage() * 0.6);
            }
            
            // notify victim for posture increment
            if (victim.getType() == EntityType.PLAYER) {
                Player player = (Player) victim;
                
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, 1000, 0.1f);
                player.sendTitle( "§4危","", 4, 10, 8);
                
                showPostureBossBarFor(postureVictim, player);
            }
            
            // notify victim for success knockback
            if (damager.getType() == EntityType.PLAYER) {
                Player player = (Player) damager;
                
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1000, 1.1f);
                player.sendTitle("", "§3⚔", 2, 10, 4);
                
                showPostureBossBarFor(postureVictim, player);
            }
            
            // update the record anyway
            behaviourRecords.put(victim, damager, new DamageRecord(event.getDamage()));
            
            // here we go as handled as a successful knockback
            return;
        }
        
        /*
         * Non-knockback handling
         */
        // increase posture for damager
        postureDamager.AddPosture(event.getDamage() / 10);
        
        // broken victim posture
        if(postureVictim.IsBreak()) {
            // notify victim for broken posture
            if (victim.getType() == EntityType.PLAYER) {
                Player player = (Player) victim;
                
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_FALL, 1000, 0.15f);
                player.sendTitle("§4破","", 4, 10, 8);
                
                showPostureBossBarFor(postureVictim, player);
            }
            
            // notify damager for a non-knockback and posture-break attack
            if (damager.getType() == EntityType.PLAYER) {
                Player player = (Player) damager;
                
                ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 1000, 1.1f);
                ((Player)event.getDamager()).sendTitle("", "§6⚔", 2, 10, 4);
                
                showPostureBossBarFor(postureVictim, player);
            }
            
            // enhance damage for victim as broken posture
            event.setDamage(event.getDamage() * (1.0 + postureVictim.Post() / 20));
            
        // non-broken victim posture
        } else {
            // increase posture for victim as they have been attacked
            postureVictim.AddPosture(event.getDamage());
            
            // notify victim for posture increment
            if (victim.getType() == EntityType.PLAYER) {
                Player player = (Player) victim;
                
                if (event.getDamage() > ThreadLocalRandom.current().nextDouble() * 5)
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.1f);
                
                showPostureBossBarFor(postureVictim, player);
            }
            
            // notify damager for a non-knockback and non-posture-break attack
            if (damager.getType() == EntityType.PLAYER) {
                Player player = (Player) damager;
                
                if (event.getDamage() > ThreadLocalRandom.current().nextDouble()*5)
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.1f);
                
                showPostureBossBarFor(postureVictim, player);
            }
            
            // decrease damage for victim as there is posture
            event.setDamage(event.getDamage() * 0.4);
        }
        
        // update the record anyway
        behaviourRecords.put(victim, damager, new DamageRecord(event.getDamage()));
    }    
    
    private void showPostureBossBarFor(Posture posture, Player player) {
        /*
         * Bossbar stuffs
         */
        BossBar previousBar = bars.get(player);
        
        // throw away previous bar
        if (previousBar != null)
            previousBar.removeAll();
        
        // update new bar for victim
        BossBar bar = Bukkit.createBossBar("", posture.formattedPosture() > 10 ? BarColor.PURPLE : BarColor.BLUE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
        bar.setProgress(posture.bossbarProgress());
        bar.addPlayer(player);
        bars.put(player, bar);
        
        // pending bar cleanup
        Bukkit.getScheduler().runTaskLater(this, () -> {
            bar.removeAll();
            bars.remove(player);
        }, 60);
    }
    
    private static class DamageRecord {
        private final long time = System.currentTimeMillis();
        private final double damage;
        
        public DamageRecord(double damage) {
            this.damage = damage;
        }
    }
    
    public static class Posture {
        private static final DecimalFormat POSTURE_FORMAT = new DecimalFormat("#.0");
        public double post = 0;
        public long lastrecordtime = System.currentTimeMillis();
        
        public double formattedPosture() {
            return Double.valueOf(POSTURE_FORMAT.format(Post())).doubleValue();
        }
        
        public double bossbarProgress() {
            double progress = formattedPosture() / 20;
            return progress > 1 ? 1 : progress;
        }
        
        public double Post() {
            post -= (System.currentTimeMillis() - lastrecordtime) / 1000;
            if (post<0)
                post =0;
            lastrecordtime = System.currentTimeMillis();
            return post;
        }
        
        public boolean IsBreak() {
            return Post()>=20;
        }
        
        public void AddPosture(double damage) {
            Post();
            post += (int)damage;
        }
    }
    
}
