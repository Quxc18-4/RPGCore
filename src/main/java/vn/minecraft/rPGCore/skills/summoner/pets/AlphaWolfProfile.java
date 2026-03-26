package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class AlphaWolfProfile implements IPetProfile {

    @Override public String getId() { return "AlphaSpiritWolf"; }
    @Override public boolean isMagicDamage() { return false; }
    @Override public String getFusionTargetId() { return null; }
    @Override public String getFusionTargetName() { return null; }

    @Override
    public double getBaseFlatDamage(int level) { return 50.0 + (level - 1) * 20.0; }

    @Override
    public double getWeaponScaling(int level) { return 2.0 + (level * 0.8); }

    // Hệ Phái (Attacker, Swift, Defender, Caster, Vanguard)
    @Override
    public String getArchetype() { return "Attacker"; }
    // Thuộc Tính (Spirit, Fire, Ice, Wind, Earth, Light)
    @Override
    public String getElement() { return "Spirit"; }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        double petCount = extraArgs.length > 0 ? extraArgs[0] : 1;
        double finalHpBonus = extraArgs.length > 1 ? extraArgs[1] : 0;

        double scale = 1.5 + (petCount * 0.2);
        if (pet.getAttribute(Attribute.SCALE) != null) pet.getAttribute(Attribute.SCALE).setBaseValue(scale);

        double newMaxHp = 2500 + (level * 300) + (finalHpBonus * 0.8);
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMaxHp);
            pet.setHealth(newMaxHp);
        }
    }

    // ==========================================
    // CƠ CHẾ CƯỠI VÀ LÁI SÓI
    // ==========================================
    @Override
    public void onInteract(Player player, LivingEntity pet, ItemStack item) {
        if (item != null && item.getType() == Material.LEAD) {
            double scale = pet.getAttribute(Attribute.SCALE) != null ? pet.getAttribute(Attribute.SCALE).getValue() : 1.0;

            if (scale >= 2.5) {
                if (pet.getPassengers().isEmpty()) {
                    pet.addPassenger(player);
                    player.sendMessage("§a§l[!] §fBạn đã trèo lên lưng Linh Lang Vương!");
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f);

                    // Truyền 200% tốc độ của chủ cho Sói
                    RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
                    if (rpgPlayer != null && pet.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                        double playerSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ di chuyển", 0.0);
                        double newSpeed = 0.3 * (1.0 + ((playerSpeedBonus * 2.0) / 100.0));
                        pet.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(newSpeed);
                    }

                    // [FIX LỖI ĐIỀU KHIỂN]: Vòng lặp lái sói bằng Góc nhìn
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Hủy vòng lặp nếu sói chết hoặc người chơi xuống xe
                            if (pet.isDead() || !pet.isValid() || pet.getPassengers().isEmpty() || !pet.getPassengers().contains(player)) {
                                this.cancel();
                                return;
                            }

                            // Chỉ chạy khi người chơi đang cầm Dây Dắt trên tay
                            if (player.getInventory().getItemInMainHand().getType() == Material.LEAD) {
                                org.bukkit.util.Vector dir = player.getLocation().getDirection().setY(0).normalize();
                                double speed = pet.getAttribute(Attribute.MOVEMENT_SPEED).getValue();

                                // Ép sói xoay đầu theo hướng người chơi nhìn
                                pet.setRotation(player.getLocation().getYaw(), pet.getLocation().getPitch());

                                // Di chuyển sói tới trước mặt (Giữ nguyên gia tốc Y để sói vẫn rớt xuống khi qua mép núi)
                                pet.setVelocity(new org.bukkit.util.Vector(dir.getX() * speed * 2.5, pet.getVelocity().getY(), dir.getZ() * speed * 2.5));

                                // Bắn hạt lấp lánh dưới chân để tạo cảm giác "Đang tăng tốc"
                                pet.getWorld().spawnParticle(Particle.CLOUD, pet.getLocation(), 1, 0.2, 0.1, 0.2, 0.01);
                            }
                        }
                    }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 1L);

                } else {
                    pet.eject();
                    // Trả lại tốc độ gốc
                    if (pet.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                        pet.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3);
                    }
                }
            } else {
                player.sendMessage("§c§l[!] §fLinh Lang Vương này còn quá nhỏ để cưỡi!");
            }
        }
    }

    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        // [CƠ CHẾ]: Không thể tấn công khi đang bị cưỡi
        if (!pet.getPassengers().isEmpty()) return 0.0;

        double maxHp = pet.getAttribute(Attribute.MAX_HEALTH) != null ? pet.getAttribute(Attribute.MAX_HEALTH).getValue() : 100;
        double currentHp = pet.getHealth();
        double scale = pet.getAttribute(Attribute.SCALE) != null ? pet.getAttribute(Attribute.SCALE).getValue() : 1.0;

        boolean forceThunder = false;

        // [BẢO HIỂM]: Dưới 30% Máu -> Bất tử 2s và kích hoạt Sấm Sét
        if (currentHp <= maxHp * 0.3 && !pet.hasMetadata("Alpha_Thunder_Used")) {
            pet.setMetadata("Alpha_Thunder_Used", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), true));
            pet.setNoDamageTicks(40);
            pet.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, pet.getLocation().add(0, 1.5, 0), 50, 0.5, 0.5, 0.5, 0.2);
            owner.sendMessage("§c§l[Cảnh báo] §fLinh Lang Vương nổi điên (Dưới 30% HP)!");
            forceThunder = true;
        }

        double random = Math.random() * 100;

        // XÁC ĐỊNH CHIÊU THỨC MUỐN TUNG RA (Dựa trên RNG)
        boolean tryThunder = forceThunder || (random <= 5);
        boolean tryTail    = !tryThunder && (random <= 20);
        boolean tryClaw    = !tryThunder && !tryTail && (random <= 40);
        boolean tryBite    = !tryThunder && !tryTail && !tryClaw && (random <= 60);

        // ==========================================
        // 1. SẤM SÉT (Hồi chiêu 30s | 300% Sát thương)
        // ==========================================
        if (tryThunder && (forceThunder || isSkillReady(pet, "Alpha_CD_Thunder", 30000))) {
            setSkillCooldown(pet, "Alpha_CD_Thunder"); // Đặt Cooldown 30s

            double radius = 4.0 + (scale * 0.5);
            owner.sendMessage("§e§l[Linh Lang Vương] §fThi triển §cTrận Đồ Sấm Sét§f!");
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);

            StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 7);
            java.util.List<LivingEntity> markedTargets = new java.util.ArrayList<>();

            for (org.bukkit.entity.Entity e : pet.getNearbyEntities(radius, 3, radius)) {
                if (e instanceof LivingEntity && e != owner && !e.hasMetadata("Summoner_Pet")) {
                    LivingEntity victim = (LivingEntity) e;
                    org.bukkit.util.Vector push = victim.getLocation().toVector().subtract(pet.getLocation().toVector()).normalize().multiply(1.5).setY(0.5);
                    victim.setVelocity(push);
                    StatusEffectManager.applyEffect(victim, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 7);

                    if (victim != target) {
                        markedTargets.add(victim);
                    }
                }
            }

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks < 20) {
                        for (int i = 0; i < 360; i += 30) {
                            double rad = Math.toRadians(i + (ticks * 10));
                            Location pLoc = pet.getLocation().clone().add(Math.cos(rad) * radius, 0.2, Math.sin(rad) * radius);
                            pet.getWorld().spawnParticle(Particle.WITCH, pLoc, 1, 0, 0, 0, 0);
                        }
                        ticks++;
                    } else {
                        pet.getWorld().strikeLightningEffect(pet.getLocation());
                        for (LivingEntity markedVictim : markedTargets) {
                            if (markedVictim.isValid() && !markedVictim.isDead()) {
                                markedVictim.getWorld().strikeLightningEffect(markedVictim.getLocation());
                                CombatManager.applySkillDamage(owner, markedVictim, baseDamage * 3.0);
                                StatusEffectManager.applyEffect(markedVictim, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 7);
                            }
                        }
                        this.cancel();
                    }
                }
            }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 0L, 1L);

            return 3.0;
        }
        // ==========================================
        // 2. QUẤT ĐUÔI (Hồi chiêu 12s | 140% Sát thương)
        // ==========================================
        else if (tryTail && isSkillReady(pet, "Alpha_CD_Tail", 12000)) {
            setSkillCooldown(pet, "Alpha_CD_Tail"); // Đặt Cooldown 12s

            double radius = 3.0 + (scale * 0.3);
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

            StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 7);
            target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 0.6 + (scale * 0.2), 0)));

            double yOffset = 0.5;
            for (int i = 0; i < 360; i += 15) {
                double rad = Math.toRadians(i);
                Location pLoc = pet.getLocation().clone().add(Math.cos(rad) * radius, yOffset, Math.sin(rad) * radius);
                pet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
                pet.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0.1, 0.1, 0.1, 0.05);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    pet.getWorld().spawnParticle(Particle.EXPLOSION, pet.getLocation().add(0, yOffset, 0), 2, radius * 0.5, 0.5, radius * 0.5, 0);
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);

                    for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(pet.getLocation(), radius, 2, radius)) {
                        if (e instanceof LivingEntity && e != owner && e != target && !e.hasMetadata("Summoner_Pet")) {
                            LivingEntity aoeTarget = (LivingEntity) e;
                            CombatManager.applySkillDamage(owner, aoeTarget, baseDamage * 1.4);
                            aoeTarget.setVelocity(aoeTarget.getVelocity().add(new org.bukkit.util.Vector(0, 0.6 + (scale * 0.2), 0)));
                            StatusEffectManager.applyEffect(aoeTarget, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 7);
                        }
                    }
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 2L);

            return 1.4;
        }
        // ==========================================
        // 3. QUÀO BÁN NGUYỆT (Hồi chiêu 9s | 160% Sát thương)
        // ==========================================
        else if (tryClaw && isSkillReady(pet, "Alpha_CD_Claw", 9000)) {
            setSkillCooldown(pet, "Alpha_CD_Claw");

            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
            org.bukkit.util.Vector dir = pet.getLocation().getDirection().setY(0).normalize();
            Location center = pet.getLocation().clone().add(0, 1.2, 0).add(dir.clone().multiply(1.5));

            org.bukkit.util.Vector right = dir.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
            org.bukkit.util.Vector up = new org.bukkit.util.Vector(0, 1, 0);

            double length = 1.2;
            double spacing = 0.8;

            // [MỚI]: 50% cơ hội quào bằng tay trái (\), 50% bằng tay phải (/)
            boolean isLeftClaw = Math.random() < 0.5;

            for (int i = -1; i <= 1; i++) {
                Location lineCenter = center.clone().add(right.clone().multiply(i * spacing));

                for (double t = -length; t <= length; t += 0.1) {
                    // Toán học Vector: Nếu là tay trái thì đổi dấu chiều Y để đường cào ngược lại
                    double yDirection = isLeftClaw ? -t : t;

                    org.bukkit.util.Vector offset = right.clone().multiply(t).add(up.clone().multiply(yDirection));
                    Location point = lineCenter.clone().add(offset);

                    pet.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
                    Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
                    pet.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, whiteDust);
                }
            }

            pet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 5, 1.0, 0.2, 1.0, 0);

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(pet.getLocation(), 4, 2, 4)) {
                        if (e instanceof LivingEntity && e != owner && e != target && !e.hasMetadata("Summoner_Pet")) {
                            org.bukkit.util.Vector toTarget = e.getLocation().toVector().subtract(pet.getLocation().toVector()).setY(0).normalize();
                            if (dir.dot(toTarget) >= 0) {
                                CombatManager.applySkillDamage(owner, (LivingEntity) e, baseDamage * 1.6);
                            }
                        }
                    }
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 1L);

            return 1.6;
        }
        // ==========================================
        // 4. CẮN ĐƠN MỤC TIÊU (Hồi chiêu 6s | 150% Sát thương)
        // ==========================================
        else if (tryBite && isSkillReady(pet, "Alpha_CD_Bite", 6000)) {
            setSkillCooldown(pet, "Alpha_CD_Bite"); // Đặt Cooldown 6s

            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_WOLF_ANGRY_DEATH, 1.0f, 0.8f);
            Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.RED, 2.0f);
            pet.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.2, 0), 30, 0.4, 0.4, 0.4, 0.1, dust);
            pet.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.2, 0), 20, 0.3, 0.3, 0.3, 0, Material.REDSTONE_BLOCK.createBlockData());

            return 1.5;
        }
        // ==========================================
        // 5. ĐÁNH THƯỜNG (0s CD | Đánh bù | 100% Sát thương)
        // ==========================================
        else {
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_WOLF_ANGRY_DEATH, 1.0f, 1.0f);
            pet.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);

            return 1.0;
        }
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        // [Giữ nguyên logic nổ tung khi chết]
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
        pet.getWorld().strikeLightningEffect(pet.getLocation());
        pet.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pet.getLocation(), 1);

        double explosionDmg = pet.hasMetadata("Base_Damage_Snapshot") ? pet.getMetadata("Base_Damage_Snapshot").get(0).asDouble() * 3.0 : 5000;

        for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(pet.getLocation(), 5, 3, 5)) {
            // [ĐÃ SỬA]: Tích hợp CombatManager.canTarget để phân biệt địch/ta
            if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                LivingEntity target = (LivingEntity) e;

                // [ĐÃ SỬA]: Bọc cờ khóa Combo Ảo cho chủ nhân
                owner.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                CombatManager.applySkillDamage(owner, target, explosionDmg);
                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 7);
            }
        }
    }

    // ==========================================
    // HELPER: QUẢN LÝ HỒI CHIÊU KỸ NĂNG (COOLDOWN)
    // ==========================================
    private boolean isSkillReady(LivingEntity pet, String skillName, long cooldownMillis) {
        if (!pet.hasMetadata(skillName)) return true;
        long lastUsed = pet.getMetadata(skillName).get(0).asLong();
        return (System.currentTimeMillis() - lastUsed) >= cooldownMillis;
    }

    private void setSkillCooldown(LivingEntity pet, String skillName) {
        pet.setMetadata(skillName, new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis()));
    }
}