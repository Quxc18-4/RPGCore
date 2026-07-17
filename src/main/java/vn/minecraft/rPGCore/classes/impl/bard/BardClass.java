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

        this.moveSpeedCap = 600.0;
        this.effectResistCap = 100.0;

        // ==========================================
        // HOOK 1: GIAI ĐIỆU DŨNG CẢM (VALOR)
        // [CÂN BẰNG]: +20% Sát thương đầu ra cho đồng minh
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata(META_VALOR_BUFF)) {
                    return 0.20;
                }
                return 0.0;
            }
        });

        // ==========================================
        // HOOK 2: GIAI ĐIỆU CAN TRƯỜNG (AEGIS)
        // [CÂN BẰNG]: -12% Sát thương nhận vào cho đồng minh
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_AEGIS_BUFF)) {
                    return 0.12;
                }
                return 0.0;
            }
        });

        // ==========================================
        // HOOK 3: GIAI ĐIỆU U ÁM (GLOOM) & LÕI 6 (DISSONANCE)
        // U Ám: Quái vật bị giảm 10% sát thương gây ra.
        // Nghịch Âm: Quái vật chịu thêm 15% sát thương
        // ==========================================
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity target) {
                if (attacker.hasMetadata(META_GLOOM_DEBUFF)) {
                    return -0.10;
                }
                return 0.0;
            }

            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_DISSONANCE_DEBUFF)) {
                    return -0.15;
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

        // Đã xóa BARD_SHARP_BUFF (10% Kháng hiệu ứng) theo yêu cầu cân bằng mới.

        // 3. ÂM SẮC (SHARP TONE) - Dạng Tấn Công (Xuyên Thấu)
        if (player.hasMetadata("BARD_SHARP_ATTACK")) {
            stats.put("Tỉ lệ xuyên giáp", stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + 10.0);
            stats.put("Tỉ lệ xuyên kháng phép", stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + 10.0);
        }

        // 4. GIAI ĐIỆU DỒN DẬP (UPBEAT) - Bơm MS và Attack Speed
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

            if (currentTime - lastHealTime >= 200) {
                RPGPlayer rpgAttacker = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
                if (rpgAttacker != null) {
                    double maxHp = rpgAttacker.getTotalMaxHealth();
                    double healAmount = maxHp * 0.005; // 0.5% Max HP

                    rpgAttacker.heal(healAmount);
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.HEART, attacker.getLocation().add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);

                    siphonHealCooldowns.put(attacker.getUniqueId(), currentTime);
                }
            }
            // LÕI 2: NHỊP ĐIỆU DỒN DẬP (Đánh trúng giảm 0.5s hồi chiêu kỹ năng)
            RPGPlayer rpgAttacker = RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
            if (rpgAttacker != null && rpgAttacker.getSkillData().getSkillLevel("BARD_CORE_2") > 0) {
                for (String skillId : rpgAttacker.getSkillData().getSkills().keySet()) {
                    String cdKey = "CD_" + skillId;
                    if (attacker.hasMetadata(cdKey)) {
                        long currentExpire = attacker.getMetadata(cdKey).get(0).asLong();
                        long now = System.currentTimeMillis();
                        if (currentExpire > now) {
                            attacker.setMetadata(cdKey, new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), currentExpire - 500L));
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // KHU VỰC QUẢN LÝ ĐÒN ĐÁNH THƯỜNG (BASIC ATTACK)
    // Cơ chế: Đạn Lượn Sóng - Khóa Mục Tiêu Phân Tán & Tương tác Âm Sắc
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

        fireAdaptiveChords(player, rpgPlayer);
        return false;
    }

    private void fireAdaptiveChords(Player player, RPGPlayer rpgPlayer) {
        // 1. ĐỌC CHỈ SỐ ÂM SẮC (TONES) TỪ NGƯỜI CHƠI
        java.util.List<vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType> tones =
                vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager.getCurrentTones(player);

        int sharpCount = 0, lowCount = 0, echoCount = 0, highCount = 0;
        for (vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType t : tones) {
            if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.SHARP) sharpCount++;
            else if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.LOW) lowCount++;
            else if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.ECHO) echoCount++;
            else if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.HIGH) highCount++;
        }

        // 2. TÍNH TOÁN CÁC CHỈ SỐ BỔ TRỢ TỪ ÂM
        // Số lượng đạn (Âm Vang)
        int projCount = Math.min(4, echoCount + 1);

        // Hệ số sát thương cơ bản chia theo số lượng tia
        double baseDmgMulti = 1.0;
        if (projCount == 2) baseDmgMulti = 0.50;
        else if (projCount == 3) baseDmgMulti = 0.33;
        else if (projCount == 4) baseDmgMulti = 0.30;

        // Cơ chế của Âm Sắc (Sharp): Ở basic attack vẫn có thể cộng sát thương nhẹ, hoặc bạn muốn gỡ luôn thì báo tôi nhé.
        // Hiện tại vẫn đang giữ +10% DMG cho tia đạn.
        double finalDmgMulti = baseDmgMulti + (sharpCount * 0.10);

        // Âm Trầm: Làm chậm 15% mỗi Âm
        double slowPercent = lowCount * 0.15;

        // Âm Cao: Gây nổ AoE 3x3 khi trúng đích (Không cộng dồn)
        boolean hasAoE = highCount > 0;

        org.bukkit.Sound instrument = BARD_INSTRUMENTS[random.nextInt(BARD_INSTRUMENTS.length)];
        player.getWorld().playSound(player.getLocation(), instrument, 1.0f, 1.5f);

        // 3. TOÁN HỌC VECTOR: TÍNH GÓC BẮN TỎA RA (SPREAD ANGLES)
        double[] spreadAngles;
        if (projCount == 1) spreadAngles = new double[]{0};
        else if (projCount == 2) spreadAngles = new double[]{-12, 12};
        else if (projCount == 3) spreadAngles = new double[]{-15, 0, 15};
        else spreadAngles = new double[]{-20, -7, 7, 20}; // Có Lõi 8 (4 đạn)

        // [QUAN TRỌNG]: Bộ nhớ dùng chung để phân phối đạn, tránh 4 đạn cắm vào 1 con
        java.util.Set<java.util.UUID> lockedTargets = new java.util.HashSet<>();

        // Phóng các nốt nhạc
        for (int i = 0; i < projCount; i++) {
            launchAdaptiveProjectile(player, rpgPlayer, finalDmgMulti, slowPercent, hasAoE, spreadAngles[i], lockedTargets);
        }
    }

    private void launchAdaptiveProjectile(Player player, RPGPlayer rpgPlayer, double dmgMulti, double slowPercent, boolean hasAoE, double yawOffsetDegrees, java.util.Set<java.util.UUID> lockedTargets) {
        new org.bukkit.scheduler.BukkitRunnable() {
            org.bukkit.Location currentLoc = player.getEyeLocation().clone().add(0, -0.3, 0);
            org.bukkit.util.Vector direction = player.getEyeLocation().getDirection().normalize();

            int ticksLived = 0;
            double noteColor = Math.random();
            org.bukkit.entity.LivingEntity homingTarget = null;

            // Xử lý góc bắn chùm (Spread)
            {
                double radians = Math.toRadians(yawOffsetDegrees);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                double x = direction.getX() * cos - direction.getZ() * sin;
                double z = direction.getX() * sin + direction.getZ() * cos;
                direction.setX(x).setZ(z).normalize().multiply(0.85); // Tốc độ bay 0.85
            }

            @Override
            public void run() {
                // Lifespan 25 ticks (~21 block)
                if (ticksLived++ > 25 || currentLoc.getBlock().getType().isSolid() || !player.isOnline()) {
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 3, 0.1, 0.1, 0.1, 0.05);
                    this.cancel();
                    return;
                }

                // 1. CẢM BIẾN DÒ TÌM THÔNG MINH (Bán kính 3.5 = Hộp quét 7x7)
                if (homingTarget == null || !homingTarget.isValid() || homingTarget.isDead()) {
                    double closestUnlockedDist = 12.25; // 3.5 * 3.5
                    double closestLockedDist = 12.25;
                    org.bukkit.entity.LivingEntity bestUnlocked = null;
                    org.bukkit.entity.LivingEntity bestLocked = null;

                    for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 3.5, 3.5, 3.5)) {
                        if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            double d = e.getLocation().distanceSquared(currentLoc);

                            // Phân loại: Đạn khác đã xí phần mục tiêu này chưa?
                            if (lockedTargets.contains(e.getUniqueId())) {
                                if (d < closestLockedDist) {
                                    closestLockedDist = d;
                                    bestLocked = (LivingEntity) e;
                                }
                            } else {
                                if (d < closestUnlockedDist) {
                                    closestUnlockedDist = d;
                                    bestUnlocked = (LivingEntity) e;
                                }
                            }
                        }
                    }

                    // Ưu tiên chọn mục tiêu CHƯA BỊ KHÓA
                    if (bestUnlocked != null) {
                        homingTarget = bestUnlocked;
                        lockedTargets.add(homingTarget.getUniqueId());
                    }
                    // Chấp nhận bắn bồi nếu xung quanh chỉ có mục tiêu đã bị khóa
                    else if (bestLocked != null) {
                        homingTarget = bestLocked;
                    }
                }

                // 2. BẺ LÁI NẾU BẮT ĐƯỢC TÍN HIỆU (HOMING)
                if (homingTarget != null) {
                    org.bukkit.Location targetEye = homingTarget.getLocation().add(0, homingTarget.getHeight() / 2.0, 0);
                    org.bukkit.util.Vector toTarget = targetEye.toVector().subtract(currentLoc.toVector()).normalize().multiply(0.85);
                    // Hòa trộn Vector (25% bẻ lái) để bẻ mượt mà
                    direction = direction.multiply(0.75).add(toTarget.multiply(0.25)).normalize().multiply(0.85);
                }

                currentLoc.add(direction);

                // 3. VẼ HẠT NỐT NHẠC VỚI QUỸ ĐẠO LƯỢN SÓNG (SINE WAVE)
                if (ticksLived % 2 == 0) {
                    org.bukkit.util.Vector right = direction.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
                    if (right.lengthSquared() == 0) {
                        right = direction.clone().crossProduct(new org.bukkit.util.Vector(1, 0, 0)).normalize();
                    }

                    // Biên độ nảy sóng âm (0.4)
                    double waveOffset = Math.sin(ticksLived * 0.4) * 0.4;
                    org.bukkit.Location visualLoc = currentLoc.clone().add(right.multiply(waveOffset));

                    player.getWorld().spawnParticle(Particle.NOTE, visualLoc.clone().add(0, -0.4, 0), 0, noteColor, 0, 0, 1);
                }

                // 4. KIỂM TRA VA CHẠM (Hitbox 0.8)
                for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                        LivingEntity hitTarget = (LivingEntity) e;
                        processOnHitEffects(player, rpgPlayer, hitTarget, dmgMulti, slowPercent, hasAoE, currentLoc);
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void processOnHitEffects(Player player, RPGPlayer rpgPlayer, LivingEntity primaryTarget, double dmgMulti, double slowPercent, boolean hasAoE, org.bukkit.Location hitLoc) {
        // [SÁT THƯƠNG GỐC] Lên mục tiêu chính
        double mainDmg = CombatManager.calculateDamage(player, rpgPlayer, primaryTarget, dmgMulti, 0.0, true, false);
        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
        CombatManager.applySkillDamage(player, primaryTarget, mainDmg);
        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

        // [ÂM TRẦM] Làm chậm mục tiêu chính
        if (slowPercent > 0) {
            vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(primaryTarget, 40, slowPercent, 1); // 40 ticks = 2 giây
        }

        player.getWorld().playSound(hitLoc, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, hitLoc, 10, 0.2, 0.2, 0.2, 0.1);

        // [ÂM CAO] Kích hoạt nổ AoE 3x3
        if (hasAoE) {
            player.getWorld().spawnParticle(Particle.SONIC_BOOM, hitLoc, 1);

            for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 1.5, 1.5, 1.5)) {
                if (e instanceof LivingEntity && !e.equals(player) && !e.equals(primaryTarget) && CombatManager.canTarget(player, e)) {
                    LivingEntity secondaryTarget = (LivingEntity) e;

                    // Nạn nhân phụ nhận 50% Sát thương
                    double secDmg = CombatManager.calculateDamage(player, rpgPlayer, secondaryTarget, dmgMulti * 0.5, 0.0, true, false);
                    player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, secondaryTarget, secDmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                    // Nạn nhân phụ cũng chịu làm chậm nếu có Âm Trầm
                    if (slowPercent > 0) {
                        vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(secondaryTarget, 40, slowPercent, 1);
                    }
                }
            }
        }
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