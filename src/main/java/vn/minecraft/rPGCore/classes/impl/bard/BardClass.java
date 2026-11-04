package vn.minecraft.rPGCore.classes.impl.bard;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
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

    // ==========================================
    // 1. FIX LỖI NUỐT ĐÒN HITBOX: ĐIỀU HƯỚNG VỀ ONLEFTCLICK
    // ==========================================
    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, org.bukkit.entity.LivingEntity target, org.bukkit.inventory.ItemStack weapon) {
        // Khi chém thẳng vào quái, mượn luồng onLeftClick để kích hoạt Skill Nhạc Trưởng / Đòn lượn sóng!
        this.onLeftClick(attacker, rpgAttacker, weapon);
        return false; // Trả về false để hủy sát thương vật lý Vanilla
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
        long attackCooldown = (long) (800 / (1.0 + (totalAtkSpeedBonus / 100.0)));
        attackCooldown = Math.max(200, attackCooldown);
        nextAttackTime.put(player.getUniqueId(), currentTime + attackCooldown);

        // Bắn đạn lượn sóng (Đoạn này đã có sẵn code nhận +150% dmg từ Âm Cao)
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
                highCount++; // Chỉ đếm, không làm gì cả
            } else if (executeTone == null) {
                executeTone = t; // Chọn Âm Vang/Sắc/Trầm đầu tiên tìm thấy làm đạn
            }
        }

        // Nếu CÓ đạn (Vang/Sắc/Trầm)
        if (executeTone != null) {
            // [QUAN TRỌNG]: TIÊU HAO ĐÚNG 1 ÂM (VANG/SẮC/TRẦM) ĐÓ
            vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager.consumeTone(player, rpgPlayer, executeTone);

            // Tính Buff từ các Âm Cao đang có trong túi (Không bị tiêu hao)
            double damageMultiplier = 1.0 + (highCount * 1.5);

            switch (executeTone) {
                case ECHO: executeEchoRing(player, rpgPlayer, damageMultiplier); break;
                case SHARP: executeSharpBeam(player, rpgPlayer, damageMultiplier); break;
                case LOW: executeLowBlackHole(player, rpgPlayer, damageMultiplier); break;
            }
            return true; // Bắn đạn đặc biệt thành công
        }

        // Nếu KHÔNG có đạn đặc biệt (Túi rỗng hoặc túi CHỈ TOÀN ÂM CAO)
        return false;
    }

    // ========================================================
    // 4. BỘ 4 HÀM VẬT LÝ MAESTRO BẠN CẦN THÊM VÀO CUỐI BARDCLASS
    // ========================================================
    private void executeHighSonicStrike(Player player, RPGPlayer rpgPlayer, double dmgMulti) {
        org.bukkit.Location startLoc = player.getEyeLocation().clone().add(0, -0.2, 0);
        org.bukkit.util.Vector dir = startLoc.getDirection().normalize().multiply(1.5);
        player.getWorld().playSound(startLoc, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);

        new org.bukkit.scheduler.BukkitRunnable() {
            org.bukkit.Location currentLoc = startLoc.clone();
            int ticksLived = 0;

            @Override
            public void run() {
                if (ticksLived++ > 12 || currentLoc.getBlock().getType().isSolid()) {
                    this.cancel(); return;
                }
                currentLoc.add(dir);
                player.getWorld().spawnParticle(org.bukkit.Particle.NOTE, currentLoc, 2, 0.1, 0.1, 0.1, 0);

                for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (e instanceof org.bukkit.entity.LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                        double dmg = CombatManager.calculateDamage(player, rpgPlayer, (org.bukkit.entity.LivingEntity) e, dmgMulti, 0.0, true, false);
                        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(player, (org.bukkit.entity.LivingEntity) e, dmg);
                        player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                        player.getWorld().playSound(currentLoc, org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
                        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, currentLoc, 10, 0.2, 0.2, 0.2, 0.1);
                        this.cancel(); return;
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void executeEchoRing(Player player, RPGPlayer rpgPlayer, double dmgMulti) {
        org.bukkit.Location center = player.getEyeLocation().clone().add(0, -0.3, 0);
        center.getWorld().playSound(center, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.8f);

        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            org.bukkit.util.Vector dir = new org.bukkit.util.Vector(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(1.6);
            new org.bukkit.scheduler.BukkitRunnable() {
                org.bukkit.Location currentLoc = center.clone();
                int ticksLived = 0;
                @Override
                public void run() {
                    if (ticksLived++ > 10 || currentLoc.getBlock().getType().isSolid()) { this.cancel(); return; }
                    currentLoc.add(dir);
                    player.getWorld().spawnParticle(org.bukkit.Particle.NOTE, currentLoc, 1, 0, 0, 0, 0);

                    for (org.bukkit.entity.Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                        if (e instanceof org.bukkit.entity.LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            double dmg = CombatManager.calculateDamage(player, rpgPlayer, (org.bukkit.entity.LivingEntity) e, dmgMulti, 0.0, true, false);
                            player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(player, (org.bukkit.entity.LivingEntity) e, dmg);
                            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            player.getWorld().playSound(currentLoc, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
                            this.cancel(); return;
                        }
                    }
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
        }
    }

    private void executeSharpBeam(Player player, RPGPlayer rpgPlayer, double dmgMulti) {
        org.bukkit.Location pLoc = player.getEyeLocation();
        org.bukkit.util.Vector dir = pLoc.getDirection().normalize().multiply(0.6);
        player.getWorld().playSound(pLoc, org.bukkit.Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 2.0f);

        for (double i = 0; i < 15.0; i += 0.6) {
            pLoc.add(dir);
            pLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, pLoc, 2, new org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 1.2f));
            if (pLoc.getBlock().getType().isSolid()) break;

            for (org.bukkit.entity.Entity e : pLoc.getWorld().getNearbyEntities(pLoc, 1.2, 1.2, 1.2)) {
                if (e instanceof org.bukkit.entity.LivingEntity && !e.equals(player) && !e.hasMetadata("Laser_Hit") && CombatManager.canTarget(player, e)) {
                    e.setMetadata("Laser_Hit", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    double dmg = CombatManager.calculateDamage(player, rpgPlayer, (org.bukkit.entity.LivingEntity) e, dmgMulti, 0.0, true, false);
                    player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(player, (org.bukkit.entity.LivingEntity) e, dmg);
                    player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override public void run() { e.removeMetadata("Laser_Hit", RPGCore.getInstance()); }
                    }.runTaskLater(RPGCore.getInstance(), 3L);
                }
            }
        }
    }

    private void executeLowBlackHole(Player player, RPGPlayer rpgPlayer, double dmgMulti) {
        org.bukkit.util.RayTraceResult rayTrace = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0,
                e -> e instanceof org.bukkit.entity.LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e));

        org.bukkit.Location center;
        if (rayTrace != null && rayTrace.getHitEntity() != null) {
            center = rayTrace.getHitEntity().getLocation();
        } else {
            org.bukkit.util.RayTraceResult blockTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0);
            center = (blockTrace != null && blockTrace.getHitBlock() != null) ? blockTrace.getHitBlock().getLocation().add(0, 1, 0) : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(10));
        }

        center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.6f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 20) { this.cancel(); return; }

                for (int i = 0; i < 360; i += 45) {
                    double angle = Math.toRadians(i + (ticks * 12));
                    double x = Math.cos(angle) * 2.5;
                    double z = Math.sin(angle) * 2.5;
                    center.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, center.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                }

                for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 5, 3, 5)) {
                    if (e instanceof org.bukkit.entity.LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e;
                        org.bukkit.util.Vector pull = center.toVector().subtract(victim.getLocation().toVector());
                        if (pull.lengthSquared() > 0.01) victim.setVelocity(pull.normalize().multiply(0.5));

                        if (ticks == 1 || ticks % 10 == 0) {
                            StatusEffectManager.applyAbsoluteRoot(victim, 10);
                            double dmg = CombatManager.calculateDamage(player, rpgPlayer, victim, dmgMulti, 0.0, true, false);
                            player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(player, victim, dmg);
                            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
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
        int projCount = Math.min(4, echoCount + 1);

        double baseDmgMulti = 1.0;
        if (projCount == 2) baseDmgMulti = 0.50;
        else if (projCount == 3) baseDmgMulti = 0.33;
        else if (projCount == 4) baseDmgMulti = 0.30;

        double finalDmgMulti = baseDmgMulti + (sharpCount * 0.10);

        // ==========================================
        // [CƠ CHẾ MỚI]: BƠM SÁT THƯƠNG TỪ ÂM CAO KHI ĐANG LÀ NHẠC TRƯỞNG
        // Nếu đang bật Ulti, mỗi Âm Cao cộng thêm 150% Sát thương đòn đánh
        // ==========================================
        if (player.hasMetadata(vn.minecraft.rPGCore.skills.bard.MaestroState.ACTIVE_META)) {
            finalDmgMulti += (highCount * 1.5);
        }

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
        else spreadAngles = new double[]{-20, -7, 7, 20};

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
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        boolean isAdmin = player.hasPermission("rpgcore.admin");

        if (unspent > 0 || isAdmin) {
            vn.minecraft.rPGCore.skills.bard.BardCoreGUI.openSelectionMenu(player, rpgPlayer);
            return true;
        }

        // Bấm F nhưng không đủ điểm -> Gọi Prompt Preview
        vn.minecraft.rPGCore.skills.bard.BardCoreGUI.sendPreviewPrompt(player);
        return true;
    }
}