package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class PoisonDartSkill implements ISkill, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PoisonDartSkill(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override public String getId() { return "assassin_poison_dart"; }
    @Override public String getName() { return "Phi Tiêu Ám Sát"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.SPECTRAL_ARROW; }
    @Override public int getMaxLevel() { return 10; }

    @Override public int getUpgradeCost(int currentLevel) { return (currentLevel == 9) ? 2 : 1; }

    @Override
    public double getManaCost(int level) {
        double cost = 45.0;
        if (level >= 3) cost += 5.0;
        if (level >= 6) cost += 5.0;
        if (level >= 9) cost += 5.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 7.5;
        if (level >= 3) cd -= 0.5;
        if (level >= 6) cd -= 0.5;
        if (level >= 9) cd -= 0.5;
        return (long) (cd * 1000);
    }

    private long getPoisonDuration(int level) {
        double dur = 5.0;
        if (level >= 3) dur += 0.5;
        if (level >= 6) dur += 0.5;
        if (level >= 9) dur += 0.5;
        return (long) (dur * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        double dmg = 150 + ((level - 1) * 10);
        double poisonSecs = getPoisonDuration(level) / 1000.0;

        List<String> lore = new ArrayList<>();
        lore.add("§7Ném một phi tiêu tẩm kịch độc về phía trước.");
        lore.add("§7Gây sát thương: §c" + (int)dmg + "% §7Sát thương vũ khí.");
        lore.add("");
        lore.add("§8[Hiệu ứng Trúng Đích]:");
        lore.add("§7- Gây §2Độc I §7trong §a" + String.format("%.1f", poisonSecs) + "s§7.");
        lore.add("§7- §cCấm Hồi Phục: §7Chặn hoàn toàn khả năng");
        lore.add("§7  hồi máu của mục tiêu trong thời gian trúng độc.");

        if (level < 10) {
            lore.add("");
            lore.add("§8§o(Đột phá ở cấp 3, 6, 9)");
            if (level == 9) lore.add("§c§l(!) §cĐột phá Tối thượng (Cấp 10): Cần 2 SP");
        } else {
            lore.add("");
            lore.add("§6§l✦ ĐỘT PHÁ TỐI THƯỢNG ✦");
            lore.add("§e- Trúng đích gây §cChoáng (Trói chân) 2s§e.");
            lore.add("§e- Phi tiêu để lại một Bãi Độc (Bán kính 3m)");
            lore.add("§e  trên mặt đất trong 4s. Kẻ địch dẫm vào sẽ bị");
            lore.add("§e  nhiễm độc và tái tạo lại thời gian Cấm Hồi Phục.");
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long now = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (now - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), now);

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        Arrow dart = player.getWorld().spawnArrow(eye, dir, 3.0f, 0f);
        dart.setShooter(player);
        dart.setGravity(false);
        dart.setPierceLevel(0);

        dart.setMetadata("Assassin_Dart", new FixedMetadataValue(plugin, true));
        dart.setMetadata("Dart_Level", new FixedMetadataValue(plugin, level));

        // Rút thẻ Buff và nhét vào thân Phi Tiêu
        if (player.hasMetadata("Assassin_FatalBlow_Level")) {
            int fbLevel = player.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
            dart.setMetadata("Dart_BonusLevel", new FixedMetadataValue(plugin, fbLevel));

            player.removeMetadata("Assassin_FatalBlow_Level", plugin);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            player.sendMessage("§c§l[!] §fPhi Tiêu Ám Sát được Cường hóa!");
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!dart.isValid() || dart.isOnGround() || dart.isDead()) {
                    this.cancel();
                    return;
                }
                Location loc = dart.getLocation();
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.05, 0.05, 0.05, 0.01);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, new Particle.DustOptions(Color.LIME, 0.8f));
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() { if (dart.isValid()) dart.remove(); }
        }.runTaskLater(plugin, 60L);

        return true;
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("assassin_double_slash", 3);
    }

    @EventHandler
    public void onDartHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow dart = (Arrow) event.getEntity();
        if (!dart.hasMetadata("Assassin_Dart")) return;

        // [CHỐNG NHÂN ĐÔI]: Mũi tên chỉ được nổ 1 lần duy nhất
        if (dart.hasMetadata("Damage_Calculated")) return;
        dart.setMetadata("Damage_Calculated", new FixedMetadataValue(plugin, true));

        int level = dart.getMetadata("Dart_Level").get(0).asInt();
        Player shooter = (Player) dart.getShooter();
        RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(shooter.getUniqueId());
        if (rpgPlayer == null) return;

        long poisonDurationMs = getPoisonDuration(level);

        if (event.getHitEntity() instanceof LivingEntity && event.getHitEntity() != shooter) {
            LivingEntity target = (LivingEntity) event.getHitEntity();

            double dmgMulti = 1.50 + ((level - 1) * 0.10);
            if (dart.hasMetadata("Dart_BonusLevel")) {
                int fbLevel = dart.getMetadata("Dart_BonusLevel").get(0).asInt();
                dmgMulti += 2.00 + ((fbLevel - 1) * 0.15);
            }

            Vector hitDir = dart.getVelocity().setY(0).normalize();
            Vector targetDir = target.getLocation().getDirection().setY(0).normalize();
            double extraCrit = hitDir.dot(targetDir) > 0.5 ? 30.0 : 0.0;

            if (extraCrit > 0) target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 10);

            double finalDmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, dmgMulti, 0.0, extraCrit, 0.0, false, false);

            // [KẾT NỐI VỚI CLASS CHA ĐỂ XUYÊN I-FRAME VÀ KHÓA ĐÁNH TAY ẢO]
            CombatManager.applySkillDamage(shooter, target, finalDmg);

            applyPoisonAndAntiHeal(target, poisonDurationMs);

            if (level == 10) {
                StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10);
                target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            }
        }

        if (level == 10) {
            Location hitLoc = event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0.5, 1.05, 0.5) :
                    (event.getHitEntity() != null ? event.getHitEntity().getLocation() : dart.getLocation());
            createPoisonPuddle(hitLoc, shooter, poisonDurationMs);
        }

        dart.remove();
    }

    private void createPoisonPuddle(Location loc, Player shooter, long poisonDurationMs) {
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 80) { this.cancel(); return; }

                if (ticks % 10 == 0) {
                    for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3.0, 2.0, 3.0)) {
                        if (e instanceof LivingEntity && e != shooter) {
                            applyPoisonAndAntiHeal((LivingEntity) e, poisonDurationMs);
                        }
                    }
                }

                if (ticks % 5 == 0) {
                    Particle.DustOptions poisonColor = new Particle.DustOptions(Color.fromRGB(34, 139, 34), 1.2f);
                    for (int i = 0; i < 15; i++) {
                        double angle = 2 * Math.PI * Math.random();
                        double r = 3.0 * Math.sqrt(Math.random());
                        Location pLoc = loc.clone().add(r * Math.cos(angle), 0.1, r * Math.sin(angle));
                        loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0.2, 0, poisonColor);
                    }
                    for (int i = 0; i < 30; i++) {
                        double angle = 2 * Math.PI * i / 30;
                        Location pLoc = loc.clone().add(3.0 * Math.cos(angle), 0.1, 3.0 * Math.sin(angle));
                        loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, poisonColor);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyPoisonAndAntiHeal(LivingEntity target, long durationMs) {
        int durationTicks = (int) (durationMs / 50);
        StatusEffectManager.applyEffect(target, PotionEffectType.POISON, durationTicks, 0);

        target.setMetadata("Assassin_AntiHeal", new FixedMetadataValue(plugin, System.currentTimeMillis() + durationMs));
        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getEyeLocation(), 3, 0.3, 0.3, 0.3, 0);
    }

    @EventHandler
    public void onEntityRegen(EntityRegainHealthEvent event) {
        if (event.getEntity().hasMetadata("Assassin_AntiHeal")) {
            long expireTime = event.getEntity().getMetadata("Assassin_AntiHeal").get(0).asLong();
            if (System.currentTimeMillis() < expireTime) {
                event.setCancelled(true);
            } else {
                event.getEntity().removeMetadata("Assassin_AntiHeal", plugin);
            }
        }
    }
}