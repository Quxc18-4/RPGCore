package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.*;

public class HolyBladesFusion implements IFusion {

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
                "§f- Triệu hồi đội hình Hoàng Kim Thương (Tối đa 6 thanh).",
                "§f- §c[Nội tại Khai Hỏa]§f: Mỗi đòn đánh thường sẽ ra lệnh cho",
                "§f  1 thanh giáo phóng thẳng về phía trước.",
                "§f- §eXuyên thấu§f: Đâm xuyên kẻ địch, gây §c" + (int)pierceDmg + "% §fST.",
                "§f- §eChém quét§f: Kẻ địch bị đâm sẽ kích hoạt 1 luồng chém",
                "§f  quét diện rộng tại chỗ, gây §c" + (int)sweepDmg + "% §fST AoE."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        UUID pid = caster.getUniqueId();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.5f, 1.5f);

        if (activeFormations.containsKey(pid)) {
            BladeFormation formation = activeFormations.get(pid);
            formation.addBlades(2);
            formation.resetDuration();
        } else {
            BladeFormation formation = new BladeFormation(caster, rpgPlayer, totalDmgMulti);
            formation.addBlades(2);
            activeFormations.put(pid, formation);
        }
    }

    public static void tryFireBlade(Player caster, RPGPlayer rpgPlayer) {
        if (activeFormations.containsKey(caster.getUniqueId())) {
            activeFormations.get(caster.getUniqueId()).fireBlade();
        }
    }

    // ==========================================
    // CLASS QUẢN LÝ ĐỘI HÌNH & VẬT LÝ ĐẠN
    // ==========================================
    private static class BladeFormation {
        private final Player owner;
        private final RPGPlayer rpgPlayer;
        private final double totalDmgMulti;
        private final List<ItemDisplay> blades = new ArrayList<>();
        private int ticksRemaining = 300;
        private BukkitRunnable orbitTask;

        // [MẶT PHẲNG 2D]: Trục X, Trục Y, Góc Xoay Z (Đã đo chuẩn đối xứng)
        private static final double[][] FAN_POSITIONS = {
                {-1.0, -0.2,  20 }, // 0: Dưới phải
                {1.0, -0.2, 160 }, // 1: Dưới trái
                {-1.1,  0.5,  40 }, // 2: Giữa phải
                {1.1,  0.5, 140 }, // 3: Giữa trái
                {-0.8,  1.1,  70 }, // 4: Trên phải
                {0.8,  1.1, 110 }  // 5: Trên trái
        };

        public BladeFormation(Player owner, RPGPlayer rpgPlayer, double totalDmgMulti) {
            this.owner = owner;
            this.rpgPlayer = rpgPlayer;
            this.totalDmgMulti = totalDmgMulti;
            startOrbiting();
        }

        public void addBlades(int amount) {
            int added = 0;
            for (int i = 0; i < amount; i++) {
                if (blades.size() >= 6) break;

                Location spawnLoc = owner.getLocation().add(0, 2, 0);
                ItemDisplay blade = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
                    // CẬP NHẬT: Đúng GOLDEN_SPEAR
                    ItemStack spear = new ItemStack(Material.valueOf("GOLDEN_SPEAR"));
                    ItemMeta meta = spear.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                        spear.setItemMeta(meta);
                    }
                    display.setItemStack(spear);
                    display.setTeleportDuration(2);
                    display.setInterpolationDuration(2);
                });
                blades.add(blade);
                added++;
            }
            if (added > 0) {
                owner.getWorld().playSound(owner.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 2.0f);
            }
        }

        public void resetDuration() {
            this.ticksRemaining = 300;
        }

        private void startOrbiting() {
            orbitTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (ticksRemaining <= 0 || !owner.isOnline() || owner.isDead() || blades.isEmpty()) {
                        cleanup();
                        activeFormations.remove(owner.getUniqueId());
                        this.cancel();
                        return;
                    }

                    Location center = owner.getLocation();

                    // Tạo mặt phẳng 2D tuyệt đối (Pitch = 0)
                    Vector forward = center.getDirection().setY(0).normalize();
                    Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                    Vector back = forward.clone().multiply(-1);

                    // Đặt tâm mặt phẳng cách lưng 1 block, lùi nhẹ xuống để vừa tầm nhìn
                    Location planeCenter = center.clone().add(back.clone().multiply(1.0)).add(0, 1.0, 0);

                    for (int i = 0; i < blades.size(); i++) {
                        ItemDisplay blade = blades.get(i);
                        double offsetX = FAN_POSITIONS[i][0];
                        double offsetY = FAN_POSITIONS[i][1];

                        // [NẮN SPRITE]: Lấy góc chuẩn trừ đi 45 độ của Minecraft (PI/4)
                        float rotZ = (float) Math.toRadians(FAN_POSITIONS[i][2]) - (float) (Math.PI / 4);

                        Location targetLoc = planeCenter.clone()
                                .add(right.clone().multiply(offsetX))
                                .add(0, offsetY, 0);
                        targetLoc.setDirection(forward); // Tấm bảng nhìn thẳng

                        blade.teleport(targetLoc);

                        // Chỉ xoay trục Z trên mặt phẳng 2D
                        Quaternionf rot = new Quaternionf().rotateZ(rotZ);
                        blade.setTransformation(new Transformation(
                                new Vector3f(), rot, new Vector3f(1.4f, 1.4f, 1.4f), new Quaternionf()
                        ));
                    }
                    ticksRemaining--;
                }
            };
            orbitTask.runTaskTimer(RPGCore.getInstance(), 0L, 2L);
        }

        public void fireBlade() {
            if (blades.isEmpty()) return;

            ItemDisplay firedBlade = blades.remove(blades.size() - 1);
            owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.5f);

            // [NẮN ĐẠN CHUẨN XÁC]: Xoay Z -45 độ để nằm ngang, xoay Y -90 độ để chĩa mũi về Z+ (phía trước)
            final Quaternionf flyingRot = new Quaternionf()
                    .rotateY((float) -Math.PI / 2)
                    .rotateZ((float) -Math.PI / 4);
            final Transformation flyTransform = new Transformation(
                    new Vector3f(), flyingRot, new Vector3f(1.4f, 1.4f, 1.4f), new Quaternionf()
            );

            new BukkitRunnable() {
                int stateTicks = 0;
                int flightTicks = 0;
                final int maxFlight = 30;
                Vector fireDirection;

                final Set<UUID> piercedTargets = new HashSet<>();
                final Set<UUID> sweptTargets = new HashSet<>();

                @Override
                public void run() {
                    if (firedBlade.isDead() || !owner.isOnline()) {
                        firedBlade.remove();
                        this.cancel();
                        return;
                    }

                    // GIAI ĐOẠN 1: Kéo ra trước mặt và xoay (Tick 0)
                    if (stateTicks == 0) {
                        firedBlade.setTeleportDuration(5);
                        firedBlade.setInterpolationDuration(5);
                        firedBlade.setInterpolationDelay(0);

                        // Kéo ra trước mắt 1.2 block
                        Location prepLoc = owner.getEyeLocation().add(owner.getLocation().getDirection().normalize().multiply(1.2));
                        prepLoc.setDirection(owner.getLocation().getDirection());

                        firedBlade.teleport(prepLoc);
                        firedBlade.setTransformation(flyTransform); // Bắt đầu xoay mượt sang thế bắn
                    }

                    // GIAI ĐOẠN 2: Chờ Animation chạy xong (Tick 1 đến 5)
                    if (stateTicks < 6) {
                        stateTicks++;
                        return;
                    }

                    // GIAI ĐOẠN 3: Bắn (Tick 6)
                    if (stateTicks == 6) {
                        firedBlade.setTeleportDuration(0); // Tắt nội suy để Hitbox chuẩn tick-by-tick
                        fireDirection = owner.getEyeLocation().getDirection().normalize(); // Khóa mục tiêu
                        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.5f);
                        stateTicks++;
                    }

                    // GIAI ĐOẠN 4: Bay và Va chạm
                    Location currentLoc = firedBlade.getLocation();
                    currentLoc.add(fireDirection.clone().multiply(1.5));
                    currentLoc.setDirection(fireDirection);

                    firedBlade.teleport(currentLoc);
                    currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 1, 0, 0, 0, 0);

                    // Va chạm tường
                    if (currentLoc.getBlock().getType().isSolid()) {
                        currentLoc.getWorld().spawnParticle(Particle.ITEM, currentLoc, 20, 0.2, 0.2, 0.2, 0.05, new ItemStack(Material.valueOf("GOLDEN_SPEAR")));
                        currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                        firedBlade.remove();
                        this.cancel();
                        return;
                    }

                    // Va chạm quái (XUYÊN + QUÉT)
                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                        if (entity instanceof LivingEntity && !entity.equals(owner) && CombatManager.canTarget(owner, entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            UUID targetId = target.getUniqueId();

                            if (!piercedTargets.contains(targetId)) {
                                piercedTargets.add(targetId);

                                // ST Đâm
                                double pierceDmgMulti = totalDmgMulti * 0.40;
                                double hitDmg = CombatManager.calculateDamage(owner, rpgPlayer, target, pierceDmgMulti, 0, true, false);
                                CombatManager.applySkillDamage(owner, target, hitDmg);

                                target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.2f);

                                // Kích nổ chém quét ngay tại chỗ
                                executeDynamicSweep(target.getLocation(), sweptTargets);
                            }
                        }
                    }

                    flightTicks++;
                    if (flightTicks >= maxFlight) {
                        firedBlade.remove();
                        this.cancel();
                    }
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

            if (blades.isEmpty() && orbitTask != null) {
                orbitTask.cancel();
                activeFormations.remove(owner.getUniqueId());
            }
        }

        private void executeDynamicSweep(Location loc, Set<UUID> sweptTargets) {
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.2f);
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 3, 1.5, 0.2, 1.5, 0.1);

            double sweepDmgMulti = totalDmgMulti * 0.60;
            double radiusSq = 3.5 * 3.5;

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
                if (entity instanceof LivingEntity && !entity.equals(owner) && CombatManager.canTarget(owner, entity)) {
                    if (entity.getLocation().distanceSquared(loc) <= radiusSq) {
                        LivingEntity target = (LivingEntity) entity;
                        UUID targetId = target.getUniqueId();

                        if (!sweptTargets.contains(targetId)) {
                            sweptTargets.add(targetId);

                            double hitDmg = CombatManager.calculateDamage(owner, rpgPlayer, target, sweepDmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(owner, target, hitDmg);
                        }
                    }
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