package mcplug.singledog;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
public class Main extends JavaPlugin implements Listener{
	@Override  
    public void onEnable(){  
        //your code here.  
		this.getLogger().info("---SingleDog---\nDog==Wolf");  
        Bukkit.getPluginManager().registerEvents(this,this); //����HelloWorld���Ǽ�����, ����ǰHelloWorld����ע�������  
    }  

    @Override      
    public void onDisable(){  
        //your code here.  
		this.getLogger().info("SingleDog Plugin OFF");  
    }
    
    @EventHandler
    public void PlayerDeathEvent(org.bukkit.event.entity.PlayerDeathEvent event) {
    	//event.setDeathMessage("��l��4��\n�����Ϸ�������");
		((Player)event.getEntity()).sendTitle( "��4��","��l��c�����Ϸ�������", 20, 50, 20);

    	UUID uid =  event.getEntity().getUniqueId();
    	for(int i=0;i<PostureList.size();i++) {
    		if(uid == PostureList.get(i).user) {
    			PostureList.remove(i--);
    		}
    	}
    }
    
    @EventHandler //���ע�����Bukkit����������ڼ���ĳ���¼�, ����ƶ�ʱBukkit�ͻ�����������
    public void EntityDamageByEntityEvent(org.bukkit.event.entity.EntityDamageByEntityEvent event){
    	//getLogger().info("����˳��ˣ�");
    	if (!(event.getEntity() instanceof Damageable && event.getDamager() instanceof Damageable) )
    		return;
    	long dat = System.currentTimeMillis();// new Date().getTime();
    	UUID uider = event.getDamager().getUniqueId();
    	UUID uidee = event.getEntity().getUniqueId();
    	
    	//���򷽼���
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
    				//�ɹ�����
    				//Bukkit.broadcastMessage("�㵯���ɹ��ˣ���ϲ");
    				//((Damageable)event.getEntity()).damage(DamagerList.get(i).damage);
    				//���ӱ��򷽵ļ���
    				posde.AddPosture(DamagerList.get(i).damage);
    				//�����Լ��ļ�����
    				posder.AddPosture(-DamagerList.get(i).damage);
    				
    				
    				if (event.getEntity() instanceof Player) {
    					//���Ǳ�������
    					((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, (int)(DamagerList.get(i).damage*700),  (int)(DamagerList.get(i).damage));
    					((Player)event.getEntity()).sendTitle( "��6����","��l��4����"+posde.PostToString(), 4, 10, 8);
    				}
    				if (event.getDamager() instanceof Player) {
    					//���ǵ�����
    					((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE,  (int)(DamagerList.get(i).damage*700),  (int)(DamagerList.get(i).damage));
    					((Player)event.getDamager()).sendTitle("", "��l��6����", 2, 10, 4);
    				}
    				//event.getDamager().setdi
    				DamagerList.remove(i);
    		    	DamagerList.add(new DamagerRecord(uider,uidee,event.getDamage()));
    		    	
    		    	if(posde.IsBreak()) {
    					//����Է���������
    		    		event.setDamage(event.getDamage()*posde.Post()/20);//�������˶����/10���˺�
    				}else {
    		    		posde.AddPosture(event.getDamage());//�������û���ƣ��ͼӼ���
    		    		event.setDamage(0);//�˺�����Ϊ0
    				}
    				return;
    			}
    		}
    	}
    	//�������Լ�������10%���˺�����
    	posder.AddPosture(event.getDamage()/10);
    	
    	//û�е�����+�м���ͳͳ�˺�����
    	if(posde.IsBreak()) {
    		if (event.getEntity() instanceof Player) {
    			//���Ǳ�������
    			((Damageable)event.getEntity()).damage(event.getDamage());
    			((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, (int)(event.getDamage()*700),  (int)(event.getDamage()));
    			((Player)event.getEntity()).sendTitle( "��4Σ","��c�Ʒ�:"+posde.PostToString(), 4, 10, 8);
    		}
    		if (event.getDamager() instanceof Player) {
    			//���ǵ�����
    			((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE,  (int)(event.getDamage()),  (int)event.getDamage());
    			((Player)event.getDamager()).sendTitle("", "��1����", 2, 10, 4);
    		}
    		event.setDamage(event.getDamage()*posde.Post()/20);//�������˶����/10���˺� ���ǲ��Ǽ���
    	}else {
    		posde.AddPosture(event.getDamage());
    		if (event.getEntity() instanceof Player) {
    			//���Ǳ�������
    			((Player)event.getEntity()).playSound(((Player)event.getEntity()).getLocation(), Sound.BLOCK_ANVIL_FALL, (int)(event.getDamage()*700),  (int)(event.getDamage()));
    			((Player)event.getEntity()).sendTitle( "","��7����:"+posde.PostToString(), 2, 10, 4);
    		}
    		if (event.getDamager() instanceof Player) {
    			
    			//���ǵ�����
    			((Player)event.getDamager()).playSound(((Player)event.getDamager()).getLocation(), Sound.BLOCK_ANVIL_PLACE,  (int)(event.getDamage()*700),  (int)event.getDamage());
    			((Player)event.getDamager()).sendTitle("", "��9����", 2, 10, 4);
    		}
        	event.setDamage(event.getDamage()*0.1);//����90%�˺�
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
			return "����("+str+"/20)";
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
