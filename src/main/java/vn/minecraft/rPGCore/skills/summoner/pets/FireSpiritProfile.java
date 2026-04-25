package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class FireSpiritProfile implements IPetProfile {

    @Override
    public String getId() { return "FireSpirit"; }

    @Override
    public boolean isMagicDamage() { return true; }

    @Override
    public String getFusionTargetId() { return "GrandFireSpirit"; }

    @Override
    public String getFusionTargetName() { return "Đại Tinh Linh Lửa"; }

    @Override
    public double getBaseFlatDamage(int level) {
        return 15.0 + (level * 2.0);
    }

    @Override
    public double getWeaponScaling(int level) {
        return 0.5 + (level * 0.05);
    }

    @Override
    public String getArchetype() { return "Caster"; }

    @Override
    public String getElement() { return "Fire"; }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        double baseHp = 500 + (level * 80);

        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }

        pet.setMetadata("Summoner_Pet", new FixedMetadataValue(RPGCore.getInstance(), owner.getUniqueId().toString()));
        pet.setMetadata("Pet_Type", new FixedMetadataValue(RPGCore.getInstance(), getId()));

        pet.getWorld().spawnParticle(Particle.FLAME, pet.getLocation(), 40, 0.5, 1.0, 0.5, 0.05);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);

        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());

        // ==========================================
        // NÃO BỘ TÂN TIẾN CỦA TINH LINH LỬA
        // ==========================================
        new BukkitRunnable() {
            int attackCooldown = 0; // Cooldown cho đòn bắn 3 tia
            int aoeCooldown = 80;   // Cooldown cho đòn nổ xung quanh (4 giây = 80 tick)

            @Override
            public void run() {
                if (!pet.isValid() || pet.isDead() || !owner.isOnline() || rpgOwner == null) {
                    this.cancel();
                    return;
                }

                // 1. Kìm chân xuống đất
                if (!pet.isOnGround() && pet.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType().isAir()) {
                    pet.setVelocity(pet.getVelocity().setY(-0.3));
                }

                if (Math.random() > 0.7) {
                    pet.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, pet.getLocation().add(0, 0.5, 0), 1, 0.2, 0.2, 0.2, 0);
                }

                // 2. KỸ NĂNG: THIÊU ĐỐT XUNG QUANH (Mỗi 4 giây / 80 tick)
                if (aoeCooldown > 0) {
                    aoeCooldown--;
                } else {
                    aoeCooldown = 80; // Reset 4 giây
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);
                    pet.getWorld().playSound(pet.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);

                    // Vẽ vòng tròn lửa bán kính R=3
                    for (int i = 0; i < 360; i += 15) {
                        double angle = Math.toRadians(i);
                        double x = Math.cos(angle) * 3.0;
                        double z = Math.sin(angle) * 3.0;
                        pet.getWorld().spawnParticle(Particle.FLAME, pet.getLocation().add(x, 0.2, z), 2, 0, 0.2, 0, 0.02);
                        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation().add(x, 0.2, z), 1, 0, 0, 0, 0);
                    }

                    // Gây 50% Sát thương và Cháy cho kẻ địch lọt vào vòng
                    for (org.bukkit.entity.Entity e : pet.getNearbyEntities(3.0, 3.0, 3.0)) {
                        if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                            LivingEntity hitTarget = (LivingEntity) e;
                            if (CombatManager.canTarget(owner, hitTarget)) {
                                // Tính tổng sát thương chuẩn của Pet, sau đó cắt lấy 50%
                                double baseDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, hitTarget, 0);
                                double finalAoeDmg = baseDmg * 0.50;

                                owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                CombatManager.applySkillDamage(owner, hitTarget, finalAoeDmg);
                                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                                hitTarget.setFireTicks(60); // Gây cháy 3 giây
                            }
                        }
                    }
                }

                // 3. KỸ NĂNG: BẮN ĐẠN 3 TIA CHÙM (Mỗi 2 giây / 40 tick)
                if (attackCooldown > 0) {
                    attackCooldown--;
                } else {
                    LivingEntity targetToShoot = SummonPetManager.getSmartTarget(owner);

                    if (targetToShoot != null) {
                        // [BỔ SUNG]: THÊM KHỐI CHẶN KHOẢNG CÁCH NÀY VÀO
                        if (!targetToShoot.getWorld().equals(pet.getWorld()) || pet.getLocation().distance(targetToShoot.getLocation()) > 25.0 || !pet.hasLineOfSight(targetToShoot)) {
                            return; // Nếu xa quá hoặc bị khuất tường -> Ngừng bắn, chờ chủ tới gần
                        }

                        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 1.5f);

                        Location shootLoc = pet.getLocation().add(0, pet.getHeight() / 2, 0);
                        Location targetCenter = targetToShoot.getLocation().add(0, targetToShoot.getHeight() / 2, 0);

                        // Vector hướng thẳng vào mục tiêu
                        Vector centerDir = targetCenter.toVector().subtract(shootLoc.toVector()).normalize().multiply(1.0);

                        // Bẻ vector sang trái 15 độ và phải 15 độ
                        Vector leftDir = rotateVectorAroundY(centerDir, -15);
                        Vector rightDir = rotateVectorAroundY(centerDir, 15);

                        // Bắn 3 viên đạn tàng hình (Mỗi viên gây 33% dmg)
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, centerDir);
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, leftDir);
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, rightDir);

                        attackCooldown = 40;
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // HÀM PHỤ LỤC: TOÁN HỌC & ĐẠN ĐẠO
    // ==========================================

    // Thuật toán xoay Vector theo trục Y để tạo đường đạn hình quạt
    private Vector rotateVectorAroundY(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double currentX = vector.getX();
        double currentZ = vector.getZ();
        double cosine = Math.cos(rad);
        double sine = Math.sin(rad);
        return new Vector((cosine * currentX - sine * currentZ), vector.getY(), (sine * currentX + cosine * currentZ));
    }

    // Hàm bắn đạn Particle tàng hình (Không dùng SmallFireball của Vanilla để chống lỗi)
    private void shootCustomFireball(Player owner, RPGPlayer rpgOwner, LivingEntity pet, Location startLoc, Vector dir) {
        new BukkitRunnable() {
            Location current = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 30 || !pet.isValid() || !owner.isOnline()) { // Bay tối đa 30 block/tick
                    this.cancel();
                    return;
                }

                current.add(dir); // Tiến lên phía trước

                // Hiệu ứng quả cầu lửa bay
                pet.getWorld().spawnParticle(Particle.FLAME, current, 3, 0.1, 0.1, 0.1, 0.02);
                if (ticks % 2 == 0) pet.getWorld().spawnParticle(Particle.SMOKE, current, 1, 0, 0, 0, 0);

                // Check va chạm địa hình
                if (current.getBlock().getType().isSolid()) {
                    pet.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.1, 0.1, 0.1, 0.1);
                    this.cancel();
                    return;
                }

                // Check va chạm mục tiêu
                for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(current, 0.6, 0.6, 0.6)) {
                    if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                        LivingEntity hitTarget = (LivingEntity) e;

                        if (CombatManager.canTarget(owner, hitTarget)) {
                            // TÍNH ĐÚNG 33% SÁT THƯƠNG
                            double baseDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, hitTarget, 0);
                            double finalShotgunDmg = baseDmg * 0.33;

                            owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(owner, hitTarget, finalShotgunDmg);
                            owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            // Hiệu ứng trúng đạn và thiêu đốt
                            hitTarget.setFireTicks(60); // Cháy 3s
                            pet.getWorld().spawnParticle(Particle.LAVA, current, 10, 0.2, 0.2, 0.2, 0.1);
                            pet.getWorld().playSound(current, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);

                            this.cancel();
                            return; // Hủy viên đạn sau khi găm trúng
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        return 1.0;
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
    }

    @Override
    public void onInteract(Player owner, LivingEntity pet, ItemStack item) {
        owner.playSound(owner.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.5f);
        owner.sendMessage("§cTinh Linh Lửa lách tách tỏa ra hơi ấm nhiệt độ cao...");
    }
}