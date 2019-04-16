package mcplug.singledog;

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
    private final Table<Entity, Entity, DamagerRecord> behaviourRecords = HashBasedTable.create();
    
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
        behaviourRecords.cellSet().removeIf(cell -> cell.getValue().apptime + 400 < date);
        
        // capature and consume corresponding record
        DamagerRecord record = behaviourRecords.remove(victim, damager);
        
        // successful knockback
        if (record != null) {
            // update postures for victim and damager
            postureVictim.AddPosture(record.damage);
            postureDamager.AddPosture(-record.damage);
            
            // notify victim for posture increment
            if (victim.getType() == EntityType.PLAYER) {
                ((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, 1000, 0.1f);
                ((Player)event.getEntity()).sendTitle( "§4危","", 4, 10, 8);
                BossBar old = bars.get(((Player)event.getEntity()));
                if (old != null) old.removePlayer((Player)event.getEntity());
                BossBar bar = Bukkit.createBossBar("", postureVictim.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                bar.setProgress(postureVictim.PostToDouble() / 20);
                bar.addPlayer((Player)event.getEntity());
                bars.put((Player)event.getEntity(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer((Player)event.getEntity());
                    bars.remove((Player)event.getEntity());
                }, 60);
            }
            
            // notify victim for success knockback
            if (damager.getType() == EntityType.PLAYER) {
                ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 1000, 1.1f);
                ((Player)event.getDamager()).sendTitle("", "§3⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getDamager()));
                if (old != null) old.removePlayer(((Player)event.getDamager()));
                BossBar bar = Bukkit.createBossBar("", postureVictim.PostToDouble() > 15 ? BarColor.PURPLE : BarColor.BLUE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                bar.setProgress(postureVictim.PostToDouble() / 20);
                bar.addPlayer((Player)event.getDamager());
                bars.put((Player)event.getDamager(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer(((Player)event.getDamager()));
                    bars.remove((Player)event.getDamager());
                }, 50);
            }
            
            if(postureVictim.IsBreak()) {
                // broke posture
                event.setDamage(event.getDamage() * (postureVictim.Post() / 20));
            } else {
                // not break yet
                postureVictim.AddPosture(event.getDamage());
                event.setDamage(event.getDamage() * 0.6);
            }
            
            // update the record anyway
            behaviourRecords.put(victim, damager, new DamagerRecord(event.getDamage()));
            
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
            if (event.getEntity() instanceof Player) {
                ((Damageable)event.getEntity()).damage(event.getDamage());
                ((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, 1000, 0.15f);
                ((Player)event.getEntity()).sendTitle( "§4破","", 4, 10, 8);
                BossBar old = bars.get(((Player)event.getEntity()));
                if (old != null) old.removePlayer((Player)event.getEntity());
                BossBar bar = Bukkit.createBossBar("", postureVictim.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID, BarFlag.CREATE_FOG);
                bar.setProgress(postureVictim.PostToDouble() / 20);
                bar.addPlayer((Player)event.getEntity());
                bars.put((Player)event.getEntity(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer((Player)event.getEntity());
                    bars.remove((Player)event.getEntity());
                }, 60);
            }
            
            // notify damager for a non-knockback and posture-break attack
            if (event.getDamager() instanceof Player) {
                ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 1000, 1.1f);
                ((Player)event.getDamager()).sendTitle("", "§6⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getDamager()));
                if (old != null) old.removePlayer(((Player)event.getDamager()));
                BossBar bar = Bukkit.createBossBar("", postureVictim.PostToDouble() > 15 ? BarColor.PURPLE : BarColor.BLUE, BarStyle.SOLID, BarFlag.CREATE_FOG);
                bar.setProgress(postureVictim.PostToDouble() / 20);
                bar.addPlayer((Player)event.getDamager());
                bars.put((Player)event.getDamager(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer(((Player)event.getDamager()));
                    bars.remove((Player)event.getDamager());
                }, 50);
            }
            
            // enhance damage for victim as broken posture
            event.setDamage(event.getDamage() * (1.0 + postureVictim.Post() / 20));
            
        // non-broken victim posture
        } else {
            // increase posture for victim as they have been attacked
            postureVictim.AddPosture(event.getDamage());
            
            // notify victim for posture increment
            if (event.getEntity() instanceof Player) {
                if (event.getDamage() > ThreadLocalRandom.current().nextDouble()*5)
                    ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.1f);
                
                ((Player)event.getEntity()).sendTitle( "","§7⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getEntity()));
                if (old != null) old.removePlayer((Player)event.getEntity());
                BossBar bar = Bukkit.createBossBar("", postureVictim.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID);
                bar.setProgress(postureVictim.PostToDouble() / 20);
                bar.addPlayer((Player)event.getEntity());
                bars.put((Player)event.getEntity(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer((Player)event.getEntity());
                    bars.remove((Player)event.getEntity());
                }, 40);
            }
            
            // notify damager for a non-knockback and non-posture-break attack
            if (event.getDamager() instanceof Player) {
                //这是被弹的人
                if (event.getDamage() > ThreadLocalRandom.current().nextDouble()*5)
                    ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.1f);
                
                ((Player)event.getDamager()).sendTitle("", "§7⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getDamager()));
                if (old != null) old.removePlayer(((Player)event.getDamager()));
                BossBar bar = Bukkit.createBossBar("", postureVictim.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID);
                bar.setProgress(postureVictim.PostToDouble() / 20);
                bar.addPlayer((Player)event.getDamager());
                bars.put((Player)event.getDamager(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer(((Player)event.getDamager()));
                    bars.remove((Player)event.getDamager());
                }, 40);
            }
            
            // decrease damage for victim as there is posture
            event.setDamage(event.getDamage() * 0.4);
        }
        
        // update the record anyway
        behaviourRecords.put(victim, damager, new DamagerRecord(event.getDamage()));
    }    
    
    public class DamagerRecord {
        public double damage;
        public long apptime;
        public DamagerRecord(double de) {
            damage = de;
            apptime = System.currentTimeMillis();
        }
    }
    
    public class Posture{
        public double post =0;
        public long lastrecordtime;
        public Posture() {
            lastrecordtime =System.currentTimeMillis();
        }
        public String PostToString(){
            Formatter formatter = new Formatter();
            String str =formatter.format("%.1f", Post()).toString();
            formatter.close();
            return "架势 ("+str+"/20)";
        }
        public double PostToDouble(){
            Formatter formatter = new Formatter();
            String str =formatter.format("%.1f", Post()).toString();
            formatter.close();
            return Double.valueOf(str) > 20 ? 20 :Double.valueOf(str);
        }
        public double Post() {
            post -= (System.currentTimeMillis()-lastrecordtime)/1000;
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
