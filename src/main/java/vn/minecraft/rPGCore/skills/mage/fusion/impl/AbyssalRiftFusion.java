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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    @Override public double getDamageMultiplier() { return 1.30; } // Thuế Debuff khắt khe

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
                "§f§8[Nội tại - Lời Nguyền Vực Thẳm]",
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
        Set<UUID> hitTargets = new HashSet<>(); // Ngăn quái nhận sát thương cột nhiều lần

        // Dữ liệu hạt nứt đất đen ngòm (Obsidian/Deepslate)
        BlockData darkDebris = Material.COBBLED_DEEPSLATE.createBlockData();

        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 15; // Rãnh dài 15 block
            Location current = start.clone();

            @Override
            public void run() {
                if (step >= maxSteps || !caster.isOnline()) {
                    // Sau khi cột sóng chạy xong, khởi tạo Task duy trì Rãnh nứt
                    if (!riftPath.isEmpty()) {
                        startRiftZoneTask(caster, riftPath);
                    }
                    this.cancel();
                    return;
                }

                current.add(direction);
                // [TERRAIN HUGGING]: Ép tọa độ Y luôn bám sát bề mặt khối rắn cao nhất
                int highestY = current.getWorld().getHighestBlockYAt(current);
                current.setY(highestY + 1.0); // Đặt trên mặt đất 1 block

                // Nếu đâm vào tường thẳng đứng quá cao (chênh > 2 block) -> Dừng sóng
                if (Math.abs(highestY - start.getY()) > 2) {
                    if (!riftPath.isEmpty()) startRiftZoneTask(caster, riftPath);
                    this.cancel();
                    return;
                }

                riftPath.add(current.clone());

                // 1. VFX: Cột năng lượng trồi lên
                current.getWorld().playSound(current, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 0.5f);
                current.getWorld().spawnParticle(Particle.SQUID_INK, current, 30, 0.5, 2.0, 0.5, 0.05);
                current.getWorld().spawnParticle(Particle.SOUL, current, 15, 0.5, 2.0, 0.5, 0.1);
                current.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, current, 40, 0.8, 0.2, 0.8, 0.1, darkDebris);

                // 2. Sát thương tức thời (Hitbox Radius 2.0m xung quanh cột)
                for (Entity entity : current.getWorld().getNearbyEntities(current, 2.0, 2.0, 2.0)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!hitTargets.contains(target.getUniqueId())) {
                            hitTargets.add(target.getUniqueId());

                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, dmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, target, hitDmg);

                            // Hất nhẹ lên tạo cảm giác bị nứt đất chọc trúng
                            target.setVelocity(new Vector(0, 0.4, 0));
                        }
                    }
                }

                step++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L); // Sóng càn lướt siêu tốc (1 block/tick)
    }

    // ==========================================
    // LINGERING ZONE: QUẢN LÝ RÃNH NỨT & DEBUFF
    // ==========================================
    private void startRiftZoneTask(Player caster, List<Location> path) {
        BlockData riftDebris = Material.OBSIDIAN.createBlockData();

        new BukkitRunnable() {
            int lifeTicks = 0;
            final int maxLife = 120; // Tồn tại 6 giây (120 ticks)

            @Override
            public void run() {
                if (lifeTicks >= maxLife || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                // 1. VFX Rãnh nứt âm ỉ (Chạy mỗi tick để giữ khói mượt)
                for (Location loc : path) {
                    if (Math.random() < 0.3) { // Tối ưu: Chỉ spawn hạt ở 30% số block để tránh giật lag
                        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 5, 0.3, 0.1, 0.3, 0.0);
                        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 2, 0.5, 0.1, 0.5, 0.0, riftDebris);
                    }
                }

                // 2. Logic Debuff (Quét mỗi 10 ticks - 0.5s để tối ưu CPU)
                if (lifeTicks % 10 == 0) {
                    Set<UUID> debuffedTargets = new HashSet<>();

                    // Quét các điểm trên đường nứt
                    for (Location loc : path) {
                        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                                LivingEntity target = (LivingEntity) entity;
                                UUID tid = target.getUniqueId();

                                if (!debuffedTargets.contains(tid)) {
                                    debuffedTargets.add(tid);

                                    // [CẬP NHẬT CHUẨN RPGCORE]: Dùng StatusEffectManager để cấp Slow (20 ticks = 1s, 20% slow, Amplifier 0 cho mob)
                                    vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(target, 20, 0.20, 0);

                                    // Cấp Metadata để CombatManager bên MageClass đọc và giảm 10% Sát thương
                                    target.setMetadata("Abyss_Weakness_Debuff", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 1000));

                                    // VFX bám trên người bị debuff
                                    target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.02);
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