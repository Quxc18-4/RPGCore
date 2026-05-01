package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
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
        double mineDmg = totalDmg * 0.30;
        double tickDmg = totalDmg * 0.05;

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

        // [VFX]: Vụ nổ gieo hạt ban đầu cực mạnh
        caster.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.5f);
        caster.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        caster.getWorld().spawnParticle(Particle.ITEM_SLIME, center, 80, 2.0, 0.5, 2.0, 0.1);

        // Vòng sóng lan tỏa màu xanh lục
        Particle.DustOptions waveDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 2.0f);
        for(double t = 0; t < Math.PI * 2; t+= 0.2) {
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(4*Math.cos(t), 0.2, 4*Math.sin(t)), 1, 0,0,0, 0, waveDust, true);
        }

        int initialMines = 5 + random.nextInt(4);
        scatterMines(caster, rpgPlayer, center, initialMines, totalDmgMulti);
    }

    private void scatterMines(Player caster, RPGPlayer rpgPlayer, Location center, int count, double totalDmgMulti) {
        double mineDmgMulti = totalDmgMulti * 0.30;
        Particle.DustOptions poisonDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 0), 1.2f);

        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 8.0;
            double offsetZ = (random.nextDouble() - 0.5) * 8.0;
            Location mineLoc = center.clone().add(offsetX, 0, offsetZ);
            mineLoc.setY(mineLoc.getWorld().getHighestBlockYAt(mineLoc) + 0.2);

            new BukkitRunnable() {
                int lifetime = 0;
                double angle = 0;

                @Override
                public void run() {
                    if (lifetime >= 200 || !caster.isOnline()) {
                        this.cancel();
                        return;
                    }

                    // [VFX]: Quả mìn giờ là một vũng độc xoay vòng cực kỳ rõ nét
                    if (lifetime % 2 == 0) {
                        angle += 0.5;
                        double x = 0.6 * Math.cos(angle);
                        double z = 0.6 * Math.sin(angle);
                        mineLoc.getWorld().spawnParticle(Particle.DUST, mineLoc.clone().add(x, 0.1, z), 1, 0,0,0, 0, poisonDust, true);

                        // Sủi bọt ngẫu nhiên
                        if (random.nextDouble() < 0.3) {
                            mineLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, mineLoc.clone().add(0, 0.3, 0), 1, 0.2, 0.2, 0.2, 0);
                        }
                    }

                    boolean triggered = false;
                    for (Entity entity : mineLoc.getWorld().getNearbyEntities(mineLoc, 1.2, 1.5, 1.2)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            triggerMineExplosion(caster, rpgPlayer, mineLoc, target, totalDmgMulti, mineDmgMulti);
                            triggered = true;
                            break;
                        }
                    }

                    if (triggered) this.cancel();
                    lifetime++;
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
        }
    }

    private void triggerMineExplosion(Player caster, RPGPlayer rpgPlayer, Location loc, LivingEntity triggerEntity, double totalDmgMulti, double mineDmgMulti) {
        // [VFX]: Vụ nổ mìn đanh thép và hoành tráng
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, 1.5, 1.5, 1.5, 0.1);

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, mineDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);

                if (target.equals(triggerEntity)) {
                    applyParasite(caster, rpgPlayer, target, totalDmgMulti);
                }
            }
        }
    }

    private void applyParasite(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double totalDmgMulti) {
        double tickDmgMulti = totalDmgMulti * 0.05;

        if (target.hasMetadata("Parasite_DoT")) return;
        target.setMetadata("Parasite_DoT", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 3 || !caster.isOnline()) {
                    target.removeMetadata("Parasite_DoT", RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                if (target.isDead()) {
                    // [VFX DÂY CHUYỀN]: Hoa nở rực rỡ khi quái chết
                    Location deathLoc = target.getLocation();
                    deathLoc.getWorld().playSound(deathLoc, Sound.ITEM_TOTEM_USE, 1.0f, 1.5f);
                    deathLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deathLoc.clone().add(0, 1, 0), 100, 1.5, 1.5, 1.5, 0.2);
                    deathLoc.getWorld().spawnParticle(Particle.EXPLOSION, deathLoc, 2);

                    double explosionDmgMulti = totalDmgMulti * 0.30;
                    for (Entity e : deathLoc.getWorld().getNearbyEntities(deathLoc, 3.0, 3.0, 3.0)) {
                        if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, (LivingEntity) e, explosionDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, (LivingEntity) e, hitDmg);
                        }
                    }

                    scatterMines(caster, rpgPlayer, deathLoc, 2, totalDmgMulti);

                    target.removeMetadata("Parasite_DoT", RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                // [VFX Rút Máu]: Sao xanh nổ liên tục trên người quái
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SPIDER_STEP, 1.0f, 0.5f);
                target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getEyeLocation(), 15, 0.4, 0.4, 0.4, 0);
                target.getWorld().spawnParticle(Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.1);

                double tickHitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, tickDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, tickHitDmg);

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L);
    }
}