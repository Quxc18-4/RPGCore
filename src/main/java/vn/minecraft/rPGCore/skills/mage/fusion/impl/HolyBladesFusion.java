package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.*;

public class HolyBladesFusion implements IFusion {

    // Quản lý đội hình kiếm tĩnh (Static) để MageClass có thể gọi tới
    private static final Map<UUID, BladeFormation> activeFormations = new HashMap<>();

    @Override public MageElement getElement1() { return MageElement.LIGHT; }
    @Override public MageElement getElement2() { return MageElement.LIGHT; }
    @Override public String getReactionName() { return "§e§lThánh Kiếm Phán Quyết"; }
    @Override public double getDamageMultiplier() { return 1.20; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double pierceDmg = totalDmg * 0.40;
        double sweepDmg = totalDmg * 0.60;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Thánh Kiếm Phán Quyết]",
                "§f- Triệu hồi 2 Thánh Kiếm bảo hộ sau lưng (Tồn tại 15s).",
                "§f- Tái kích hoạt để gọi thêm 1 kiếm (Tối đa 6).",
                "§f- §c[Nội tại Khai Hỏa]§f: Mỗi đòn đánh thường sẽ ra lệnh cho",
                "§f  1 Thánh Kiếm lao thẳng tới mục tiêu.",
                "§f- §eĐâm thẳng§f: Gây §c" + (int)pierceDmg + "% §fST Kỹ năng (Đơn).",
                "§f- §eChém quét§f: Sau khi đâm, kiếm quét ngang gây §c" + (int)sweepDmg + "% §fST AoE."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        UUID pid = caster.getUniqueId();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.5f);

        if (activeFormations.containsKey(pid)) {
            BladeFormation formation = activeFormations.get(pid);
            formation.addBlade();
            formation.resetDuration();
        } else {
            BladeFormation formation = new BladeFormation(caster, rpgPlayer, totalDmgMulti);
            formation.addBlade();
            formation.addBlade();
            activeFormations.put(pid, formation);
        }
    }

    // API Tĩnh để MageClass gọi vào khi người chơi Left-Click
    public static void tryFireBlade(Player caster, RPGPlayer rpgPlayer) {
        if (activeFormations.containsKey(caster.getUniqueId())) {
            activeFormations.get(caster.getUniqueId()).fireBlade();
        }
    }

    // ==========================================
    // HỆ THỐNG QUẢN LÝ ĐỘI HÌNH & VẬT LÝ ĐẠN
    // ==========================================
    private static class BladeFormation {
        private final Player owner;
        private final RPGPlayer rpgPlayer;
        private final double totalDmgMulti;
        private final List<ItemDisplay> blades = new ArrayList<>();
        private int ticksRemaining = 300; // 15s * 20 ticks
        private BukkitRunnable orbitTask;

        public BladeFormation(Player owner, RPGPlayer rpgPlayer, double totalDmgMulti) {
            this.owner = owner;
            this.rpgPlayer = rpgPlayer;
            this.totalDmgMulti = totalDmgMulti;
            startOrbiting();
        }

        public void addBlade() {
            if (blades.size() >= 6) return; // Cap 6 kiếm

            Location spawnLoc = owner.getLocation().add(0, 2, 0);
            ItemDisplay blade = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.GOLDEN_SPEAR)); // Giáo ma thuật
                display.setTeleportDuration(2); // Nội suy di chuyển mượt bám theo lưng
            });
            blades.add(blade);
            owner.getWorld().playSound(owner.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 2.0f);
        }

        public void resetDuration() {
            this.ticksRemaining = 300;
        }

        private void startOrbiting() {
            orbitTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // [TỐI ƯU MEMORY LEAK]: Tự động dọn rác nếu chủ nhân Offline hoặc chết
                    if (ticksRemaining <= 0 || !owner.isOnline() || owner.isDead() || blades.isEmpty()) {
                        cleanup();
                        activeFormations.remove(owner.getUniqueId());
                        this.cancel();
                        return;
                    }

                    Location center = owner.getLocation();
                    Vector direction = center.getDirection().setY(0).normalize();
                    Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

                    // Thuật toán dải hình cánh cung sau lưng
                    double spacing = 0.8;
                    int count = blades.size();
                    double startOffset = -(count - 1) * spacing / 2.0;

                    for (int i = 0; i < count; i++) {
                        ItemDisplay blade = blades.get(i);
                        // Lùi ra sau lưng 1.2 block, nâng lên ngang vai 1.5 block
                        Location targetLoc = center.clone().subtract(direction.clone().multiply(1.2)).add(0, 1.5, 0);
                        targetLoc.add(right.clone().multiply(startOffset + (i * spacing)));

                        blade.teleport(targetLoc);

                        // Xoay kiếm chỉa chéo lên trời uy dũng
                        Transformation transform = blade.getTransformation();
                        transform.getRightRotation().set(new AxisAngle4f((float) Math.toRadians(45), 1, 0, 0));
                        blade.setTransformation(transform);
                    }

                    ticksRemaining--;
                }
            };
            orbitTask.runTaskTimer(RPGCore.getInstance(), 0L, 2L);
        }

        public void fireBlade() {
            if (blades.isEmpty()) return;

            ItemDisplay firedBlade = blades.remove(blades.size() - 1);
            // Gỡ bỏ nội suy để đạn bay sát thực tế (không bị delay hình ảnh)
            firedBlade.setTeleportDuration(0);

            Location startLoc = firedBlade.getLocation();
            Vector fireDirection = owner.getEyeLocation().getDirection().normalize();

            owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.5f);

            new BukkitRunnable() {
                int flightTicks = 0;
                final int maxFlight = 30; // Tầm bay 30 block

                @Override
                public void run() {
                    if (flightTicks >= maxFlight || firedBlade.isDead()) {
                        executeSweep(firedBlade.getLocation());
                        firedBlade.remove();
                        this.cancel();
                        return;
                    }

                    Location currentLoc = firedBlade.getLocation();
                    currentLoc.add(fireDirection.clone().multiply(1.2)); // Tốc độ bay cực nhanh
                    firedBlade.teleport(currentLoc);

                    // Xoay kiếm chúi thẳng xuống hướng mục tiêu
                    Transformation transform = firedBlade.getTransformation();
                    transform.getRightRotation().set(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0));
                    firedBlade.setTransformation(transform);

                    currentLoc.getWorld().spawnParticle(Particle.GLOW, currentLoc, 1, 0, 0, 0, 0);

                    // Đập tường -> Nổ
                    if (currentLoc.getBlock().getType().isSolid()) {
                        executeSweep(currentLoc);
                        firedBlade.remove();
                        this.cancel();
                        return;
                    }

                    // Va chạm quái vật -> Đâm thẳng 40%
                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                        if (entity instanceof LivingEntity && !entity.equals(owner) && CombatManager.canTarget(owner, entity)) {
                            LivingEntity target = (LivingEntity) entity;

                            double pierceDmgMulti = totalDmgMulti * 0.40;
                            double hitDmg = CombatManager.calculateDamage(owner, rpgPlayer, target, pierceDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(owner, target, hitDmg);

                            executeSweep(currentLoc);
                            firedBlade.remove();
                            this.cancel();
                            return;
                        }
                    }
                    flightTicks++;
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

            // Tự dọn rác đội hình nếu hết kiếm
            if (blades.isEmpty() && orbitTask != null) {
                orbitTask.cancel();
                activeFormations.remove(owner.getUniqueId());
            }
        }

        private void executeSweep(Location loc) {
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.2f);

            // VFX Vòng xoay chém quét
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 1.5, 0.2, 1.5, 0.1);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 15, 1.0, 1.0, 1.0, 0.1);

            double sweepDmgMulti = totalDmgMulti * 0.60;

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.0, 2.0, 3.0)) {
                if (entity instanceof LivingEntity && !entity.equals(owner) && CombatManager.canTarget(owner, entity)) {
                    LivingEntity target = (LivingEntity) entity;
                    double hitDmg = CombatManager.calculateDamage(owner, rpgPlayer, target, sweepDmgMulti, 0, true, false);
                    CombatManager.applySkillDamage(owner, target, hitDmg);
                }
            }
        }

        public void cleanup() {
            if (orbitTask != null) orbitTask.cancel();
            for (ItemDisplay blade : blades) {
                if (blade != null && !blade.isDead()) blade.remove();
            }
            blades.clear();
        }
    }
}