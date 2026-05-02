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
    @Override public String getReactionName() { return "§e§lNgọn Thương Ánh Sáng"; }
    @Override public double getDamageMultiplier() { return 1.20; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        double pierceDmg = totalDmg * 0.40;
        double expDmg = totalDmg * 0.60;
        int maxSpears = 6;
        int duration = 15;
        int stickChance = 30; // 30% Xác suất kẹt

        return Arrays.asList(
                "§e[Hình thái Dung Hợp - Ngọn Thương Ánh Sáng]",
                "§f- Triệu hồi 2 Ngọn Thương Ánh Sáng ở trạng thái chuẩn bị.",
                "",
                "§f§c[Kích Hoạt - Hiệu Lệnh Tấn Công]",
                "§f- Đánh thường để ra lệnh cho",
                "§f  1 ngọn thương lao thẳng về phía trước.",
                "§f- Đâm xuyên qua kẻ địch, gây §c" + (int)pierceDmg + "% §fSát thương.",
                "",
                "§f§b[Nội Tại - Quang Bạo]", // Đổi tên từ Chém Quét -> Quang Bạo
                "§f- Mỗi khi xuyên qua một kẻ địch, sẽ có",
                "§f  xác suất §a" + stickChance + "% §fngọn thương bị kẹt lại.",
                "§f- Khi đó, thương sẽ §ephát nổ§f, gây §c" + (int)expDmg + "% §fSát thương.",
                "",
                "§f§b[Nội Tại - Tích Trữ]",
                "§f- Có thể tích trữ tối đa §a" + maxSpears + " §fngọn thương.",
                "§f- Thời gian duy trì: §a" + duration + "s§f."
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

    // [CẬP NHẬT]: Trả về boolean để báo hiệu cho MageClass biết đã bắn giáo hay chưa
    public static boolean tryFireBlade(Player caster, RPGPlayer rpgPlayer) {
        UUID pid = caster.getUniqueId();
        if (activeFormations.containsKey(pid)) {
            activeFormations.get(pid).fireBlade();
            return true; // Trả về true -> Hủy đòn đánh thường cơ bản
        }
        return false;
    }

    // [CẬP NHẬT]: Hàm dọn dẹp khẩn cấp khi Server tắt/reload (Gọi trong onDisable)
    public static void cleanupAll() {
        for (BladeFormation formation : activeFormations.values()) {
            formation.cleanup();
        }
        activeFormations.clear();
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

        private static final double[][] FAN_POSITIONS = {
                {-1.0, -0.2,  20 },
                {1.0, -0.2, 160 },
                {-1.1,  0.5,  40 },
                {1.1,  0.5, 140 },
                {-0.8,  1.1,  70 },
                {0.8,  1.1, 110 }
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
                    Vector forward = center.getDirection().setY(0).normalize();
                    Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                    Vector back = forward.clone().multiply(-1);

                    Location planeCenter = center.clone().add(back.clone().multiply(1.0)).add(0, 1.0, 0);

                    for (int i = 0; i < blades.size(); i++) {
                        ItemDisplay blade = blades.get(i);
                        double offsetX = FAN_POSITIONS[i][0];
                        double offsetY = FAN_POSITIONS[i][1];
                        float rotZ = (float) Math.toRadians(FAN_POSITIONS[i][2]) - (float) (Math.PI / 4);

                        Location targetLoc = planeCenter.clone()
                                .add(right.clone().multiply(offsetX))
                                .add(0, offsetY, 0);
                        targetLoc.setDirection(forward);

                        blade.teleport(targetLoc);

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

                    if (stateTicks == 0) {
                        firedBlade.setTeleportDuration(5);
                        firedBlade.setInterpolationDuration(5);
                        firedBlade.setInterpolationDelay(0);

                        Location prepLoc = owner.getEyeLocation().add(owner.getLocation().getDirection().normalize().multiply(1.2));
                        prepLoc.setDirection(owner.getLocation().getDirection());

                        firedBlade.teleport(prepLoc);
                        firedBlade.setTransformation(flyTransform);
                    }

                    if (stateTicks < 6) {
                        stateTicks++;
                        return;
                    }

                    if (stateTicks == 6) {
                        firedBlade.setTeleportDuration(0);
                        fireDirection = owner.getEyeLocation().getDirection().normalize();
                        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.5f);
                        stateTicks++;
                    }

                    Location currentLoc = firedBlade.getLocation();
                    Location prevLoc = currentLoc.clone(); // Lưu tọa độ cũ để vẽ Trail

                    currentLoc.add(fireDirection.clone().multiply(1.5));
                    currentLoc.setDirection(fireDirection);

                    firedBlade.teleport(currentLoc);

                    // [CẬP NHẬT VFX]: Vẽ Trail nét liền bằng thuật toán Nội suy tuyến tính (Lerp)
                    double distance = prevLoc.distance(currentLoc);
                    Particle.DustOptions goldDust = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f);
                    for (double d = 0; d <= distance; d += 0.3) {
                        Location trailPoint = prevLoc.clone().add(fireDirection.clone().multiply(d));
                        trailPoint.getWorld().spawnParticle(Particle.END_ROD, trailPoint, 1, 0, 0, 0, 0);
                        trailPoint.getWorld().spawnParticle(Particle.DUST, trailPoint, 1, 0, 0, 0, 0, goldDust, true);
                    }
                    // Va chạm tường
                    if (currentLoc.getBlock().getType().isSolid()) {
                        currentLoc.getWorld().spawnParticle(Particle.ITEM, currentLoc, 20, 0.2, 0.2, 0.2, 0.05, new ItemStack(Material.valueOf("GOLDEN_SPEAR")));
                        currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                        firedBlade.remove();
                        this.cancel();
                        return;
                    }

                    // Va chạm quái (XUYÊN + XÁC SUẤT NỔ)
                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                        if (entity instanceof LivingEntity && !entity.equals(owner) && CombatManager.canTarget(owner, entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            UUID targetId = target.getUniqueId();

                            if (!piercedTargets.contains(targetId)) {
                                piercedTargets.add(targetId);

                                // 1. ST Đâm Xuyên (Luôn gây ra khi chạm)
                                double pierceDmgMulti = totalDmgMulti * 0.40;
                                double hitDmg = CombatManager.calculateDamage(owner, rpgPlayer, target, pierceDmgMulti, 0, true, false);
                                CombatManager.applySkillDamage(owner, target, hitDmg);

                                target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.2f);

                                // 2. Đổ xúc xắc (RNG): 30% Kẹt lại và Phát Nổ
                                if (Math.random() <= 0.30) {
                                    // Kích nổ Quang Bạo tại vị trí con quái
                                    executeLightExplosion(target.getLocation(), sweptTargets);

                                    // Hủy mũi thương ngay lập tức, ngắt luồng bay xuyên thấu
                                    firedBlade.remove();
                                    this.cancel();
                                    return;
                                }
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

        private void executeLightExplosion(Location loc, Set<UUID> sweptTargets) {
            // Hiệu ứng âm thanh vụ nổ pha lê ánh sáng
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.5f);

            // Hiệu ứng hạt: Chớp sáng và các mảnh văng tung tóe
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 1);
            loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 2);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 25, 0.8, 0.8, 0.8, 0.15);

            double expDmgMulti = totalDmgMulti * 0.60;
            double radiusSq = 3.5 * 3.5;

            // Gây sát thương nổ diện rộng (Bán kính 3.5m)
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
                if (entity instanceof LivingEntity && !entity.equals(owner) && CombatManager.canTarget(owner, entity)) {
                    if (entity.getLocation().distanceSquared(loc) <= radiusSq) {
                        LivingEntity target = (LivingEntity) entity;
                        UUID targetId = target.getUniqueId();

                        // Tránh quái bị dính nổ nhiều lần từ 1 mũi thương
                        if (!sweptTargets.contains(targetId)) {
                            sweptTargets.add(targetId);

                            double hitDmg = CombatManager.calculateDamage(owner, rpgPlayer, target, expDmgMulti, 0, true, false);
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