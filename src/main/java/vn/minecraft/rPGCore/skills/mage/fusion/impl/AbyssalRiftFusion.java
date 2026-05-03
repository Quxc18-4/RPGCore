package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.*;

public class AbyssalRiftFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.DARK; }
    @Override public MageElement getElement2() { return MageElement.DARK; }
    @Override public String getReactionName() { return "§8§lVực Thẳm Khởi Nguyên"; }

    @Override public double getDamageMultiplier() { return 1.20; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        int duration = 6;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Vực Thẳm Khởi Nguyên]",
                "§f- Giáng một luồng ma lực hắc ám càn lướt trên mặt đất,",
                "§f  tạo ra những cột năng lượng trồi lên xé toạc địa hình.",
                "§f- Kẻ địch trúng cột hắc ám chịu §c" + (int)totalDmg + "% §fSát thương.",
                "",
                "§f§b[Nội tại - Lời Nguyền Vực Thẳm]",
                "§f- Để lại một §8Rãnh Nứt Vực Sâu§f tồn tại trong §a" + duration + "s§f.",
                "§f- Kẻ địch đứng trên rãnh nứt sẽ chịu hiệu ứng xấu:",
                "§f  Giảm §e10% Sát thương §fgây ra§f và giảm §e20% tốc chạy§f."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location startLoc = caster.getLocation();
        Vector direction = startLoc.getDirection().setY(0).normalize();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.5f);

        executeAbyssalWave(caster, rpgPlayer, startLoc, direction, totalDmgMulti);
    }

    private void executeAbyssalWave(Player caster, RPGPlayer rpgPlayer, Location start, Vector direction, double dmgMulti) {
        List<Location> riftPath = new ArrayList<>();

        // [ANTI-DUP]: Lớp 1 - HashSet nội bộ
        Set<UUID> hitTargets = new HashSet<>();
        // [ANTI-DUP]: Lớp 2 - Metadata ngoại bộ để khóa sát thương tuyệt đối
        String hitLockKey = "Abyssal_Hit_" + caster.getUniqueId();

        Vector rightDir = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        BlockData darkDebris = Material.COBBLED_DEEPSLATE.createBlockData();

        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 15;
            Location current = start.clone();

            // Lấy độ cao chuẩn từ chân người chơi
            int prevYCenter = start.getBlockY();
            int prevYRight = start.getBlockY();
            int prevYLeft = start.getBlockY();

            @Override
            public void run() {
                if (step >= maxSteps || !caster.isOnline()) {
                    if (!riftPath.isEmpty()) {
                        startRiftZoneTask(caster, riftPath);
                    }
                    this.cancel();
                    return;
                }

                current.add(direction);

                Location locCenter = current.clone();
                Location locRight = current.clone().add(rightDir);
                Location locLeft = current.clone().subtract(rightDir);

                boolean rowHasValidBlock = false;

                // --- KIỂM TRA ĐỊA HÌNH 2.0 (QUÉT SÀN THỰC TẾ) ---
                Integer floorYCenter = getFloorY(locCenter, prevYCenter);
                if (floorYCenter != null) {
                    locCenter.setY(floorYCenter + 1.0);
                    riftPath.add(locCenter);
                    spawnAbyssalParticle(locCenter, darkDebris);
                    prevYCenter = floorYCenter;
                    rowHasValidBlock = true;
                }

                Integer floorYRight = getFloorY(locRight, prevYRight);
                if (floorYRight != null) {
                    locRight.setY(floorYRight + 1.0);
                    riftPath.add(locRight);
                    spawnAbyssalParticle(locRight, darkDebris);
                    prevYRight = floorYRight;
                    rowHasValidBlock = true;
                }

                Integer floorYLeft = getFloorY(locLeft, prevYLeft);
                if (floorYLeft != null) {
                    locLeft.setY(floorYLeft + 1.0);
                    riftPath.add(locLeft);
                    spawnAbyssalParticle(locLeft, darkDebris);
                    prevYLeft = floorYLeft;
                    rowHasValidBlock = true;
                }

                if (!rowHasValidBlock) {
                    if (!riftPath.isEmpty()) startRiftZoneTask(caster, riftPath);
                    this.cancel();
                    return;
                }

                current.getWorld().playSound(current, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 0.5f);

                // Gây sát thương
                for (Entity entity : current.getWorld().getNearbyEntities(current, 2.5, 2.0, 2.5)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        LivingEntity target = (LivingEntity) entity;
                        UUID tid = target.getUniqueId();

                        // [ANTI-DUP]: Chỉ cho qua nếu chưa dính HashSet VÀ chưa có Metadata khóa
                        if (!hitTargets.contains(tid) && !target.hasMetadata(hitLockKey)) {
                            hitTargets.add(tid);

                            // Khóa cứng mục tiêu trong 0.5s (10 ticks) để chống Dup sát thương tuyệt đối
                            target.setMetadata(hitLockKey, new FixedMetadataValue(RPGCore.getInstance(), true));
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (target.isValid()) target.removeMetadata(hitLockKey, RPGCore.getInstance());
                                }
                            }.runTaskLater(RPGCore.getInstance(), 10L);

                            // Gây sát thương qua RPGCore
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, dmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, target, hitDmg);

                            target.setVelocity(new Vector(0, 0.4, 0));
                        }
                    }
                }

                step++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // TÌM SÀN THÔNG MINH (TERRAIN HUGGING 2.0)
    // ==========================================
    private Integer getFloorY(Location currentLoc, int prevY) {
        // Quét từ trên xuống: Bắt đầu từ vị trí cao hơn bước trước 2 block (để leo dốc)
        // Dò xuống sâu tối đa 3 block (để rớt hố)
        for (int y = prevY + 2; y >= prevY - 3; y--) {
            Location checkLoc = currentLoc.clone();
            checkLoc.setY(y);
            if (checkLoc.getBlock().getType().isSolid()) {
                return y; // Trả về tọa độ Y của sàn nhà thực tế đầu tiên chạm vào
            }
        }
        return null; // Quá sâu hoặc dốc đứng, coi như mất đường
    }

    private void spawnAbyssalParticle(Location loc, BlockData darkDebris) {
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc, 15, 0.3, 1.5, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.SOUL, loc, 5, 0.3, 1.5, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 15, 0.4, 0.2, 0.4, 0.1, darkDebris);
    }

    // ==========================================
    // LINGERING ZONE: QUẢN LÝ RÃNH NỨT & DEBUFF
    // ==========================================
    private void startRiftZoneTask(Player caster, List<Location> path) {
        BlockData riftDebris = Material.OBSIDIAN.createBlockData();

        new BukkitRunnable() {
            int lifeTicks = 0;
            final int maxLife = 120; // 6 giây

            @Override
            public void run() {
                if (lifeTicks >= maxLife || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                // Vẽ Rãnh nứt
                for (Location loc : path) {
                    if (Math.random() < 0.15) {
                        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 3, 0.3, 0.1, 0.3, 0.0);
                        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 1, 0.4, 0.1, 0.4, 0.0, riftDebris);
                    }
                }

                // Debuff (0.5s/lần)
                if (lifeTicks % 10 == 0) {
                    Set<UUID> debuffedTargets = new HashSet<>();

                    for (Location loc : path) {
                        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.2, 1.5, 1.2)) {
                            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                                LivingEntity target = (LivingEntity) entity;
                                UUID tid = target.getUniqueId();

                                if (!debuffedTargets.contains(tid)) {
                                    debuffedTargets.add(tid);

                                    // Áp dụng Khống chế từ RPGCore
                                    vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(target, 20, 0.20, 0);

                                    // Đóng dấu Metadata cho MageClass
                                    target.setMetadata("Abyss_Weakness_Debuff", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 1000));

                                    target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0.02);
                                }
                            }
                        }
                    }
                }
                lifeTicks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}