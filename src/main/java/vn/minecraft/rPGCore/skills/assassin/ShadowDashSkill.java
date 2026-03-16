package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ShadowDashSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ShadowDashSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_shadow_dash"; }
    @Override public String getName() { return "Lướt Bóng Đêm"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.PHANTOM_MEMBRANE; }

    @Override public int getMaxLevel() { return 8; }

    @Override
    public int getUpgradeCost(int currentLevel) {
        return (currentLevel == 7) ? 3 : 1;
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.emptyMap();
    }

    @Override
    public double getManaCost(int level) {
        if (level == 8) return 110.0;
        return 60.0 + ((level - 1) * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 18.0;
        if (level < 8) {
            cd -= (level - 1) * 0.35;
        } else {
            cd = 18.0 - (6 * 0.35) - 0.9;
        }
        return (long) (cd * 1000);
    }

    private double getDamageMultiplier(int level) {
        double base = 3.80;
        if (level < 8) {
            base += (level - 1) * 0.15;
        } else {
            base = 3.80 + (6 * 0.15) + 0.40;
        }
        return base;
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        double dmg = getDamageMultiplier(displayLevel) * 100;

        lore.add("§7Lao vút đi về phía trước, cứ mỗi 4m");
        lore.add("§7sẽ để lại một Tàng ảnh của bản thân.");
        lore.add("§7Gây sát thương: §c" + (int)dmg + "% §7dọc đường lướt.");
        lore.add("");
        lore.add("§8[Chuỗi Kết Liễu]:");
        lore.add("§7Nếu lướt trúng ít nhất 1 mục tiêu, sau §e0.5s");
        lore.add("§7bạn sẽ tự động lướt thêm 1 lần nữa về");
        lore.add("§7hướng đang nhìn.");
        lore.add("");
        lore.add("§8[Hoàn Trả Điểm Yếu]:");
        lore.add("§7Nếu lần lướt đầu tiên §cKhông trúng ai§7,");
        lore.add("§7hoàn lại §b40% Mana §7và §aGiảm 40% Hồi chiêu§7.");

        if (level < 8) {
            lore.add("");
            lore.add("§8§o(Nâng cấp giảm hồi chiêu và tăng sát thương)");
            if (level == 7) lore.add("§c§l(!) §cĐột phá Tối thượng (Cấp 8): Cần 3 SP");
        } else {
            lore.add("");
            lore.add("§6§l✦ ĐỘT PHÁ TỐI THƯỢNG ✦");
            lore.add("§eNếu lần lướt thứ 2 vẫn trúng mục tiêu,");
            lore.add("§esau 0.5s sẽ kích hoạt Lướt lần 3 với");
            lore.add("§esát thương được x2 (Nhân đôi)!");
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

        long currentTime = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (currentTime - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (currentTime - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        boolean fromStealth = false;
        if (player.hasMetadata("Assassin_Stealth")) {
            fromStealth = true;
            player.removeMetadata("Assassin_Stealth", plugin);
            player.sendMessage("§c§l[!] Đột kích từ bóng tối! Hiện hình!");
            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(plugin, player);
        }

        player.sendMessage("§8§l[!] LƯỚT BÓNG ĐÊM!");

        performDash(player, rpgPlayer, level, 1, fromStealth);

        return true;
    }

    // ==========================================
    // LOGIC LƯỚT VÀ GÂY SÁT THƯƠNG
    // ==========================================
    private void performDash(final Player player, final RPGPlayer rpgPlayer, final int level, final int dashPhase, final boolean fromStealth) {
        if (!player.isOnline() || player.isDead()) return;

        double baseMulti = getDamageMultiplier(level);
        final double finalDmgMulti = (dashPhase == 3) ? baseMulti * 2.0 : baseMulti;

        final Location startLoc = player.getLocation();

        Vector rawDir = player.getEyeLocation().getDirection();
        rawDir.setY(0);
        if (rawDir.lengthSquared() > 0) {
            rawDir.normalize();
        } else {
            rawDir = player.getLocation().getDirection().setY(0).normalize();
        }
        final Vector dir = rawDir;

        player.playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.8f);

        new BukkitRunnable() {
            int ticks = 0;
            Location lastShadowLoc = startLoc.clone();
            boolean hitAny = false;
            final Set<UUID> hitTargets = new HashSet<>();

            // Cài đặt hạt bụi màu tím rực rỡ cho dải lướt
            final Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.5f);

            @Override
            public void run() {
                if (ticks > 5 || player.isDead() || !player.isOnline()) {
                    player.setVelocity(new Vector(0, 0, 0));
                    player.setFallDistance(0);
                    this.cancel();
                    handleDashEnd(player, rpgPlayer, level, dashPhase, hitAny);
                    return;
                }

                // [ĐÃ GIẢM KHOẢNG CÁCH]: Giảm Velocity từ 2.6 xuống 2.1
                player.setVelocity(dir.clone().multiply(2.1).setY(0.05));
                Location currentLoc = player.getLocation();

                // [HIỆU ỨNG MỚI]: Nhả khói tím liên tục trong lúc lướt
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc.clone().add(0, 1, 0), 6, 0.2, 0.2, 0.2, purpleDust);
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc.clone().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.05);

                if (currentLoc.distance(lastShadowLoc) >= 4.0) {
                    spawnShadowTrail(lastShadowLoc);
                    lastShadowLoc = currentLoc.clone();
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 2.0, 2.0, 2.0)) {
                    if (e instanceof LivingEntity && e != player && !hitTargets.contains(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;
                        hitTargets.add(target.getUniqueId());
                        hitAny = true;

                        double extraCrit = (fromStealth) ? 100.0 : 0.0;

                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, extraCrit, 0.0, false, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);

                        // Hiệu ứng khi chém trúng địch (Thêm tia lửa tím)
                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                        target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // XỬ LÝ HẬU QUẢ SAU MỖI LẦN LƯỚT
    // ==========================================
    private void handleDashEnd(Player player, RPGPlayer rpgPlayer, int level, int dashPhase, boolean hitAny) {

        if (dashPhase == 1 && !hitAny) {
            double refundMana = getManaCost(level) * 0.40;
            double newMana = Math.min(rpgPlayer.getTotalMaxResource(), rpgPlayer.getCurrentResource() + refundMana);
            rpgPlayer.setCurrentResource(newMana);

            long now = System.currentTimeMillis();
            long fullCd = getCooldown(level);
            long reducedCd = (long) (fullCd * 0.60);

            cooldowns.put(player.getUniqueId(), now - fullCd + reducedCd);

            player.sendMessage("§b⚡ Lướt hụt! Hoàn lại " + (int)refundMana + " Mana và Giảm 40% Hồi chiêu!");
            return;
        }

        if (hitAny) {
            if (dashPhase == 1) {
                scheduleNextDash(player, rpgPlayer, level, 2);
            } else if (dashPhase == 2 && level == 8) {
                scheduleNextDash(player, rpgPlayer, level, 3);
            }
        }
    }

    // ==========================================
    // BỘ ĐẾM 0.5S CHUẨN BỊ CHO LƯỚT TIẾP THEO
    // ==========================================
    private void scheduleNextDash(final Player player, final RPGPlayer rpgPlayer, final int level, final int nextPhase) {
        player.sendMessage("§e§l[!] Nhắm mục tiêu! Lướt lần " + nextPhase + " sau 0.5s...");

        new BukkitRunnable() {
            int timer = 10;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                if (timer % 5 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                }

                if (timer <= 0) {
                    performDash(player, rpgPlayer, level, nextPhase, false);
                    this.cancel();
                }
                timer--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // VẼ TÀNG ẢNH (SHADOW TRAIL)
    // ==========================================
    private void spawnShadowTrail(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 1.5f);

        Particle.DustOptions darkPurple = new Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.0f);

        for (double y = 0.2; y <= 1.8; y += 0.4) {
            // [HIỆU ỨNG MỚI]: Tàng ảnh kết hợp giữa Mực đen và Bụi tím đậm
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, y, 0), 1, 0.15, 0.1, 0.15, 0.01);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 2, 0.15, 0.1, 0.15, darkPurple);
        }
    }
}