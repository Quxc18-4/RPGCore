package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.List;

public class DoomExecutionFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.DARK; }
    @Override public String getReactionName() { return "§5§lHắc Ấn Tử Thần"; }

    @Override public double getDamageMultiplier() { return 1.50; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double baseExplosion = baseDmgPct * getDamageMultiplier();
        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Hắc Ấn Tử Thần]",
                "§f- Phóng Hắc Tiễn tự động truy đuổi mục tiêu có HP cao nhất.",
                "§f- Gắn Hắc Ấn lên mục tiêu, tồn tại trong §a5 giây§f.",
                "§f- Mọi sát thương nhận vào sẽ nạp năng lượng cho Ấn.",
                "",
                "§b[Nội tại - Trợ Giúp Vực Sâu]§f",
                "§f- Khi Solo: Đòn đánh của bạn nạp §b10% / Hit§f.",
                "§f- Khi có Đồng minh: Bản thân nạp §b8% / Hit§f,",
                "§f  và Đồng minh hỗ trợ nạp §b2% / Hit§f.",
                "",
                "§c[Nội tại - Phán Quyết Vực Thẳm]§f",
                "§f- Sau 5s, Ấn nổ tung & áp hiệu ứng §5HẮC HỎA§f.",
                "§f- Mốc 1 (0-39%): Phạm vi: 3m, gây §c" + (int)baseExplosion + "% §fSát thương.",
                "§f- Mốc 2 (40-79%): Phạm vi: 3m, gây §c" + (int)(baseExplosion * 1.3) + "% §fSát thương.",
                "§f- Mốc 3 (80-100%): Phạm vi: 5m, gây §c" + (int)(baseExplosion * 1.5) + "% §fSát thương."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        // 1. TÌM MỤC TIÊU CÓ HP CAO NHẤT TRONG BÁN KÍNH 25M
        LivingEntity bestTarget = null;
        double maxHp = -1.0;

        for (Entity e : caster.getNearbyEntities(25, 25, 25)) {
            if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                LivingEntity le = (LivingEntity) e;
                if (le.getHealth() > maxHp) {
                    maxHp = le.getHealth();
                    bestTarget = le;
                }
            }
        }

        if (bestTarget == null) {
            caster.sendMessage("§c§l[!] §fKhông có kẻ địch nào ở gần để ám!");
            return;
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);

        // 2. BẮN VẬT THỂ BAY (HẮC TIỄN) TRUY ĐUỔI MỤC TIÊU
        LivingEntity finalTarget = bestTarget;
        new BukkitRunnable() {
            Location current = caster.getEyeLocation().clone();
            int ticks = 0;

            @Override
            public void run() {
                if (finalTarget.isDead() || !finalTarget.isValid() || ticks > 60) {
                    this.cancel();
                    return;
                }

                Location targetEye = finalTarget.getEyeLocation();
                Vector dir = targetEye.toVector().subtract(current.toVector());
                double dist = dir.length();

                // Nếu đạn bay trúng mục tiêu (Khoảng cách < 1.5 block)
                if (dist < 1.5) {
                    this.cancel();
                    current.getWorld().playSound(current, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 2.0f);
                    current.getWorld().spawnParticle(Particle.SQUID_INK, current, 30, 0.5, 0.5, 0.5, 0.1);

                    // Kích hoạt cơ chế đếm số Hit trong 5 giây
                    executeDoomMark(caster, rpgPlayer, finalTarget, baseMultiplier);
                    return;
                }

                // Di chuyển đạn về phía mục tiêu (Tốc độ 1.0 block/tick)
                current.add(dir.normalize().multiply(1.0));

                // Hiệu ứng đạn bay: Đầu đạn Soul Fire, đuôi đạn Mực đen
                current.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, current, 3, 0.1, 0.1, 0.1, 0.0);
                current.getWorld().spawnParticle(Particle.SQUID_INK, current, 2, 0.1, 0.1, 0.1, 0.0);

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void executeDoomMark(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double baseMultiplier) {
        // Sinh TextDisplay lơ lửng
        TextDisplay display = target.getWorld().spawn(target.getLocation().add(0, target.getHeight() + 0.5, 0), TextDisplay.class, d -> {
            d.setText("§7[☠ 0%]");
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
        });

        final double[] currentCharge = {0.0};

        // ==========================================
        // LISTENER NẶC DANH: CHECK ĐỒNG MINH THEO THỜI GIAN THỰC
        // ==========================================
        Listener dynamicHitListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onTargetHit(EntityDamageEvent event) {
                if (event.getEntity().equals(target)) {
                    if (currentCharge[0] >= 100.0) return;

                    double addRate = 2.0; // Mặc định do sát thương lửa, độc, hoặc từ đồng đội = 2%

                    // Phân tích nếu là một Entity đánh
                    if (event instanceof EntityDamageByEntityEvent) {
                        Entity damager = ((EntityDamageByEntityEvent) event).getDamager();

                        // Nếu là đạn (Cung, Tuyết, Phép...) -> Tìm người bắn
                        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Entity) {
                            damager = (Entity) ((Projectile) damager).getShooter();
                        }

                        if (damager.equals(caster)) {
                            // Người đánh là Caster -> Check xem có đồng minh ở quanh Caster không (thời gian thực!)
                            boolean hasAlly = false;
                            for (Entity e : caster.getNearbyEntities(15, 15, 15)) {
                                if (e instanceof Player && !e.equals(caster) && !CombatManager.canTarget(caster, e)) {
                                    hasAlly = true;
                                    break;
                                }
                            }
                            addRate = hasAlly ? 8.0 : 10.0; // 8% nếu trong Group, 10% nếu đang Solo
                        }
                        else if (damager instanceof Player && !CombatManager.canTarget(caster, damager)) {
                            // Người đánh là Đồng minh -> 2%
                            addRate = 2.0;
                        }
                    }

                    // Cộng tiến trình
                    currentCharge[0] = Math.min(100.0, currentCharge[0] + addRate);

                    String color = "§7";
                    if (currentCharge[0] >= 80.0) color = "§c§l";
                    else if (currentCharge[0] >= 40.0) color = "§e§l";

                    display.setText(color + "[☠ " + (int)currentCharge[0] + "%]");

                    float pitch = 0.5f + (float)(currentCharge[0] / 100.0);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, pitch);
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(dynamicHitListener, RPGCore.getInstance());

        // ==========================================
        // ĐỒNG HỒ 5 GIÂY (100 Ticks)
        // ==========================================
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100 || target.isDead() || !target.isValid() || !caster.isOnline()) {
                    HandlerList.unregisterAll(dynamicHitListener);
                    display.remove();

                    // Boss vẫn còn sống (Hoặc vừa chết do nổ) -> Kích hoạt Execution
                    if (target.isValid()) {
                        triggerExecution(caster, rpgPlayer, target, currentCharge[0], baseMultiplier);
                    }
                    this.cancel();
                    return;
                }

                display.teleport(target.getLocation().add(0, target.getHeight() + 0.8, 0));
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void triggerExecution(Player caster, RPGPlayer rpgPlayer, LivingEntity target, double finalCharge, double baseMulti) {
        // MỐC 1 (Mặc định)
        double radius = 1.5; // Bán kính 1.5 ~ Vùng 3x3
        double dmgMulti = getDamageMultiplier(); // x1

        // MỐC 3
        if (finalCharge >= 80.0) {
            radius = 2.5; // Bán kính 2.5 ~ Vùng 5x5
            dmgMulti *= 1.5; // Tăng 50% sát thương (30+20)
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        }
        // MỐC 2
        else if (finalCharge >= 40.0) {
            radius = 1.5; // Bán kính 1.5 ~ Vùng 3x3
            dmgMulti *= 1.3; // Tăng 30% sát thương
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        }
        else {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        }

        // Visual vụ nổ
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation(), (int)(radius * 30), radius, 1.0, radius, 0.1);
        target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation(), (int)(radius * 20), radius, 1.0, radius, 0.1);

        double finalMulti = baseMulti * dmgMulti;

        // Gây sát thương và Áp hiệu ứng
        for (Entity e : target.getWorld().getNearbyEntities(target.getLocation(), radius, radius, radius)) {
            if (e instanceof LivingEntity && !e.equals(caster) && CombatManager.canTarget(caster, e)) {
                LivingEntity victim = (LivingEntity) e;

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, victim, finalMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, victim, hitDmg);

                // Áp HẮC HỎA
                ReactionEngine.applyMarkOnly(victim, MageElement.DARK);
                ReactionEngine.processHit(caster, victim, MageElement.FIRE);
            }
        }
    }
}