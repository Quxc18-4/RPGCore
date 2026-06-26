package vn.minecraft.rPGCore.classes.impl.bard;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;
import java.util.Map;

public class BardClass extends RPGClass {

    // Hằng số Metadata Keys dựa theo GDD
    public static final String META_VALOR_BUFF = "Bard_Valor_Buff";
    public static final String META_AEGIS_BUFF = "Bard_Aegis_Buff";
    public static final String META_GLOOM_DEBUFF = "Bard_Gloom_Debuff";
    public static final String META_DISSONANCE_DEBUFF = "Bard_Dissonance_Debuff";
    public static final String META_SIPHON_MARK = "Bard_Siphon_Mark";

    // Khai báo công cụ Random và mảng Nhạc Cụ ở ngay đầu class BardClass
    private final java.util.Random random = new java.util.Random();
    private final org.bukkit.Sound[] BARD_INSTRUMENTS = {
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_GUITAR,
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_FLUTE,
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME,
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL,
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP
    };

    // Quản lý Cooldown hồi máu của Hút Hồn để chống lạm dụng (Spam)
    private final Map<java.util.UUID, Long> siphonHealCooldowns = new java.util.HashMap<>();

    public BardClass(String id, String displayName, Material icon, List<String> description,
                     double maxHealth, double maxResource, ResourceType resourceType,
                     double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);

        // ==========================================
        // THIẾT LẬP GIỚI HẠN (CAP) THEO GDD
        // ==========================================
        this.moveSpeedCap = 600.0; // Mở khóa Tốc độ di chuyển lên 600% (Tiến trình vi mô)
        this.effectResistCap = 100.0; // Đạt max 100% Kháng hiệu ứng

        // ==========================================
        // HOOK 1: GIAI ĐIỆU DŨNG CẢM (VALOR)
        // +10% Sát thương đầu ra cho đồng minh
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata(META_VALOR_BUFF)) {
                    return 0.10;
                }
                return 0.0;
            }
        });

        // ==========================================
        // HOOK 2: GIAI ĐIỆU CAN TRƯỜNG (AEGIS)
        // -10% Sát thương nhận vào cho đồng minh
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_AEGIS_BUFF)) {
                    return 0.10; // Giảm 10%
                }
                return 0.0;
            }
        });

        // ==========================================
        // HOOK 3: GIAI ĐIỆU U ÁM (GLOOM) & LÕI 6 (DISSONANCE)
        // U Ám: Quái vật bị giảm 10% sát thương gây ra.
        // Nghịch Âm: Quái vật chịu thêm 15% sát thương (Mô phỏng -15% Giáp/Kháng phép)
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata(META_GLOOM_DEBUFF)) {
                    return -0.10; // Quái vật đánh yếu đi 10%
                }
                return 0.0;
            }

            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_DISSONANCE_DEBUFF)) {
                    return -0.15; // Trả về ÂM -> Nạn nhân nhận THÊM 15% sát thương
                }
                return 0.0;
            }
        });
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        Player player = rpgPlayer.getBukkitPlayer();

        // 1. TIẾN TRÌNH VI MÔ: Bơm % Giảm hồi chiêu (CDR)
        if (player.hasMetadata("BARD_BONUS_CDR")) {
            double bonusCdr = player.getMetadata("BARD_BONUS_CDR").get(0).asDouble();
            stats.put("Giảm hồi chiêu", stats.getOrDefault("Giảm hồi chiêu", 0.0) + bonusCdr);
        }

        // 2. LÕI 5: BỘI ÂM (OVERTONE) - Cấp MS và Kháng Hiệu Ứng động theo số Âm đang giữ
        if (player.hasMetadata("BARD_OVERTONE_ACTIVE") && player.hasMetadata("BARD_TONE_COUNT")) {
            int toneCount = player.getMetadata("BARD_TONE_COUNT").get(0).asInt();
            if (toneCount > 0) {
                stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (20.0 * toneCount));
                stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + (15.0 * toneCount));
            }
        }

        // 3. ÂM SẮC (SHARP TONE) - Dạng Buff (Kháng CC)
        if (player.hasMetadata("BARD_SHARP_BUFF")) {
            stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + 10.0);
        }

        // 4. ÂM SẮC (SHARP TONE) - Dạng Tấn Công (Xuyên Thấu)
        if (player.hasMetadata("BARD_SHARP_ATTACK")) {
            stats.put("Tỉ lệ xuyên giáp", stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + 10.0);
            stats.put("Tỉ lệ xuyên kháng phép", stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + 10.0);
        }

        // 5. GIAI ĐIỆU DỒN DẬP (UPBEAT) - Bơm MS và Attack Speed
        if (player.hasMetadata("BARD_UPBEAT_BUFF")) {
            stats.put("Tốc độ đánh", stats.getOrDefault("Tốc độ đánh", 0.0) + 15.0);
            stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + 30.0);
        }

        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // ==========================================
    // HOOK SỰ KIỆN TẤN CÔNG (ON ATTACK LANDED)
    // Xử lý Giai điệu Hút Hồn (Siphon) - Hồi máu trên đòn đánh
    // ==========================================
    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        if (target.hasMetadata(META_SIPHON_MARK)) {
            long currentTime = System.currentTimeMillis();
            long lastHealTime = siphonHealCooldowns.getOrDefault(attacker.getUniqueId(), 0L);

            // Tối đa hồi 5 lần/giây (Tương đương Delay 200ms) để chống lạm dụng
            if (currentTime - lastHealTime >= 200) {
                RPGPlayer rpgAttacker = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
                if (rpgAttacker != null) {
                    // Sử dụng hàm an toàn từ RPGPlayer
                    double maxHp = rpgAttacker.getTotalMaxHealth();
                    double healAmount = maxHp * 0.005; // 0.5% Max HP

                    // Gọi thông qua RPGPlayer.heal() để áp dụng màng lọc Anti-Heal
                    rpgAttacker.heal(healAmount);

                    // Chỉ hiển thị Particle khi đòn đánh trúng, không cần biết có bị chặn hồi máu hay không (để giữ feedback hình ảnh)
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.HEART, attacker.getLocation().add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);

                    siphonHealCooldowns.put(attacker.getUniqueId(), currentTime);
                }
            }
            // [MỚI] LÕI 2: NHỊP ĐIỆU DỒN DẬP (Đánh trúng giảm 0.5s hồi chiêu kỹ năng)
            RPGPlayer rpgAttacker = RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
            if (rpgAttacker != null && rpgAttacker.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
                for (String skillId : rpgAttacker.getSkillData().getSkills().keySet()) {
                    String cdKey = "CD_" + skillId;
                    if (attacker.hasMetadata(cdKey)) {
                        long currentExpire = attacker.getMetadata(cdKey).get(0).asLong();
                        long now = System.currentTimeMillis();
                        if (currentExpire > now) {
                            // Trừ đi 500ms (0.5s) cho mỗi đòn đánh
                            attacker.setMetadata(cdKey, new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), currentExpire - 500L));
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // KHU VỰC QUẢN LÝ ĐÒN ĐÁNH THƯỜNG (BASIC ATTACK)
    // Cơ chế mới: Dual-Mode (Truy Ảnh 180 độ / Đạn Xuyên Thấu)
    // ==========================================
    private final Map<java.util.UUID, Long> nextAttackTime = new java.util.HashMap<>();

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, org.bukkit.entity.LivingEntity target, org.bukkit.inventory.ItemStack weapon) {
        return false;
    }

    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, org.bukkit.inventory.ItemStack weapon) {
        long currentTime = System.currentTimeMillis();
        long allowedTime = nextAttackTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return false;

        double totalAtkSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long attackCooldown = (long) (800 / (1.0 + (totalAtkSpeedBonus / 100.0)));
        attackCooldown = Math.max(200, attackCooldown);
        nextAttackTime.put(player.getUniqueId(), currentTime + attackCooldown);

        fireDualModeChords(player, rpgPlayer);
        return false;
    }

    private void fireDualModeChords(Player player, RPGPlayer rpgPlayer) {
        org.bukkit.Sound instrument = BARD_INSTRUMENTS[random.nextInt(BARD_INSTRUMENTS.length)];
        player.getWorld().playSound(player.getLocation(), instrument, 1.0f, 1.5f);

        // [ĐÃ SỬA]: Tăng Bán kính quét lên 8.5 Block (Tầm đánh chuẩn của Pháp Sư Tầm Trung)
        double searchRadius = 8.5;
        java.util.List<org.bukkit.entity.LivingEntity> validTargets = new java.util.ArrayList<>();
        org.bukkit.util.Vector playerDir = player.getEyeLocation().getDirection().normalize();

        for (org.bukkit.entity.Entity e : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            // Tuân thủ Luật: Nhận dạng đồng minh trước khi Target
            if (e instanceof org.bukkit.entity.LivingEntity && CombatManager.canTarget(player, e)) {
                org.bukkit.util.Vector dirToTarget = e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

                // Thuật toán: Chỉ quét góc 180 độ phía trước mặt
                if (playerDir.dot(dirToTarget) > 0) {
                    validTargets.add((org.bukkit.entity.LivingEntity) e);
                }
            }
        }

        if (validTargets.isEmpty()) {
            // Không có quái -> Đạn Skillshot 100% ST (Bắn mù)
            launchSkillshotChord(player, rpgPlayer);
        } else {
            // Có quái -> Đạn Truy Ảnh chia 3 (Mỗi tia 33% ST)
            java.util.Collections.shuffle(validTargets);
            java.util.List<org.bukkit.entity.LivingEntity> finalTargets = validTargets.subList(0, Math.min(3, validTargets.size()));

            for (org.bukkit.entity.LivingEntity target : finalTargets) {
                launchHomingChord(player, rpgPlayer, target);
            }
        }
    }

    // ==========================================
    // CHẾ ĐỘ 1: ĐẠN SKILLSHOT ĐƠN MỤC TIÊU (100% SÁT THƯƠNG)
    // ==========================================
    private void launchSkillshotChord(Player player, RPGPlayer rpgPlayer) {
        new org.bukkit.scheduler.BukkitRunnable() {
            org.bukkit.Location currentLoc = player.getEyeLocation().clone().add(0, -0.3, 0);
            org.bukkit.util.Vector direction = player.getEyeLocation().getDirection().normalize().multiply(0.9); // Tốc độ bay khá nhanh
            int ticksLived = 0;
            double noteColor = Math.random();

            org.bukkit.util.Vector randomAxis = new org.bukkit.util.Vector(Math.random(), Math.random(), Math.random()).normalize();
            org.bukkit.util.Vector perpVector = direction.clone().crossProduct(randomAxis).normalize();

            @Override
            public void run() {
                // Tầm bắn xa: ~18 block (20 ticks * 0.9 tốc độ)
                if (ticksLived++ > 20 || currentLoc.getBlock().getType().isSolid() || !player.isOnline()) {
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 5, 0.1, 0.1, 0.1, 0.05);
                    this.cancel();
                    return;
                }

                double waveOffset = Math.sin(ticksLived * 0.5) * 0.3;
                org.bukkit.Location visualLoc = currentLoc.clone().add(perpVector.clone().multiply(waveOffset));

                player.getWorld().spawnParticle(Particle.NOTE, visualLoc.clone().add(0, -0.4, 0), 0, noteColor, 0, 0, 1);

                for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                        LivingEntity target = (LivingEntity) e;

                        // ST Kỹ năng 100%
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, 1.0, 0.0, true, false);

                        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(player, target, dmg);
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                        player.getWorld().playSound(currentLoc, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 15, 0.2, 0.2, 0.2, 0.1);
                        this.cancel();
                        return;
                    }
                }
                currentLoc.add(direction);
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // CHẾ ĐỘ 2: ĐẠN TRUY ẢNH 180 ĐỘ (33% SÁT THƯƠNG MỖI TIA)
    // ==========================================
    private void launchHomingChord(Player player, RPGPlayer rpgPlayer, LivingEntity target) {
        new org.bukkit.scheduler.BukkitRunnable() {
            org.bukkit.Location currentLoc = player.getEyeLocation().clone().add(0, -0.3, 0);
            int ticksLived = 0;
            double noteColor = Math.random();

            @Override
            public void run() {
                // Lifespan 40 ticks đủ để bay hết cự ly 8.5 Block
                if (ticksLived++ > 40 || !target.isValid() || target.isDead() || !player.isOnline() || target.getWorld() != currentLoc.getWorld()) {
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 5, 0.1, 0.1, 0.1, 0.05);
                    this.cancel();
                    return;
                }

                org.bukkit.Location targetEyeLoc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
                org.bukkit.util.Vector direction = targetEyeLoc.toVector().subtract(currentLoc.toVector());
                double distance = direction.length();

                if (distance < 1.2) { // Mở rộng Hitbox va chạm một chút để đảm bảo trúng
                    // ST Kỹ năng 33% (Phân tách)
                    double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.33, 0.0, true, false);

                    player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, target, dmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                    player.getWorld().playSound(currentLoc, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.5f);
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 8, 0.2, 0.2, 0.2, 0.1);
                    this.cancel();
                    return;
                }

                // [ĐÃ SỬA]: Tốc độ rượt đuổi đẩy lên 0.75 block/tick để bắt kịp khoảng cách 8.5 Block
                direction.normalize().multiply(0.75);

                org.bukkit.util.Vector right = direction.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
                if (right.lengthSquared() == 0) {
                    right = direction.clone().crossProduct(new org.bukkit.util.Vector(1, 0, 0)).normalize();
                }

                double waveOffset = Math.sin(ticksLived * 0.4) * 0.4;
                org.bukkit.Location visualLoc = currentLoc.clone().add(right.multiply(waveOffset));

                player.getWorld().spawnParticle(Particle.NOTE, visualLoc.clone().add(0, -0.4, 0), 0, noteColor, 0, 0, 1);
                currentLoc.add(direction);
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, org.bukkit.inventory.ItemStack weapon) {
        // Kiểm tra xem người chơi có điểm Lõi trong Database không
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        if (unspent > 0) {
            // Mở Menu Random 3 Lõi
            vn.minecraft.rPGCore.skills.bard.BardCoreGUI.openSelectionMenu(player, rpgPlayer);
            return true; // Hủy sự kiện văng vũ khí
        }

        player.sendMessage("§cBạn chưa có Điểm Lõi Nâng Cấp nào. Hãy di chuyển thêm!");
        return true;
    }
}