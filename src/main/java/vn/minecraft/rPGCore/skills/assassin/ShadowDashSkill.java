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
import org.bukkit.metadata.FixedMetadataValue;
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
        Map<String, Integer> req = new HashMap<>();
        req.put("assassin_shadow_decoy", 6);
        req.put("assassin_phantom_blades",10);
        return req;
    }

    @Override
    public int getRequiredPlayerLevel() {
        return 32; // Yêu cầu Level 18
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

        int dmg = (int) Math.round(getDamageMultiplier(displayLevel) * 100);

        lore.add("§7Lướt nhanh về phía trước");
        lore.add("§7Gây sát thương: §c" + dmg + "% §7dọc đường lướt.");
        lore.add("");
        lore.add("§8[Nội tại - Chuỗi]:");
        lore.add("§7Nếu lướt trúng ít nhất 1 mục tiêu, sau §e0.6s");
        lore.add("§7tái kích hoạt khả năng lướt.");
        lore.add("");
        lore.add("§8[Nội tại - Làm mới]:");
        lore.add("§7Nếu lần lướt đầu tiên §cKhông trúng mục tiêu§7,");
        lore.add("§7hoàn lại §b40% Mana §7và §aGiảm 40% Hồi chiêu§7.");

        if (level < 8) {
            lore.add("");
            lore.add("§8§o(Nâng cấp giảm hồi chiêu và tăng sát thương)");
            if (level == 7) lore.add("§c§l(!) §cĐột phá Tối thượng (Cấp 8): Cần 3 SP");
        } else {
            lore.add("");
            lore.add("§6§l✦ ĐỘT PHÁ TỐI THƯỢNG ✦");
            lore.add("§eNếu lần lướt thứ 2 vẫn trúng mục tiêu,");
            lore.add("§esau 0.6s sẽ kích hoạt Lướt lần 3 gây");
            lore.add("§esát thương §c" + dmg*2 + "%§7.");
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

        // [ĐÃ SỬA]: Kỹ năng kích hoạt sẽ luôn phá Tàng hình (Bom Khói)
        if (player.hasMetadata("Assassin_Stealth") || player.hasMetadata("Assassin_Smoke_Bomb_Active")) {
            player.removeMetadata("Assassin_Stealth", plugin);
            player.removeMetadata("Assassin_Smoke_Bomb_Active", plugin);
            player.sendMessage("§c§l[!] Bạn đã hiện hình do tung kỹ năng!");
            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(plugin, player);
        }

        int fbLevel = 0;
        if (player.hasMetadata("Assassin_FatalBlow_Level")) {
            fbLevel = player.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
            player.removeMetadata("Assassin_FatalBlow_Level", plugin);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            player.sendMessage("§c§l[!] §fLướt Bóng Đêm được Cường hóa!");
        }

        player.sendMessage("§8§l[!] LƯỚT BÓNG ĐÊM!");

        performDash(player, rpgPlayer, level, 1, fbLevel);

        if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.AssassinClass) {
            ((vn.minecraft.rPGCore.classes.impl.AssassinClass) rpgPlayer.getCurrentClass()).addSkillStack(player);
        }

        return true;
    }

    private void performDash(final Player player, final RPGPlayer rpgPlayer, final int level, final int dashPhase, final int fbLevel) {
        if (!player.isOnline() || player.isDead()) return;

        double baseMulti = getDamageMultiplier(level);

        if (fbLevel > 0) {
            baseMulti += 2.00 + ((fbLevel - 1) * 0.15);
        }

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

            final Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.5f);

            @Override
            public void run() {
                Location currentLoc = player.getLocation();

                if (currentLoc.distanceSquared(startLoc) >= 100.0 || ticks > 3 || player.isDead() || !player.isOnline()) {
                    player.setVelocity(new Vector(0, 0, 0));
                    player.setFallDistance(0);
                    this.cancel();
                    handleDashEnd(player, rpgPlayer, level, dashPhase, hitAny, fbLevel);
                    return;
                }

                player.setVelocity(dir.clone().multiply(3.2).setY(0.05));

                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc.clone().add(0, 1, 0), 6, 0.2, 0.2, 0.2, purpleDust);
                currentLoc.getWorld().spawnParticle(Particle.WITCH, currentLoc.clone().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.05);

                if (currentLoc.distanceSquared(lastShadowLoc) >= 16.0) {
                    spawnShadowTrail(lastShadowLoc);
                    lastShadowLoc = currentLoc.clone();
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 2.0, 2.0, 2.0)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e) && !hitTargets.contains(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;
                        hitTargets.add(target.getUniqueId());
                        hitAny = true;

                        // [ĐÃ XÓA BUG 100K DAME]: Không truyền extraCrit 100.0 nữa.
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, 0.0, 0.0, false, false);
                        // [ĐÃ SỬA]: Bọc cờ khóa Combo Ảo
                        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                        target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleDashEnd(Player player, RPGPlayer rpgPlayer, int level, int dashPhase, boolean hitAny, int fbLevel) {
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
                scheduleNextDash(player, rpgPlayer, level, 2, fbLevel);
            } else if (dashPhase == 2 && level == 8) {
                scheduleNextDash(player, rpgPlayer, level, 3, fbLevel);
            }
        }
    }

    private void scheduleNextDash(final Player player, final RPGPlayer rpgPlayer, final int level, final int nextPhase, final int fbLevel) {
        player.sendMessage("§e§l[!] Nhắm mục tiêu! Lướt lần " + nextPhase + " sau 0.6s...");

        new BukkitRunnable() {
            int timer = 12; // 0.6 giây
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
                    performDash(player, rpgPlayer, level, nextPhase, fbLevel);
                    this.cancel();
                }
                timer--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnShadowTrail(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 1.5f);
        Particle.DustOptions darkPurple = new Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.0f);
        for (double y = 0.2; y <= 1.8; y += 0.4) {
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, y, 0), 1, 0.15, 0.1, 0.15, 0.01);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 2, 0.15, 0.1, 0.15, darkPurple);
        }
    }
}