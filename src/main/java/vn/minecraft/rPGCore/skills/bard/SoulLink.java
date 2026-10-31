package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.GloomMelody;
import vn.minecraft.rPGCore.classes.impl.bard.melodies.MelodyUtils;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SoulLink implements ISkill {

    @Override
    public String getId() { return "bard_soul_link"; }

    @Override
    public String getName() { return "Khúc Cầu Hồn"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.SOUL_LANTERN; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public int getRequiredPlayerLevel() { return 30; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 140.0 + (level * 20.0);
    }

    @Override
    public long getCooldown(int level) {
        return (long) ((50.0 - (level * 2.0)) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        int reduction = 10 + (displayLevel * 10);
        int vulnerable = displayLevel * 5;
        double trueDmgRate = displayLevel * 0.1;

        List<String> lore = new java.util.ArrayList<>(Arrays.asList(
                "§7Kéo một bản nhạc với tần số u ám,",
                "§7khóa chặt linh hồn giữa sự sống và cái chết.",
                "",
                "§8▪ §fLoại Âm Tiết: §e[ Âm Cao ]",
                "§8▪ §fLoại Giai Điệu: §5[ U Ám ]",
                "",
                "§c[Thi Triển - Khúc Cầu Hồn]",
                "§f- Phóng ra một sợi xích linh hồn kết nối với",
                "§f  kẻ địch. Nếu đi xa quá §e10 Block§f, xích sẽ đứt.",
                "§f- Khi xích còn tồn tại, bạn nhận giảm §a" + reduction + "%§f",
                "§f  sát thương GÂY RA TỪ MỤC TIÊU ĐÓ.",
                "§f- Kẻ địch bị xích sẽ nhận thêm §c" + vulnerable + "%§f sát thương",
                "§f  từ mọi nguồn (Không cộng dồn nếu nhiều Nhạc Sư xích).",
                "§f- §e[Bào Mòn]:§f Mỗi giây, sợi xích rút đi §c" + trueDmgRate + "%§f",
                "§f  Máu Hiện Tại của mục tiêu thành Sát Thương Chuẩn.",
                "§f  §7(Chỉ tính sát thương này cho 1 Nhạc Sư đầu tiên xích)."
        ));

        if (displayLevel >= 5) {
            lore.addAll(Arrays.asList(
                    "",
                    "§6§l[Thức Tỉnh Tiềm Năng - Cấp 5]",
                    "§f- Nội tại: Mỗi 15s, tự động kết nối 1 đồng minh lân cận",
                    "§f  (Tối đa 3 người). Đồng minh nhận §c+10% Sát thương§f",
                    "§f  và §b+30% Tốc độ di chuyển§f."
            ));
        }

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        int maxCap = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_8") > 0 ? 4 : 3;
        final boolean willMelody = (ToneManager.getCurrentTones(player).size() + 1) >= maxCap && MelodyUtils.canTriggerMelody(player);

        if (willMelody && rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
            if (player.getHealth() / rpgPlayer.getTotalMaxHealth() > 0.90) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + getManaCost(level));
                player.sendMessage("§d§l[Hơi Thở Của Gió] §fThi triển không tốn Năng lượng!");
            }
        }

        final double reductionRate = 0.10 + (level * 0.10);
        final double vulnerableRate = level * 0.05;
        final double trueDmgPercent = level * 0.1;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 0.5f);

        // ==========================================
        // RAYCAST TÌM ĐIỂM CHẠM VÀ MỤC TIÊU
        // ==========================================
        Location pLoc = player.getEyeLocation();
        Vector dir = pLoc.getDirection().normalize().multiply(0.5);
        LivingEntity target = null;
        Location impactLoc = pLoc.clone();

        for (double i = 0; i < 15.0; i += 0.5) {
            impactLoc.add(dir);
            // [FIX 1.21.11 API]: Đổi REDSTONE thành DUST
            impactLoc.getWorld().spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.0f));

            // Check trúng block cứng -> Dừng tia
            if (impactLoc.getBlock().getType().isSolid()) break;

            for (Entity e : impactLoc.getWorld().getNearbyEntities(impactLoc, 0.8, 0.8, 0.8)) {
                if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                    target = (LivingEntity) e;
                    break;
                }
            }
            if (target != null) break;
        }

        // ==========================================
        // KÍCH HOẠT GIAI ĐIỆU HOẶC ÂM SẮC TẠI ĐIỂM CHẠM
        // ==========================================
        if (willMelody) {
            new GloomMelody().cast(player, rpgPlayer, impactLoc);
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.HIGH);
        }

        if (target != null) {
            startSoulLink(player, target, reductionRate, vulnerableRate, trueDmgPercent);
            player.sendMessage("§5§l[Tether] §fĐã khóa hồn mục tiêu: §c" + target.getName());
        } else {
            player.sendMessage("§c§l[!] §fKhông tìm thấy linh hồn để kết nối.");
        }

        if (level >= 5) {
            startAllyLinkAura(player, rpgPlayer);
        }

        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    private void startSoulLink(Player player, LivingEntity target, double redRate, double vulnRate, double trueDmgPct) {

        final String metaRed = "BARD_LINK_REDUCTION";
        final String metaTarget = "BARD_LINK_TARGET_UUID";
        final String metaVulnList = "BARD_LINK_VULNERABLE_LIST";
        final String metaDoTClaim = "BARD_SOUL_LINK_DOT_OWNER";

        player.setMetadata(metaRed, new FixedMetadataValue(RPGCore.getInstance(), redRate));
        player.setMetadata(metaTarget, new FixedMetadataValue(RPGCore.getInstance(), target.getUniqueId().toString()));

        List<String> vulnList = new ArrayList<>();
        if (target.hasMetadata(metaVulnList)) {
            Object obj = target.getMetadata(metaVulnList).get(0).value();
            if (obj instanceof List) vulnList = (List<String>) obj;
        }
        vulnList.add(player.getUniqueId().toString());
        target.setMetadata(metaVulnList, new FixedMetadataValue(RPGCore.getInstance(), vulnList));
        target.setMetadata("BARD_LINK_VULN_RATE", new FixedMetadataValue(RPGCore.getInstance(), vulnRate));

        // Kiểm tra độc quyền True Damage
        final boolean isDoTOwner = !target.hasMetadata(metaDoTClaim);
        if (isDoTOwner) {
            target.setMetadata(metaDoTClaim, new FixedMetadataValue(RPGCore.getInstance(), player.getUniqueId().toString()));
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || target.isDead() || !target.isValid()
                        || player.getLocation().distanceSquared(target.getLocation()) > 100.0) {
                    breakLink();
                    this.cancel();
                    return;
                }

                ticks++;

                if (ticks % 2 == 0) {
                    drawTether(player.getLocation().add(0, 1, 0), target.getLocation().add(0, 1, 0), Color.fromRGB(75, 0, 130));
                }

                if (ticks % 20 == 0 && isDoTOwner) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f);
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);

                    double currentHp = target.getHealth();
                    double trueDmg = currentHp * (trueDmgPct / 100.0);

                    player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, target, trueDmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                }
            }

            private void breakLink() {
                player.removeMetadata(metaRed, RPGCore.getInstance());
                player.removeMetadata(metaTarget, RPGCore.getInstance());

                if (target.isValid()) {
                    List<String> currentList = new ArrayList<>();
                    if (target.hasMetadata(metaVulnList)) {
                        Object obj = target.getMetadata(metaVulnList).get(0).value();
                        if (obj instanceof List) currentList = (List<String>) obj;
                    }
                    currentList.remove(player.getUniqueId().toString());

                    if (currentList.isEmpty()) {
                        target.removeMetadata(metaVulnList, RPGCore.getInstance());
                        target.removeMetadata("BARD_LINK_VULN_RATE", RPGCore.getInstance());
                    } else {
                        target.setMetadata(metaVulnList, new FixedMetadataValue(RPGCore.getInstance(), currentList));
                    }

                    if (isDoTOwner) target.removeMetadata(metaDoTClaim, RPGCore.getInstance());

                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                }
                player.sendMessage("§8§l[Tether] §7Liên kết linh hồn đã bị đứt.");
            }

        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void startAllyLinkAura(Player player, RPGPlayer rpgPlayer) {
        final String metaAura = "BARD_ALLY_LINK_ACTIVE";
        if (player.hasMetadata(metaAura)) return;
        player.setMetadata(metaAura, new FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            int auraTicks = 0;
            Set<UUID> linkedAllies = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    clearAllAllies();
                    player.removeMetadata(metaAura, RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                auraTicks++;

                if (auraTicks % 300 == 0 && linkedAllies.size() < 3) {
                    for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 10, 5, 10)) {
                        if (e instanceof Player && !e.equals(player) && !CombatManager.canTarget(player, e)) {
                            Player ally = (Player) e;
                            if (!linkedAllies.contains(ally.getUniqueId())) {
                                linkedAllies.add(ally.getUniqueId());
                                ally.sendMessage("§b§l[Tether] §fBạn được kết nối linh hồn với Nhạc Sư!");
                                break;
                            }
                        }
                    }
                }

                Set<UUID> toRemove = new HashSet<>();
                for (UUID uuid : linkedAllies) {
                    Player ally = org.bukkit.Bukkit.getPlayer(uuid);

                    if (ally == null || !ally.isOnline() || ally.isDead() || ally.getLocation().distanceSquared(player.getLocation()) > 100.0) {
                        toRemove.add(uuid);
                        if (ally != null && ally.isOnline()) {
                            ally.sendMessage("§8§l[Tether] §7Đứt liên kết linh hồn đồng minh.");
                            ally.removeMetadata("BARD_ALLY_DMG_BUFF", RPGCore.getInstance());
                            RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId()).updateMoveSpeed();
                        }
                        continue;
                    }

                    ally.setMetadata("BARD_ALLY_DMG_BUFF", new FixedMetadataValue(RPGCore.getInstance(), 0.10));
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, false));

                    if (auraTicks % 3 == 0) {
                        drawTether(player.getLocation().add(0, 1, 0), ally.getLocation().add(0, 1, 0), Color.fromRGB(0, 255, 255));
                    }
                }
                linkedAllies.removeAll(toRemove);
            }

            private void clearAllAllies() {
                for (UUID uuid : linkedAllies) {
                    Player ally = org.bukkit.Bukkit.getPlayer(uuid);
                    if (ally != null && ally.isOnline()) {
                        ally.removeMetadata("BARD_ALLY_DMG_BUFF", RPGCore.getInstance());
                    }
                }
                linkedAllies.clear();
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void drawTether(Location start, Location end, Color color) {
        Vector link = end.toVector().subtract(start.toVector());
        double distance = link.length();
        Vector step = link.normalize().multiply(0.5);
        Location current = start.clone();
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.8f);

        for (double i = 0; i < distance; i += 0.5) {
            current.add(step);
            // [FIX 1.21.11 API]: Đổi REDSTONE thành DUST
            current.getWorld().spawnParticle(Particle.DUST, current, 1, dust);
        }
    }
}