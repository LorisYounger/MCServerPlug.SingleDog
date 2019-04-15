package mcplug.singledog;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Maps;
public class Main extends JavaPlugin implements Listener{
    @Override  
    public void onEnable(){  
        //your code here.  
        this.getLogger().info("---SingleDog---\nDog==Wolf");  
        Bukkit.getPluginManager().registerEvents(this,this); //这里HelloWorld类是监听器, 将当前HelloWorld对象注册监听器  
    }  

    @Override      
    public void onDisable(){  
        //your code here.  
        this.getLogger().info("SingleDog Plugin OFF");  
    }
    
    @EventHandler
    public void PlayerDeathEvent(org.bukkit.event.entity.PlayerDeathEvent event) {
        //event.setDeathMessage("§l§4菜\n你打游戏像蔡徐坤");
        //((Player)event.getEntity()).sendTitle( "§4菜","§l§c你打游戏像蔡徐坤", 20, 50, 20);

        UUID uid =  event.getEntity().getUniqueId();
        for(int i=0;i<PostureList.size();i++) {
            if(uid == PostureList.get(i).user) {
                PostureList.remove(i--);
            }
        }
    }
    
    private final Map<Player, BossBar> bars = Maps.newHashMap();
    
    @EventHandler(ignoreCancelled = true) //这个注解告诉Bukkit这个方法正在监听某个事件, 玩家移动时Bukkit就会调用这个方法
    public void EntityDamageByEntityEvent(org.bukkit.event.entity.EntityDamageByEntityEvent event){
        //getLogger().info("玩家退出了！");
        if (!(event.getEntity() instanceof Damageable && event.getDamager() instanceof Damageable) )
            return;
        long dat = System.currentTimeMillis();// new Date().getTime();
        UUID uider = event.getDamager().getUniqueId();
        UUID uidee = event.getEntity().getUniqueId();
        
        //被打方架势
        Posture posde = null;
        Posture posder = null;
        for(int i=0;i<PostureList.size();i++) {
            if(uidee == PostureList.get(i).user) {
                posde = PostureList.get(i);
            }else if(uider == PostureList.get(i).user) {
                posder = PostureList.get(i);
            }else if(PostureList.get(i).Post() ==0) {
                PostureList.remove(i--);
            }
        }
        if (posde == null) {
            posde = new Posture(uidee);
            PostureList.add(posde);
        }
        if (posder == null) {
            posder = new Posture(uider);
            PostureList.add(posder);
        }
        
        
        for(int i=0;i<DamagerList.size();i++) {
            if(DamagerList.get(i).apptime +400 < dat) {
                DamagerList.remove(i--);
            }else {
                if (DamagerList.get(i).damager == uidee && DamagerList.get(i).damagee == uider) {
                    //成功弹反
                    //Bukkit.broadcastMessage("你弹反成功了！恭喜");
                    //((Damageable)event.getEntity()).damage(DamagerList.get(i).damage);
                    //增加被打方的架势
                    posde.AddPosture(DamagerList.get(i).damage);
                    //减少自己的架势条
                    posder.AddPosture(-DamagerList.get(i).damage);
                    
                    
                    if (event.getEntity() instanceof Player) {
                        //这是被弹的人
                        ((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, 1000, 0.1f);
                        ((Player)event.getEntity()).sendTitle( "§4危","", 4, 10, 8);
                        BossBar old = bars.get(((Player)event.getEntity()));
                        if (old != null) old.removeAll();
                        BossBar bar = Bukkit.createBossBar("", posde.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                        bar.setProgress(posde.PostToDouble() / 20);
                        bar.addPlayer((Player)event.getEntity());
                        bars.put((Player)event.getEntity(), bar);
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            bar.removeAll();
                            bars.remove((Player)event.getEntity());
                        }, 60);
                    }
                    if (event.getDamager() instanceof Player) {
                        //这是弹的人
                        ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 1000, 1.1f);
                        ((Player)event.getDamager()).sendTitle("", "§3⚔", 2, 10, 4);
                        BossBar old = bars.get(((Player)event.getDamager()));
                        if (old != null) old.removeAll();
                        BossBar bar = Bukkit.createBossBar("", posde.PostToDouble() > 15 ? BarColor.PURPLE : BarColor.BLUE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                        bar.setProgress(posde.PostToDouble() / 20);
                        bar.addPlayer((Player)event.getDamager());
                        bars.put((Player)event.getDamager(), bar);
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            bar.removeAll();
                            bars.remove((Player)event.getDamager());
                        }, 50);
                    }
                    //event.getDamager().setdi
                    DamagerList.remove(i);
                    DamagerList.add(new DamagerRecord(uider,uidee,event.getDamage()));
                    
                    if(posde.IsBreak()) {
                        //如果对方架势破了
                        event.setDamage(event.getDamage()*posde.Post()/20);//架势破了多架势/10的伤害
                    }else {
                        posde.AddPosture(event.getDamage());//如果架势没有破，就加架势
                        event.setDamage(event.getDamage() / 50);//伤害设置为/50
                    }
                    return;
                }
            }
        }
        //攻击者自己会增加10%的伤害架势
        posder.AddPosture(event.getDamage()/10);
        
        //没有弹反的+有架势统统伤害减半
        if(posde.IsBreak()) {
            if (event.getEntity() instanceof Player) {
                //这是被弹的人
                ((Damageable)event.getEntity()).damage(event.getDamage());
                ((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, 1000, 0.15f);
                ((Player)event.getEntity()).sendTitle( "§4破","", 4, 10, 8);
                BossBar old = bars.get(((Player)event.getEntity()));
                if (old != null) old.removePlayer((Player)event.getEntity());
                BossBar bar = Bukkit.createBossBar("", posde.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID, BarFlag.CREATE_FOG);
                bar.setProgress(posde.PostToDouble() / 20);
                bar.addPlayer((Player)event.getEntity());
                bars.put((Player)event.getEntity(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer((Player)event.getEntity());
                    bars.remove((Player)event.getEntity());
                }, 60);
            }
            if (event.getDamager() instanceof Player) {
                //这是弹的人
                ((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 1000, 1.1f);
                ((Player)event.getDamager()).sendTitle("", "§6⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getDamager()));
                if (old != null) old.removePlayer(((Player)event.getDamager()));
                BossBar bar = Bukkit.createBossBar("", posde.PostToDouble() > 15 ? BarColor.PURPLE : BarColor.BLUE, BarStyle.SOLID, BarFlag.CREATE_FOG);
                bar.setProgress(posde.PostToDouble() / 20);
                bar.addPlayer((Player)event.getDamager());
                bars.put((Player)event.getDamager(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer(((Player)event.getDamager()));
                    bars.remove((Player)event.getDamager());
                }, 50);
            }
            event.setDamage(event.getDamage()*posde.Post()/20);//架势破了多架势/10的伤害 但是不涨架势
        } else if (ThreadLocalRandom.current().nextBoolean()) {
            posde.AddPosture(event.getDamage());
            if (event.getEntity() instanceof Player) {
                //这是被弹的人
                //((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, 0.2f, 0.1f);
                ((Player)event.getEntity()).sendTitle( "","§7⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getEntity()));
                if (old != null) old.removePlayer((Player)event.getEntity());
                BossBar bar = Bukkit.createBossBar("", posde.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID);
                bar.setProgress(posde.PostToDouble() / 20);
                bar.addPlayer((Player)event.getEntity());
                bars.put((Player)event.getEntity(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer((Player)event.getEntity());
                    bars.remove((Player)event.getEntity());
                }, 40);
            }
            if (event.getDamager() instanceof Player) {
                //这是被弹的人
                //((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.1f);
                ((Player)event.getDamager()).sendTitle("", "§7⚔", 2, 10, 4);
                BossBar old = bars.get(((Player)event.getDamager()));
                if (old != null) old.removePlayer(((Player)event.getDamager()));
                BossBar bar = Bukkit.createBossBar("", posde.PostToDouble() > 10 ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SOLID);
                bar.setProgress(posde.PostToDouble() / 20);
                bar.addPlayer((Player)event.getDamager());
                bars.put((Player)event.getDamager(), bar);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    bar.removePlayer(((Player)event.getDamager()));
                    bars.remove((Player)event.getDamager());
                }, 40);
            }
            event.setDamage(event.getDamage()*0.1);//免疫90%伤害
        }
        
        DamagerList.add(new DamagerRecord(uider,uidee,event.getDamage()));
        
    }    
    public static ArrayList<DamagerRecord> DamagerList = new ArrayList<DamagerRecord>();
    public static ArrayList<Posture> PostureList = new ArrayList<Posture>();

    public class DamagerRecord {
        public UUID damager;
        public UUID damagee;
        public double damage;
        public long apptime;
        public DamagerRecord(UUID der,UUID dee,double de) {
            damager = der;
            damagee = dee;
            damage = de;
            apptime = System.currentTimeMillis();
        }
    }
    public class Posture{
        public UUID user;
        public double post =0;
        public long lastrecordtime;
        public Posture(UUID usr) {
            user = usr;
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
