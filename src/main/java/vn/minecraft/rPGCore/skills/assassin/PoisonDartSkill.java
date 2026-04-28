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
import vn.minecraft.rPGCore.RPGCore;
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

    @Override
    public List<String> getDescription(int level) {
        double dmg = 150 + ((level - 1) * 20); // Cấp 10 = 330%

        List<String> lore = new ArrayList<>();
        lore.add("§7Ném một phi tiêu tẩm kịch độc về phía trước.");
        lore.add("§7Gây sát thương: §c" + (int)dmg + "% §7Sát thương vũ khí.");
        lore.add("");
        lore.add("§8[Hiệu ứng Trúng Đích]:");
        lore.add("§7- Gây §2Độc I §7trong §a7.0s§7.");
        lore.add("§7- §cCấm Hồi Phục: §7Chặn hoàn toàn khả năng");
        lore.add("§7  hồi máu của mục tiêu trong thời gian trúng độc.");

        if (level < 10) {
            lore.add("");
            lore.add("§8§o(Đột phá ở cấp 3, 6, 9)");
            if (level == 9) lore.add("§c§l(!) §cĐột phá Tối thượng (Cấp 10): Cần 2 SP");
        } else {
            lore.add("");
            lore.add("§6§l✦ ĐỘT PHÁ TỐI THƯỢNG ✦");
            lore.add("§e- Trúng mục tiêu gây §cChoáng 3s§e.");
            lore.add("§e- Bãi độc phát nổ với Bán kính 4m (Duy trì 3.5s):");
            lore.add("§e  + Mục tiêu đứng bên trong mỗi giây có tỉ lệ");
            lore.add("§e    10/20/30% tiến hóa độc thành §2Độc II (4s)§e.");
            lore.add("§e  + Kẻ địch bước vào từ bên ngoài có tỉ lệ");
            lore.add("§e    20/40/60% lây nhiễm §2Độc I§e.");
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

        // 2. KIỂM TRA HỒI CHIÊU (Áp dụng ý tưởng của bạn)
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        // 3. TRỪ NĂNG LƯỢNG VÀ LƯU COOLDOWN
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        // ==========================================
        // [PHÁ ẨN THÂN]: LỘ DIỆN KHI TUNG CHIÊU TẤN CÔNG
        // ==========================================
        ISkill stealthSkill = RPGCore.getInstance().getSkillManager().getSkill("assassin_smoke_bomb");
        if (stealthSkill instanceof SmokeBombSkill) {
            ((SmokeBombSkill) stealthSkill).breakStealth(player);
        }

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        Arrow dart = player.getWorld().spawnArrow(eye, dir, 3.0f, 0f);
        dart.setShooter(player);
        dart.setGravity(false);
        dart.setPierceLevel(0);

        dart.setMetadata("Assassin_Dart", new FixedMetadataValue(plugin, true));
        dart.setMetadata("Dart_Level", new FixedMetadataValue(plugin, level));

        if (player.hasMetadata("Assassin_Stealth") || player.hasMetadata("Assassin_Smoke_Bomb_Active")) {
            player.removeMetadata("Assassin_Stealth", plugin);
            player.removeMetadata("Assassin_Smoke_Bomb_Active", plugin);
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) p.showPlayer(plugin, player);
        }

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

        if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.AssassinClass) {
            ((vn.minecraft.rPGCore.classes.impl.AssassinClass) rpgPlayer.getCurrentClass()).addSkillStack(player);
        }

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

        if (dart.hasMetadata("Damage_Calculated")) return;
        dart.setMetadata("Damage_Calculated", new FixedMetadataValue(plugin, true));

        int level = dart.getMetadata("Dart_Level").get(0).asInt();
        Player shooter = (Player) dart.getShooter();
        RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(shooter.getUniqueId());
        if (rpgPlayer == null) return;

        if (event.getHitEntity() instanceof LivingEntity && event.getHitEntity() != shooter) {
            LivingEntity target = (LivingEntity) event.getHitEntity();

            double dmgMulti = 1.50 + ((level - 1) * 0.20);
            if (dart.hasMetadata("Dart_BonusLevel")) {
                int fbLevel = dart.getMetadata("Dart_BonusLevel").get(0).asInt();
                dmgMulti += 2.00 + ((fbLevel - 1) * 0.15);
            }

            Vector hitDir = dart.getVelocity().setY(0).normalize();
            Vector targetDir = target.getLocation().getDirection().setY(0).normalize();
            double extraCrit = hitDir.dot(targetDir) > 0.5 ? 30.0 : 0.0;

            if (extraCrit > 0) target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 10);

            double finalDmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, dmgMulti, 0.0, extraCrit, 0.0, false, false);
            CombatManager.applySkillDamage(shooter, target, finalDmg);

            applyPoisonAndAntiHeal(target, 7000); // Cứng 7 giây

            if (level == 10) {
                StatusEffectManager.applyEffect(target, PotionEffectType.NAUSEA, 60, 0);
                StatusEffectManager.applyEffect(target, PotionEffectType.DARKNESS, 60, 0);
                // Trói tuyệt đối (Slow 100%)
                StatusEffectManager.applySlow(target, 60, 1.0, 10);
                target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            }
        }

        if (level == 10) {
            Location hitLoc = event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0.5, 1.05, 0.5) :
                    (event.getHitEntity() != null ? event.getHitEntity().getLocation() : dart.getLocation());
            createPoisonPuddle(hitLoc, shooter);
        }

        dart.remove();
    }

    private void createPoisonPuddle(Location loc, Player shooter) {
        Map<UUID, Integer> targetTicks = new HashMap<>();
        Particle.DustOptions poisonColor = new Particle.DustOptions(Color.fromRGB(34, 139, 34), 1.2f);

        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 70) { this.cancel(); return; } // Tồn tại 70 tick (3.5 giây)

                if (ticks % 20 == 0) { // Quét mỗi giây
                    for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 4.0, 2.0, 4.0)) {
                        if (e instanceof LivingEntity && e != shooter) {
                            LivingEntity target = (LivingEntity) e;
                            int timeInside = targetTicks.getOrDefault(target.getUniqueId(), 0) + 1;
                            targetTicks.put(target.getUniqueId(), timeInside);

                            if (target.hasPotionEffect(PotionEffectType.POISON)) {
                                if (target.getPotionEffect(PotionEffectType.POISON).getAmplifier() == 0) {
                                    double evolveChance = Math.min(0.30, timeInside * 0.10);
                                    if (Math.random() <= evolveChance) {
                                        StatusEffectManager.applyEffect(target, PotionEffectType.POISON, 80, 1); // Độc 2 trong 4s
                                        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getEyeLocation(), 3);
                                    }
                                }
                            } else {
                                double infectChance = Math.min(0.60, timeInside * 0.20);
                                if (Math.random() <= infectChance) {
                                    applyPoisonAndAntiHeal(target, 7000);
                                }
                            }
                        }
                    }
                }

                if (ticks % 5 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = 2 * Math.PI * Math.random();
                        double r = 4.0 * Math.sqrt(Math.random());
                        Location pLoc = loc.clone().add(r * Math.cos(angle), 0.1, r * Math.sin(angle));
                        loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0.2, 0, poisonColor);
                    }
                    for (int i = 0; i < 40; i++) { // Viền 4m
                        double angle = 2 * Math.PI * i / 40;
                        Location pLoc = loc.clone().add(4.0 * Math.cos(angle), 0.1, 4.0 * Math.sin(angle));
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

        // [ĐÃ SỬA]: Chuyển quyền quản lý cho StatusEffectManager để nó tự lo vụ thông báo
        StatusEffectManager.applyAntiHeal(target, durationTicks);

        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getEyeLocation(), 3, 0.3, 0.3, 0.3, 0);
    }

    @EventHandler
    public void onEntityRegen(EntityRegainHealthEvent event) {
        // [ĐÃ SỬA]: Dùng hàm kiểm tra chung
        if (event.getEntity() instanceof LivingEntity && StatusEffectManager.isHealingBlocked((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @Override
    public int getRequiredPlayerLevel() {
        return 6; // Yêu cầu Level 6
    }
}