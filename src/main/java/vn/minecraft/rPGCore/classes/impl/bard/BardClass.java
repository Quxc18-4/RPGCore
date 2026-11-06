package vn.minecraft.rPGCore.classes.impl.bard;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                if (attacker.hasMetadata(META_GLOOM_DEBUFF)) return -0.10;
                return 0.0;
            }

            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                if (victim.hasMetadata(META_DISSONANCE_DEBUFF)) return -0.15;
                return 0.0;
            }
        });
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        Player player = rpgPlayer.getBukkitPlayer();

        // 1. TIẾN TRÌNH VI MÔ: Bơm % Giảm hồi chiêu (CDR)
//        if (player.hasMetadata("BARD_BONUS_CDR")) {
//            double bonusCdr = player.getMetadata("BARD_BONUS_CDR").get(0).asDouble();
//            stats.put("Giảm hồi chiêu", stats.getOrDefault("Giảm hồi chiêu", 0.0) + bonusCdr);
//        }

        // 2. LÕI 5: BỘI ÂM (OVERTONE) - Cấp MS và Kháng Hiệu Ứng động theo số Âm đang giữ
        if (player.hasMetadata("BARD_OVERTONE_ACTIVE") && player.hasMetadata("BARD_TONE_COUNT")) {
            int toneCount = player.getMetadata("BARD_TONE_COUNT").get(0).asInt();
            if (toneCount > 0) {
                stats.put("Tốc độ di chuyển", stats.getOrDefault("Tốc độ di chuyển", 0.0) + (20.0 * toneCount));
                stats.put("Kháng hiệu ứng", stats.getOrDefault("Kháng hiệu ứng", 0.0) + (15.0 * toneCount));
            }
        }

        // ÂM SẮC (SHARP TONE) - Xuyên Thấu
        if (player.hasMetadata("BARD_SHARP_ATTACK")) {
            stats.put("Tỉ lệ xuyên giáp", stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + 10.0);
            stats.put("Tỉ lệ xuyên kháng phép", stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + 10.0);
        }

        // GIAI ĐIỆU DỒN DẬP (UPBEAT)
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
        this.onLeftClick(attacker, rpgAttacker, weapon);
        return false;
    }

    // ==========================================
    // 2. PHÂN LUỒNG 1 CỬA (SINGLE ENTRY POINT)
    // ==========================================
    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, org.bukkit.inventory.ItemStack weapon) {
        boolean isMaestro = player.hasMetadata(vn.minecraft.rPGCore.skills.bard.MaestroState.ACTIVE_META);

        if (isMaestro) {
            // Nếu là Nhạc Trưởng, thử dùng Âm đặc biệt (Vang/Sắc/Trầm) làm đạn.
            // Nếu bắn thành công -> Bỏ qua hoàn toàn Cooldown tốc đánh (Vì đạn 4.5s mới có 1 viên)
            if (tryExecuteMaestroSpecialAttack(player, rpgPlayer)) {
                return true;
            }
        }

        // TRẠNG THÁI BÌNH THƯỜNG (Hoặc Nhạc Trưởng nhưng hết đạn Vang/Sắc/Trầm)
        // -> ÁP DỤNG COOLDOWN TỐC ĐÁNH CHO ĐÒN LƯỢN SÓNG CƠ BẢN
        long currentTime = System.currentTimeMillis();
        long allowedTime = nextAttackTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return false;

        double totalAtkSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long attackCooldown = (long) (1200 / (1.0 + (totalAtkSpeedBonus / 100.0)));
        attackCooldown = Math.max(200, attackCooldown);
        nextAttackTime.put(player.getUniqueId(), currentTime + attackCooldown);

        fireAdaptiveChords(player, rpgPlayer);
        return true;
    }

    // ==========================================
    // 3. LOGIC MAESTRO: CHỈ TIÊU HAO ĐẠN (VANG/SẮC/TRẦM)
    // ==========================================
    private boolean tryExecuteMaestroSpecialAttack(Player player, RPGPlayer rpgPlayer) {
        if (player.hasMetadata("Ghost_L_Blocker")) return false;

        java.util.List<vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType> tones = vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager.getCurrentTones(player);
        if (tones.isEmpty()) return false;

        int highCount = 0;
        vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType executeTone = null;

        // Đếm số lượng Âm Cao (HIGH) để tính Buff, và tìm 1 Âm làm đạn
        for (vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType t : tones) {
            if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.HIGH) {
                highCount++;
            } else if (executeTone == null) {
                executeTone = t;
            }
        }

        if (executeTone != null) {
            vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager.consumeTone(player, rpgPlayer, executeTone);

            double damageMultiplier = 1.0 + (highCount * 1.5);

            switch (executeTone) {
                case ECHO: executeEchoTone(player, rpgPlayer, damageMultiplier); break;
                case SHARP: executeSharpTone(player, rpgPlayer, damageMultiplier); break;
                case LOW: executeLowTone(player, rpgPlayer, damageMultiplier); break;
            }
            return true;
        }

        return false;
    }

    // ========================================================
    // [CẬP NHẬT 1]: ÂM TRẦM (LOW TONE) - LỰU ĐẠN BÃI LÀM CHẬM KHỔNG LỒ
    // ========================================================
    private void executeLowTone(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        final Location startLoc = player.getEyeLocation();
        final Vector direction = startLoc.getDirection().normalize();

        // Cân bằng: Sát thương tick rất nhỏ vì mang tính Khống chế (CC Tax)
        final double tickDmgMulti = baseMulti * 0.20;

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int distance = 0;

            @Override
            public void run() {
                if (distance > 20 || currentLoc.getBlock().getType().isSolid()) {
                    triggerLowTonePuddle(player, rpgPlayer, currentLoc, tickDmgMulti);
                    this.cancel();
                    return;
                }

                currentLoc.add(direction);
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 2, new Particle.DustOptions(Color.PURPLE, 1.2f));

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        triggerLowTonePuddle(player, rpgPlayer, currentLoc, tickDmgMulti);
                        this.cancel();
                        return;
                    }
                }
                distance++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void triggerLowTonePuddle(Player player, RPGPlayer rpgPlayer, Location center, double dmgMulti) {
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        final double radius = 4.0;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 60 || !player.isOnline()) { // Tồn tại 3s = 60 ticks
                    this.cancel();
                    return;
                }

                if (ticks % 2 == 0) {
                    double angle = (ticks * Math.PI / 10.0);
                    double shrinkRadius = radius - ((ticks % 20) / 20.0) * radius;

                    double x = Math.cos(angle) * shrinkRadius;
                    double z = Math.sin(angle) * shrinkRadius;

                    Location noteLoc = center.clone().add(x, 0.2, z);
                    center.getWorld().spawnParticle(Particle.NOTE, noteLoc, 0, 0.1, 0, 0, 1);
                    center.getWorld().spawnParticle(Particle.WITCH, noteLoc, 1, 0, 0, 0, 0);
                }

                // Logic: Quét quái mỗi 10 ticks (0.5s) để Slow và giật DMG
                if (ticks % 10 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, 1.5, radius)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;

                            // [SỬA ĐỔI]: SỬ DỤNG HÀM APPLYLOW CHUẨN CỦA STATUS_EFFECT_MANAGER
                            // Thời gian 20 ticks (1s) | Giảm 100% tốc độ Player (1.0) | Slowness 5 cho Quái (Amp 4)
                            vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(target, 20, 1.0, 4);

                            // Gây sát thương chuẩn theo rule
                            double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 0.0, true, false);
                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(player, target, dmg);
                            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                        }
                    }
                }
               ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ========================================================
    // [CẬP NHẬT 2]: ÂM SẮC (SHARP TONE) - CHARGE LASER VÀ BẮN HELIX
    // ========================================================
    private void executeSharpTone(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        final double burstMulti = baseMulti * 3.0; // Burst x3 Sát thương
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

        new BukkitRunnable() {
            int chargeTicks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel(); return;
                }

                // Vận chiêu 2s
                if (chargeTicks < 40) {
                    Location eye = player.getEyeLocation();
                    Vector dir = eye.getDirection().normalize();
                    for (double i = 1; i <= 15; i += 1.0) {
                        player.getWorld().spawnParticle(Particle.DUST, eye.clone().add(dir.clone().multiply(i)), 1, new Particle.DustOptions(Color.RED, 0.8f));
                    }
                    if (chargeTicks % 10 == 0) player.getWorld().playSound(eye, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.5f, 2.0f);
                }
                else if (chargeTicks == 40) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.2f);

                    // Giật lùi người chơi
                    player.setVelocity(player.getLocation().getDirection().multiply(-1.5).setY(0.4));

                    fireHelixBeam(player, rpgPlayer, burstMulti);
                    this.cancel();
                }
                chargeTicks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void fireHelixBeam(Player player, RPGPlayer rpgPlayer, double dmgMulti) {
        Location startLoc = player.getEyeLocation();
        Vector dir = startLoc.getDirection().normalize();
        List<UUID> hitTargets = new ArrayList<>();

        for (double d = 0; d <= 15; d += 0.5) {
            Location point = startLoc.clone().add(dir.clone().multiply(d));

            double r = 2.0;
            double angle = d * Math.PI;

            double x1 = Math.cos(angle) * r;
            double y1 = Math.sin(angle) * r;
            Vector v1 = getPerpendicularVectors(dir, x1, y1);
            point.getWorld().spawnParticle(Particle.DUST, point.clone().add(v1), 2, new Particle.DustOptions(Color.ORANGE, 1.5f));

            double x2 = Math.cos(angle + Math.PI) * r;
            double y2 = Math.sin(angle + Math.PI) * r;
            Vector v2 = getPerpendicularVectors(dir, x2, y2);
            point.getWorld().spawnParticle(Particle.END_ROD, point.clone().add(v2), 1, 0, 0, 0, 0);

            for (Entity e : point.getWorld().getNearbyEntities(point, 2.0, 2.0, 2.0)) {
                if (e instanceof LivingEntity && CombatManager.canTarget(player, e) && !hitTargets.contains(e.getUniqueId())) {
                    hitTargets.add(e.getUniqueId());

                    double dmg = CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) e, dmgMulti, 15.0, true, false);
                    player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, (LivingEntity) e, dmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                    point.getWorld().spawnParticle(Particle.ENCHANTED_HIT, e.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }

    // ========================================================
    // [CẬP NHẬT 3]: ÂM VANG (ECHO TONE) - LỰU ĐẠN SÓNG ÂM
    // ========================================================
    private void executeEchoTone(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        final Location startLoc = player.getEyeLocation();
        final Vector velocity = startLoc.getDirection().normalize().multiply(1.2);
        final Vector gravity = new Vector(0, -0.06, 0);

        new BukkitRunnable() {
            Location nadeLoc = startLoc.clone();
            int ticks = 0;
            boolean isOnGround = false;

            @Override
            public void run() {
                if (ticks >= 60 || !player.isOnline()) {
                    explodeEchoTone(player, rpgPlayer, nadeLoc, baseMulti, isOnGround);
                    this.cancel(); return;
                }

                if (!isOnGround) {
                    nadeLoc.add(velocity);
                    velocity.add(gravity);
                    if (nadeLoc.getBlock().getType().isSolid()) {
                        isOnGround = true;
                        nadeLoc.setY(nadeLoc.getBlockY() + 1.2);
                    }
                }

                nadeLoc.getWorld().spawnParticle(Particle.NOTE, nadeLoc, 0, (ticks % 24) / 24.0, 0, 0, 1);
                if (ticks % 5 == 0) nadeLoc.getWorld().spawnParticle(Particle.DUST, nadeLoc, 1, new Particle.DustOptions(Color.BLUE, 1.2f));
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void explodeEchoTone(Player player, RPGPlayer rpgPlayer, Location explodeLoc, double baseMulti, boolean isGround) {
        explodeLoc.getWorld().playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
        explodeLoc.getWorld().playSound(explodeLoc, Sound.BLOCK_BELL_RESONATE, 2.0f, 0.8f);

        int noteCount = 40;
        List<Location> noteLocs = new ArrayList<>();
        List<Vector> noteDirs = new ArrayList<>();
        Map<UUID, Integer> hitMap = new HashMap<>();

        for (int i = 0; i < noteCount; i++) {
            noteLocs.add(explodeLoc.clone());
            double u = Math.random();
            double v = Math.random();

            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);

            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);

            // Chỉ nổ bán cầu nếu rớt xuống đất
            if (isGround && y < 0) y = Math.abs(y);
            noteDirs.add(new Vector(x, y, z).normalize().multiply(0.8));
        }

        // Tối ưu chạy gom tất cả tia đạn vào 1 BukkitRunnable
        new BukkitRunnable() {
            int travelTicks = 0;
            @Override
            public void run() {
                if (travelTicks > 10) {
                    this.cancel(); return;
                }

                for (int i = 0; i < noteLocs.size(); i++) {
                    Location loc = noteLocs.get(i);
                    Vector dir = noteDirs.get(i);

                    loc.add(dir);
                    loc.getWorld().spawnParticle(Particle.NOTE, loc, 0, Math.random(), 0, 0, 1);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 1, 0, 0, 0, 0);

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.8, 0.8, 0.8)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                            UUID targetID = e.getUniqueId();
                            int hits = hitMap.getOrDefault(targetID, 0);

                            // Damage Falloff: Trúng phát đầu = 100%, phát sau = +5%
                            double currentMulti = (hits == 0) ? baseMulti : (baseMulti * 0.05);

                            double dmg = CombatManager.calculateDamage(player, rpgPlayer, (LivingEntity) e, currentMulti, 0.0, true, false);
                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(player, (LivingEntity) e, dmg);
                            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            hitMap.put(targetID, hits + 1);
                            dir.setY(999); // Hủy quỹ đạo hạt này
                        }
                    }
                }
                travelTicks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ========================================================
    // TOÁN HỌC TRỢ GIÚP (HELPER: VECTOR MATH)
    // ========================================================
    private Vector getPerpendicularVectors(Vector direction, double xOff, double yOff) {
        Vector dir = direction.clone().normalize();
        Vector up = new Vector(0, 1, 0);
        if (Math.abs(dir.getY()) > 0.99) up = new Vector(1, 0, 0);

        Vector right = dir.clone().crossProduct(up).normalize();
        Vector realUp = right.clone().crossProduct(dir).normalize();

        return right.multiply(xOff).add(realUp.multiply(yOff));
    }

    private void fireAdaptiveChords(Player player, RPGPlayer rpgPlayer) {
        java.util.List<vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType> tones =
                vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager.getCurrentTones(player);

        int sharpCount = 0, lowCount = 0, echoCount = 0, highCount = 0;
        for (vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType t : tones) {
            if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.SHARP) sharpCount++;
            else if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.LOW) lowCount++;
            else if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.ECHO) echoCount++;
            else if (t == vn.minecraft.rPGCore.classes.impl.bard.tones.ToneType.HIGH) highCount++;
        }

        int projCount = Math.min(4, echoCount + 1);
        double baseDmgMulti = 1.0;
        if (projCount == 2) baseDmgMulti = 0.50;
        else if (projCount == 3) baseDmgMulti = 0.33;
        else if (projCount == 4) baseDmgMulti = 0.30;

        double finalDmgMulti = baseDmgMulti + (sharpCount * 0.10);

        if (player.hasMetadata(vn.minecraft.rPGCore.skills.bard.MaestroState.ACTIVE_META)) {
            finalDmgMulti += (highCount * 1.5);
        }

        double slowPercent = lowCount * 0.15;
        boolean hasAoE = highCount > 0;

        org.bukkit.Sound instrument = BARD_INSTRUMENTS[random.nextInt(BARD_INSTRUMENTS.length)];
        player.getWorld().playSound(player.getLocation(), instrument, 1.0f, 1.5f);

        double[] spreadAngles;
        if (projCount == 1) spreadAngles = new double[]{0};
        else if (projCount == 2) spreadAngles = new double[]{-12, 12};
        else if (projCount == 3) spreadAngles = new double[]{-15, 0, 15};
        else spreadAngles = new double[]{-20, -7, 7, 20};

        java.util.Set<java.util.UUID> lockedTargets = new java.util.HashSet<>();

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

            {
                double radians = Math.toRadians(yawOffsetDegrees);
                double cos = Math.cos(radians);
                double sin = Math.sin(radians);
                double x = direction.getX() * cos - direction.getZ() * sin;
                double z = direction.getX() * sin + direction.getZ() * cos;
                direction.setX(x).setZ(z).normalize().multiply(0.85);
            }

            @Override
            public void run() {
                if (ticksLived++ > 25 || currentLoc.getBlock().getType().isSolid() || !player.isOnline()) {
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 3, 0.1, 0.1, 0.1, 0.05);
                    this.cancel();
                    return;
                }

                if (homingTarget == null || !homingTarget.isValid() || homingTarget.isDead()) {
                    double closestUnlockedDist = 12.25;
                    double closestLockedDist = 12.25;
                    org.bukkit.entity.LivingEntity bestUnlocked = null;
                    org.bukkit.entity.LivingEntity bestLocked = null;

                    for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 3.5, 3.5, 3.5)) {
                        if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            double d = e.getLocation().distanceSquared(currentLoc);
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

                    if (bestUnlocked != null) {
                        homingTarget = bestUnlocked;
                        lockedTargets.add(homingTarget.getUniqueId());
                    } else if (bestLocked != null) {
                        homingTarget = bestLocked;
                    }
                }

                if (homingTarget != null) {
                    org.bukkit.Location targetEye = homingTarget.getLocation().add(0, homingTarget.getHeight() / 2.0, 0);
                    org.bukkit.util.Vector toTarget = targetEye.toVector().subtract(currentLoc.toVector()).normalize().multiply(0.85);
                    direction = direction.multiply(0.75).add(toTarget.multiply(0.25)).normalize().multiply(0.85);
                }

                currentLoc.add(direction);

                if (ticksLived % 2 == 0) {
                    org.bukkit.util.Vector right = direction.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
                    if (right.lengthSquared() == 0) right = direction.clone().crossProduct(new org.bukkit.util.Vector(1, 0, 0)).normalize();
                    double waveOffset = Math.sin(ticksLived * 0.4) * 0.4;
                    org.bukkit.Location visualLoc = currentLoc.clone().add(right.multiply(waveOffset));
                    player.getWorld().spawnParticle(Particle.NOTE, visualLoc.clone().add(0, -0.4, 0), 0, noteColor, 0, 0, 1);
                }

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
        double mainDmg = CombatManager.calculateDamage(player, rpgPlayer, primaryTarget, dmgMulti, 0.0, true, false);
        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
        CombatManager.applySkillDamage(player, primaryTarget, mainDmg);
        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

        if (slowPercent > 0) {
            vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(primaryTarget, 40, slowPercent, 1);
        }

        player.getWorld().playSound(hitLoc, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, hitLoc, 10, 0.2, 0.2, 0.2, 0.1);

        if (hasAoE) {
            player.getWorld().spawnParticle(Particle.SONIC_BOOM, hitLoc, 1);
            for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 1.5, 1.5, 1.5)) {
                if (e instanceof LivingEntity && !e.equals(player) && !e.equals(primaryTarget) && CombatManager.canTarget(player, e)) {
                    LivingEntity secondaryTarget = (LivingEntity) e;
                    double secDmg = CombatManager.calculateDamage(player, rpgPlayer, secondaryTarget, dmgMulti * 0.5, 0.0, true, false);
                    player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, secondaryTarget, secDmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                    if (slowPercent > 0) {
                        vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(secondaryTarget, 40, slowPercent, 1);
                    }
                }
            }
        }
    }

    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, org.bukkit.inventory.ItemStack weapon) {
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        boolean isAdmin = player.hasPermission("rpgcore.admin");

        if (unspent > 0 || isAdmin) {
            vn.minecraft.rPGCore.skills.bard.BardCoreGUI.openSelectionMenu(player, rpgPlayer);
            return true;
        }

        vn.minecraft.rPGCore.skills.bard.BardCoreGUI.sendPreviewPrompt(player);
        return true;
    }
}