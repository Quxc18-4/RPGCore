package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ParasiticMinefieldFusion implements IFusion {

    private final Random random = new Random();

    @Override public MageElement getElement1() { return MageElement.NATURE; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }
    @Override public String getReactionName() { return "§2§lLãnh Địa Ký Sinh"; }
    @Override public double getDamageMultiplier() { return 1.30; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double mineDmg = totalDmg * 0.30; // 30% cho mỗi quả mìn/nổ xác
        double tickDmg = totalDmg * 0.05; // 5% rút máu mỗi giây

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Lãnh Địa Ký Sinh]",
                "§f- Phân tán 5-8 Nụ Hoa Độc tàng hình vào khu vực ngắm tới.",
                "§f- Kẻ địch đạp trúng sẽ khiến Nụ Hoa phát nổ, gây §c§l" + (int)mineDmg + "% §fSát thương,",
                "§f  đồng thời gieo rắc §2Mầm Bệnh Ký Sinh§f.",
                "§f- Mầm bệnh rút §c§l" + (int)tickDmg + "% §fSát thương mỗi giây (Tồn tại 3s).",
                "§f- §c[Hiệu ứng Dây Chuyền]§f: Nếu kẻ địch chết khi mang Mầm Bệnh,",
                "§f  xác chúng sẽ phát nổ và văng ra thêm 2 Nụ Hoa Độc mới!"
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Block targetBlock = caster.getTargetBlockExact(30, org.bukkit.FluidCollisionMode.NEVER);
        Location targetLoc;
        if (targetBlock != null) {
            targetLoc = targetBlock.getLocation();
        } else {
            targetLoc = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(30));
        }

        targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc));
        Location center = targetLoc.add(0.5, 1.0, 0.5);

        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(center, Sound.ENTITY_SPLASH_POTION_BREAK, 1.5f, 0.5f);
        caster.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 100, 3.0, 1.0, 3.0, 0.05);

        // Sinh 5-8 quả mìn ban đầu
        int initialMines = 5 + random.nextInt(4);
        scatterMines(caster, rpgPlayer, center, initialMines, totalDmgMulti);
    }

    // ==========================================
    // HÀM RẢI MÌN (Tái sử dụng cho cả lúc cast và lúc nổ dây chuyền)
    // ==========================================
    private void scatterMines(Player caster, RPGPlayer rpgPlayer, Location center, int count, double totalDmgMulti) {
        double mineDmgMulti = totalDmgMulti * 0.30;
        Particle.DustOptions poisonDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 150, 0), 1.0f);

        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 10.0;
            double offsetZ = (random.nextDouble() - 0.5) * 10.0;
            Location mineLoc = center.clone().add(offsetX, 0, offsetZ);
            mineLoc.setY(mineLoc.getWorld().getHighestBlockYAt(mineLoc) + 0.2);

            // Vòng đời của 1 quả Mìn (10 giây = 200 ticks)
            new BukkitRunnable() {
                int lifetime = 0;

                @Override
                public void run() {
                    if (lifetime >= 200 || !caster.isOnline()) {
                        this.cancel();
                        return;
                    }

                    // Hạt Particle lờ mờ để đánh dấu bãi mìn (Mỗi 5 ticks mới nháy 1 lần để đỡ giật)
                    if (lifetime % 5 == 0) {
                        mineLoc.getWorld().spawnParticle(Particle.DUST, mineLoc, 1, 0.1, 0.1, 0.1, 0, poisonDust, true);
                    }

                    // Cảm biến đạp mìn (Quét bán kính 1.2m)
                    boolean triggered = false;
                    for (Entity entity : mineLoc.getWorld().getNearbyEntities(mineLoc, 1.2, 1.5, 1.2)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;

                            triggerMineExplosion(caster, rpgPlayer, mineLoc, target, totalDmgMulti, mineDmgMulti);
                            triggered = true;
                            break; // 1 thằng đạp là mìn nổ, không cần quét tiếp
                        }
                    }

                    if (triggered) {
                        this.cancel();
                    }

                    lifetime++;
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L); // Quét liên tục mỗi 1 tick
        }
    }

    // ==========================================
    // NỔ MÌN VÀ GẮN KÝ SINH
    // ==========================================
    private void triggerMineExplosion(Player caster, RPGPlayer rpgPlayer, Location loc, LivingEntity triggerEntity, double totalDmgMulti, double mineDmgMulti) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 30, 1.5, 1.0, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 1.5, 1.0, 1.5, 0.1);

        // Gây sát thương Nổ AoE (R=2.5) cho tất cả quái đứng gần quả mìn
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                // [Cốt lõi RPGCore]: Tính sát thương nổ
                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, mineDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);

                // Nếu là con đạp mìn -> Gắn Ký Sinh rút máu
                if (target.equals(triggerEntity)) {
                    applyParasite(caster, rpgPlayer, target, totalDmgMulti);
                }
            }
        }
    }

    // ==========================================
    // DEBUFF KÝ SINH (Rút máu & Nổ Dây Chuyền)
    // ==========================================
    private void applyParasite(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double totalDmgMulti) {
        double tickDmgMulti = totalDmgMulti * 0.05;

        // Tránh bị stack nhiều cái Ký sinh cùng lúc gây tuột TPS
        if (target.hasMetadata("Parasite_DoT")) return;
        target.setMetadata("Parasite_DoT", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Hết 3 giây (3 lặp x 20 ticks) hoặc quái chết
                if (ticks >= 3 || !caster.isOnline()) {
                    target.removeMetadata("Parasite_DoT", RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                // [HIỆU ỨNG DÂY CHUYỀN]: Quái chết trong lúc đang bị Ký sinh!
                if (target.isDead()) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);

                    // Gây sát thương Nổ Xác AoE
                    double explosionDmgMulti = totalDmgMulti * 0.30;
                    for (Entity e : target.getWorld().getNearbyEntities(target.getLocation(), 3.0, 3.0, 3.0)) {
                        if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) e, explosionDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) e, hitDmg);
                        }
                    }

                    // Đẻ thêm 2 Nụ Hoa mới tại vị trí xác chết
                    scatterMines(caster, rpgPlayer, target.getLocation(), 2, totalDmgMulti);

                    target.removeMetadata("Parasite_DoT", RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                // Gây sát thương DoT
                target.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, target.getEyeLocation(), 5, 0.3, 0.3, 0.3, 0);
                double tickHitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, tickDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, tickHitDmg);

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L); // Cứ 1 giây (20 ticks) giật 1 lần
    }
}