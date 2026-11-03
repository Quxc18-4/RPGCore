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

    private static boolean isModifierRegistered = false;

    public SoulLink() {
        if (!isModifierRegistered) {
            CombatManager.registerCombatModifier(new SoulLinkCombatModifier());
            isModifierRegistered = true;
        }
    }

    @Override
    public String getId() { return "bard_soul_link"; }

    @Override
    public String getName() { return "Đồng Âm Cộng Hưởng"; }

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
        double trueDmgRate = displayLevel * 0.2;

        List<String> lore = new java.util.ArrayList<>(Arrays.asList(
                "§7Kéo một bản nhạc với giai điệu u ám,",
                "§7khóa chặt linh hồn giữa sự sống và cái chết.",
                "",
                "§8▪ §fLoại Âm Tiết: §e[ Âm Cao ]",
                "§8▪ §fLoại Giai Điệu: §5[ U Ám ]",
                "§8▪ §fPhạm vi: §e15 Block",
                "",
                "§c[Thi Triển - Khúc Cầu Hồn]",
                "§f- Phóng xích linh hồn vào mục tiêu. Nếu di chuyển",
                "§f  cách xa quá 15 Block, sợi dậy sẽ mất liên kết.",
                "§f- Bản thân được §aGiảm " + reduction + "%§f ST nhận vào từ mục tiêu",
                "§f  và kẻ địch chịu thêm §c" + vulnerable + "%§f ST từ mọi nguồn.",
                "",
                "§e[Nội tại - Ăn Mòn]",
                "§f- Mỗi giây, sợi xích bào mòn §c" + String.format("%.1f", trueDmgRate) + "%§f Máu Hiện Tại",
                "§f  của mục tiêu thành Sát Thương Chuẩn."
        ));

        if (displayLevel >= 5) {
            lore.addAll(Arrays.asList(
                    "",
                    "§6§l[Thức Tỉnh Tiềm Năng - Đồng Âm]",
                    "§f- Mỗi 10s chọn 1 đồng minh để kết nối (Tối đa 3).",
                    "§f- Nhận §c+10% Sát thương§f và §b+30% Tốc chạy§f."
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
        final double trueDmgPercent = level * 0.2;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 0.5f);

        Location pLoc = player.getEyeLocation();
        Vector dir = pLoc.getDirection().normalize().multiply(0.5);
        LivingEntity target = null;
        Location impactLoc = pLoc.clone();

        // Tầm Cast giảm còn 15
        for (double i = 0; i < 15.0; i += 0.5) {
            impactLoc.add(dir);
            impactLoc.getWorld().spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(Color.fromRGB(138, 43, 226), 0.5f));

            if (impactLoc.getBlock().getType().isSolid()) break;

            for (Entity e : impactLoc.getWorld().getNearbyEntities(impactLoc, 1.0, 1.0, 1.0)) {
                if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                    target = (LivingEntity) e;
                    break;
                }
            }
            if (target != null) {
                impactLoc = target.getLocation();
                break;
            }
        }

        if (willMelody) {
            new GloomMelody().cast(player, rpgPlayer, impactLoc);
            ToneManager.consumeTones(player, rpgPlayer);
            MelodyUtils.setMelodyCooldown(player);
        } else {
            ToneManager.addTone(player, rpgPlayer, ToneType.HIGH);
        }

        if (target != null) {
            startSoulLink(player, target, reductionRate, vulnerableRate, trueDmgPercent);
            player.sendMessage("§5§l[Cộng Hưởng] §fĐã khóa hồn mục tiêu: §c" + target.getName());
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

        final boolean isDoTOwner = !target.hasMetadata(metaDoTClaim);
        if (isDoTOwner) {
            target.setMetadata(metaDoTClaim, new FixedMetadataValue(RPGCore.getInstance(), player.getUniqueId().toString()));
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Giới hạn đứt xích giảm còn 15 Block (15^2 = 225.0)
                if (!player.isOnline() || player.isDead() || target.isDead() || !target.isValid()
                        || player.getLocation().distanceSquared(target.getLocation()) > 225.0) {
                    breakLink(true);
                    this.cancel();
                    return;
                }

                ticks++;

                // Vẽ dây: Mỗi 5 Ticks, Giãn hạt 1.0 Block
                if (ticks % 5 == 0) {
                    drawTether(player.getLocation().add(0, 1, 0), target.getLocation().add(0, 1, 0), Color.fromRGB(75, 0, 130));
                }

                if (ticks % 20 == 0 && isDoTOwner) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f);
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0.05);

                    double currentHp = target.getHealth();
                    double trueDmg = currentHp * (trueDmgPct / 100.0);

                    player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, target, trueDmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                }
            }

            private void breakLink(boolean forceBreak) {
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

                    // Hiệu ứng đứt xích cho Mục tiêu
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.2);

                    if (forceBreak && player.isOnline() && !player.isDead()) {
                        vn.minecraft.rPGCore.managers.StatusEffectManager.applyAbsoluteRoot(target, 20);
                    }
                }

                // Hiệu ứng đứt xích cho Nhạc Sư
                if (player.isOnline()) {
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_WOLF_ARMOR_BREAK, 1.0f, 0.5f);
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                    player.sendMessage("§8§l[Cộng Hưởng] §7Liên kết linh hồn đã bị đứt.");
                }
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

                // Tìm kiếm đồng minh lân cận (Phạm vi tìm kiếm 15 Block)
                if (auraTicks % 300 == 0 && linkedAllies.size() < 3) {
                    for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 15, 5, 15)) {
                        if (e instanceof Player && !e.equals(player) && !CombatManager.canTarget(player, e)) {
                            Player ally = (Player) e;
                            if (!linkedAllies.contains(ally.getUniqueId())) {
                                linkedAllies.add(ally.getUniqueId());
                                ally.sendMessage("§b§l[Đồng Âm] §fBạn được kết nối linh hồn với Nhạc Sư!");
                                break;
                            }
                        }
                    }
                }

                Set<UUID> toRemove = new HashSet<>();
                for (UUID uuid : linkedAllies) {
                    Player ally = org.bukkit.Bukkit.getPlayer(uuid);

                    // Tầm đứt xích đồng minh giảm còn 15 Block (15^2 = 225.0)
                    if (ally == null || !ally.isOnline() || ally.isDead() || ally.getLocation().distanceSquared(player.getLocation()) > 225.0) {
                        toRemove.add(uuid);
                        if (ally != null && ally.isOnline()) {
                            ally.sendMessage("§8§l[Đồng Âm] §7Đứt liên kết linh hồn đồng minh.");
                            ally.removeMetadata("BARD_ALLY_DMG_BUFF", RPGCore.getInstance());
                            RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId()).updateMoveSpeed();

                            ally.getWorld().playSound(ally.getLocation(), Sound.ITEM_WOLF_ARMOR_BREAK, 0.8f, 1.5f);
                            ally.getWorld().spawnParticle(Particle.ENCHANTED_HIT, ally.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                        }
                        continue;
                    }

                    // Tăng sát thương bằng Meta, Tăng tốc bằng siêu hàm Stats (chuẩn 30%)
                    ally.setMetadata("BARD_ALLY_DMG_BUFF", new FixedMetadataValue(RPGCore.getInstance(), 0.10));
                    if (auraTicks % 30 == 0) {
                        // Liên tục bồi tốc độ để chống hết hạn
                        vn.minecraft.rPGCore.managers.StatusEffectManager.applySpeed(ally, 40, 0.30);
                    }

                    if (auraTicks % 5 == 0) {
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
                        RPGCore.getInstance().getPlayerManager().getRPGPlayer(ally.getUniqueId()).updateMoveSpeed();
                    }
                }
                linkedAllies.clear();
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void drawTether(Location start, Location end, Color color) {
        Vector link = end.toVector().subtract(start.toVector());
        double distance = link.length();
        Vector step = link.normalize().multiply(1.0);
        Location current = start.clone();
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.8f);

        for (double i = 0; i < distance; i += 1.0) {
            current.add(step);
            current.getWorld().spawnParticle(Particle.DUST, current, 1, dust);
        }
    }

    // ==========================================
    // HOOK CHUẨN: XỬ LÝ NHẬN/GIẢM SÁT THƯƠNG
    // ==========================================
    public class SoulLinkCombatModifier implements CombatManager.ICombatModifier {

        @Override
        public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
            double modifier = 0.0;

            // 1. Quái nhận thêm Sát thương (Vulnerable)
            // Khi Victim là Quái vật bị xích: Âm % -> Nhận thêm Damage
            if (victim.hasMetadata("BARD_LINK_VULNERABLE_LIST") && victim.hasMetadata("BARD_LINK_VULN_RATE")) {
                List<String> list = (List<String>) victim.getMetadata("BARD_LINK_VULNERABLE_LIST").get(0).value();
                if (list != null && !list.isEmpty()) {
                    modifier -= victim.getMetadata("BARD_LINK_VULN_RATE").get(0).asDouble();
                }
            }

            // 2. Bard được Giảm Sát thương từ Quái bị xích (Reduction)
            // Khi Victim là Bard, Attacker là Quái vật đã xích Bard: Dương % -> Giảm Damage
            if (victim.hasMetadata("BARD_LINK_REDUCTION") && victim.hasMetadata("BARD_LINK_TARGET_UUID")) {
                String targetId = victim.getMetadata("BARD_LINK_TARGET_UUID").get(0).asString();
                if (attacker.getUniqueId().toString().equals(targetId)) {
                    modifier += victim.getMetadata("BARD_LINK_REDUCTION").get(0).asDouble();
                }
            }

            return modifier;
        }

        @Override
        public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
            // Tiềm năng cấp 5: Đồng minh được +10% Damage
            if (attacker.hasMetadata("BARD_ALLY_DMG_BUFF")) {
                return attacker.getMetadata("BARD_ALLY_DMG_BUFF").get(0).asDouble();
            }
            return 0.0;
        }
    }
}