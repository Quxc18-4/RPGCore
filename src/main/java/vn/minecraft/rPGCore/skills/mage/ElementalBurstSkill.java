package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ElementalBurstSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ElementalBurstSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_elemental_burst"; }
    @Override public String getName() { return "Bạo Phát Nguyên Tố"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.BLAZE_POWDER; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 15; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public double getManaCost(int level) {
        // Giảm 20 Mana: 80 -> 60
        return 60.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        // Hồi chiêu: 14s ở Lv1 -> 11s ở Lv10
        double cd = 14.0 - (level * 0.3);
        return (long) (cd * 1000);
    }

    // Sát thương chuẩn (Lv1: 240%, Lv10: 330%)
    private double getBaseDamageMulti(int level) {
        return 2.4 + ((level - 1) * 0.1);
    }

    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);
        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        int baseDmgPct = (int) (getBaseDamageMulti(displayLevel) * 100);

        lore.add("§7Giải phóng toàn bộ ma lực dư thừa, tạo");
        lore.add("§7ra một vụ Bạo phát xung quanh bản thân.");
        lore.add("§7Kỹ năng tối ưu để phá vỡ vòng vây.");
        lore.add("");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");

        switch (element) {
            case FIRE:
                lore.add("§c[Thi Triển - Liệt Hỏa Trùng m]");
                lore.add("§f- Triệu hồi 9 cột lửa phun trào hình quạt.");
                lore.add("§f- Cột đầu trúng mục tiêu gây §c" + baseDmgPct + "% Sát Thương§f,");
                lore.add("§f  các cột sau chạm cùng mục tiêu chỉ gây §c15% Sát Thương§f.");
                lore.add("§f- Hất văng và có 50% tỷ lệ §6Thiêu Đốt§f.");
                break;
            case WATER:
                lore.add("§b[Thi Triển - Sóng Dữ]");
                lore.add("§f- Đẩy một bức tường nước dài 4m về phía trước.");
                lore.add("§f- Cuốn trôi, đẩy lùi mọi kẻ địch và làm chậm 30%.");
                lore.add("§f- Gây §c" + (int)(baseDmgPct * 1.2) + "% Sát Thương§f.");
                break;
            case WIND:
                // Tính toán ép kiểu Int và làm tròn để hiện lên Lore thật đẹp
                int speedBonus = (int) Math.round(15.0 + ((displayLevel - 1) * (20.0 / 9.0)));
                lore.add("§a[Thi Triển - Phong Nhẫn Trảm]");
                lore.add("§f- Bắn 8 lưỡi đao gió xuyên thấu 360 độ.");
                lore.add("§f- Gây §c" + baseDmgPct + "% ST§f cho mục tiêu.");
                lore.add("§f- Tăng §a" + speedBonus + "% Tốc độ di chuyển§f trong 3s.");
                break;
            case NATURE:
                lore.add("§2[Thi Triển - Bão Lá Ma Thuật]");
                lore.add("§f- Tạo ra cơn bão lá di chuyển thẳng về phía trước.");
                lore.add("§f- Khóa khả năng bật nhảy của kẻ địch bên trong.");
                lore.add("§f- Cú nổ đầu gây §c" + (int)(baseDmgPct * 0.8) + "% Sát Thương§f, sau đó");
                lore.add("§f  gây §c" + (int)(baseDmgPct * 0.2) + "% Sát Thương§f liên tục mỗi giây.");
                break;
            case LIGHT:
                lore.add("§e[Thi Triển - Quang Phổ Chói Lòa]");
                lore.add("§f- Bùng nổ ánh sáng diện rộng tại bản thân.");
                lore.add("§f- Gây §c" + (int)(baseDmgPct * 0.7) + "% Sát Thương§f.");
                lore.add("§f- Làm mù và choáng hoàn toàn kẻ địch trong §a" + String.format("%.1f", 1.5 + (displayLevel * 0.1)) + "s§f.");
                break;
            case DARK:
                lore.add("§5[Thi Triển - Khe Nứt Diệt Vong]");
                lore.add("§f- Mở khe nứt quét tia laser hình quạt về phía trước.");
                lore.add("§f- Gây §c" + (int)(baseDmgPct * 1.2) + "% Sát Thương§f, kèm hiệu ứng §8Mù Lòa§f");
                lore.add("§f  và §cĂn Mòn §ftrong 4s.");
                break;
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;
        applyGlobalCooldown(player, rpgPlayer, level);

        MageElement element = ElementalSwapSkill.getActiveElement(player);
        double baseMulti = getBaseDamageMulti(level);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);

        switch (element) {
            case FIRE: triggerFireBurst(player, rpgPlayer, baseMulti); break;
            case WATER: triggerWaterBurst(player, rpgPlayer, baseMulti); break;
            case WIND: triggerWindBurst(player, rpgPlayer, level, baseMulti); break;
            case NATURE: triggerNatureBurst(player, rpgPlayer, baseMulti); break;
            case LIGHT: triggerLightBurst(player, rpgPlayer, level, baseMulti); break;
            case DARK: triggerDarkBurst(player, rpgPlayer, baseMulti); break;
        }

        return true;
    }

    // ==========================================
    // 1. LỬA: LIỆT HỎA TRÙNG ÂM (3 ROW x 3 COL)
    // ==========================================
    private void triggerFireBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location startLoc = player.getLocation();
        Vector forward = startLoc.getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Set<UUID> hitTargets = new HashSet<>();

        new BukkitRunnable() {
            int row = 0;
            @Override
            public void run() {
                if (row >= 3 || !player.isOnline()) { this.cancel(); return; }

                double distance = 2.0 + (row * 2.5);
                double spread = 1.5 + (row * 1.5);

                List<Location> pillarLocs = Arrays.asList(
                        startLoc.clone().add(forward.clone().multiply(distance)).add(right.clone().multiply(-spread)),
                        startLoc.clone().add(forward.clone().multiply(distance)),
                        startLoc.clone().add(forward.clone().multiply(distance)).add(right.clone().multiply(spread))
                );

                for (Location loc : pillarLocs) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);

                    // [HÀM CON]: Vẽ cột lửa mọc từ dưới lên trong 4 ticks
                    new BukkitRunnable() {
                        double currentY = 0;
                        @Override
                        public void run() {
                            if (currentY > 3.5) { this.cancel(); return; }

                            // Vẽ 2 tầng hạt mỗi lần chạy để cột trông dày dặn
                            for(int i = 0; i < 2; i++) {
                                Location partLoc = loc.clone().add(0, currentY, 0);
                                loc.getWorld().spawnParticle(Particle.FLAME, partLoc, 12, 0.2, 0.1, 0.2, 0.05, null, true);
                                // Dùng SMOKE_NORMAL để tan nhanh hơn
                                loc.getWorld().spawnParticle(Particle.SMOKE, partLoc, 3, 0.1, 0.1, 0.1, 0.02, null, true);
                                currentY += 0.4;
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L);

                    // Hitbox check ngay khi cột bắt đầu mọc
                    for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 4.0, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;

                            // Tính Multiplier gốc (Bị giảm nếu trúng nhiều cột)
                            double multi = hitTargets.contains(target.getUniqueId()) ? (baseMulti * 0.15) : baseMulti;

                            // =====================================
                            // [FIX]: GỌI REACTION ENGINE HỆ HỎA
                            // =====================================
                            vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine.ReactionResult reaction =
                                    vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine.processHit(player, target, vn.minecraft.rPGCore.classes.impl.mage.MageElement.FIRE);

                            // Nhân thêm hệ số sát thương từ phản ứng (nếu có)
                            double finalMultiToApply = multi * reaction.damageMultiplier;

                            // Truyền hệ số đã buff qua CombatManager
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalMultiToApply, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);

                            target.setVelocity(new Vector(0, 0.7, 0));
                            hitTargets.add(target.getUniqueId());
                        }
                    }
                }
                row++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // ==========================================
    // 2. NƯỚC: SÓNG THẦN XÔ NGÃ
    // ==========================================
    private void triggerWaterBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location startLoc = player.getLocation();
        Vector forward = startLoc.getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        Set<UUID> hitTargets = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 15 || !player.isOnline()) { this.cancel(); return; } // Tiến 15 block

                Location center = startLoc.clone().add(forward.clone().multiply(ticks * 0.8)); // Tốc độ sóng 0.8
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 0.8f);

                // Vẽ sóng ngang 4 block
                for (double i = -2.0; i <= 2.0; i += 0.5) {
                    Location pLoc = center.clone().add(right.clone().multiply(i));
                    pLoc.getWorld().spawnParticle(Particle.SPLASH, pLoc, 5, 0.2, 0.5, 0.2, 0.1, null, true);
                    pLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 2, 0.2, 0.5, 0.2, 0.0, null, true);
                }

                // Hitbox sóng ngang
                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 2.5, 1.5, 2.5)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;

                        // Đẩy lùi mạnh theo hướng sóng (Chạy mỗi tick để liên tục xô ngã)
                        target.setVelocity(forward.clone().multiply(0.8).setY(0.3));

                        if (!hitTargets.contains(target.getUniqueId())) {
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WATER);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * 1.2 * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);

                            // Làm chậm 30% trong 3s (Player: -30% speed, Quái: Slowness 2)
                            StatusEffectManager.applySlow(target, 60, 0.3, 1);
                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);

                            hitTargets.add(target.getUniqueId());
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 3. GIÓ: PHONG NHẪN TRẢM (NOVA)
    // ==========================================
    private void triggerWindBurst(Player player, RPGPlayer rpgPlayer, int level, double baseMulti) {
        Location center = player.getLocation().add(0, 1, 0);
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);

        // [ÉP CHỈ SỐ]: Cấp 1 = 15.0, Cấp 10 = 35.0
        double speedValue = 15.0 + ((level - 1) * (20.0 / 9.0));

        // Buff Speed nội tại
        player.setMetadata("Wind_Burst_Speed", new FixedMetadataValue(plugin, speedValue));
        new BukkitRunnable() {
            @Override
            public void run() { player.removeMetadata("Wind_Burst_Speed", plugin); }
        }.runTaskLater(plugin, 60L); // Tồn tại 3s

        Set<UUID> hitTargets = new HashSet<>();
        List<Vector> blades = new ArrayList<>();

        // Tạo 8 hướng đao gió
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            blades.add(new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(1.5));
        }

        new BukkitRunnable() {
            int ticks = 0;
            List<Location> currentLocs = new ArrayList<>();

            @Override
            public void run() {
                if (ticks == 0) {
                    for (int i = 0; i < 8; i++) currentLocs.add(center.clone());
                }

                if (ticks >= 5 || !player.isOnline()) { this.cancel(); return; } // Tầm xa ~10 block

                for (int i = 0; i < 8; i++) {
                    Location loc = currentLocs.get(i);
                    loc.add(blades.get(i));

                    loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0.0, null, true);
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.05, null, true);

                    for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            if (!hitTargets.contains(target.getUniqueId())) {
                                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WIND);
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * reaction.damageMultiplier, 0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);

                                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                                hitTargets.add(target.getUniqueId());
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 4. THIÊN NHIÊN: BÃO LÁ CUỒNG SI
    // ==========================================
    private void triggerNatureBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location startLoc = player.getLocation().add(0, 1.2, 0);
        Vector forward = player.getLocation().getDirection().setY(0).normalize();
        Set<UUID> hitTargets = new HashSet<>();

        Particle.DustOptions darkGreenLeaf = new Particle.DustOptions(org.bukkit.Color.fromRGB(34, 139, 34), 1.2f);
        Particle.DustOptions lightGreenLeaf = new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.9f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Giữ nguyên thời gian sống (3 giây), kết hợp đi chậm sẽ làm giảm tầm bắn
                if (ticks >= 60 || !player.isOnline()) { this.cancel(); return; }

                // 1. [ĐI CHẬM & GIẢM TẦM]: Giảm từ 0.3 xuống 0.18
                // Tầm xa tối đa bây giờ là ~10.8 block (thay vì 18 block như cũ)
                Location center = startLoc.clone().add(forward.clone().multiply(ticks * 0.18));

                for (int i = 0; i < 3; i++) {
                    double angle = (ticks * 0.8) + (i * (Math.PI * 2 / 3));

                    // 2. [TĂNG BỀ RỘNG]: Tăng bán kính xoắn ốc từ 1.2 lên 2.2
                    double radius = 2.2;

                    Vector relative = new Vector(Math.cos(angle) * radius, Math.sin(angle * 0.5) * 0.5, Math.sin(angle) * radius);
                    Location pLoc = center.clone().add(relative);

                    Particle.DustOptions currentDust = (i % 2 == 0) ? darkGreenLeaf : lightGreenLeaf;

                    // Tăng số lượng DUST từ 3 lên 5 để bão không bị "thưa" khi phình to
                    center.getWorld().spawnParticle(Particle.DUST, pLoc, 5, 0.2, 0.2, 0.2, 0.0, currentDust);
                    center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pLoc, 1, 0, 0, 0, 0);
                }

                // Lõi bão và tàn dư
                center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 15, 1.5, 1.0, 1.5, 0.01);
                if (ticks % 3 == 0) {
                    center.getWorld().spawnParticle(Particle.CHERRY_LEAVES, center, 3, 1.5, 1.5, 1.5, 0.0);
                }

                if (ticks % 5 == 0) {
                    center.getWorld().playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.4f, 1.2f); // Giảm pitch nhẹ để tiếng gió nghe "nặng" hơn
                }

                // 3. [MỞ RỘNG HITBOX]: Quét rộng ra 4.5 block (cũ là 3.5) để khớp với bề rộng bão
                if (ticks % 10 == 0) {
                    for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.5, 3.0, 4.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 20, 128, false, false, false));

                            double multi = hitTargets.contains(target.getUniqueId()) ? (baseMulti * 0.2) : (baseMulti * 0.8);
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.NATURE);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            hitTargets.add(target.getUniqueId());
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 5. ÁNH SÁNG: QUANG PHỔ CHÓI LÒA (STUN)
    // ==========================================
    private void triggerLightBurst(Player player, RPGPlayer rpgPlayer, int level, double baseMulti) {
        Location center = player.getLocation().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2f, 2.0f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);

        Particle.DustOptions lightDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 2.5f);

        // [ĐÃ SỬA] Quả cầu nở to dần ra ngoài
        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > 6.0) { this.cancel(); return; }
                int particles = (int) (60 * r);
                for (int i = 0; i < particles; i++) {
                    double phi = Math.acos(1 - 2 * random.nextDouble());
                    double theta = 2 * Math.PI * random.nextDouble();
                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.cos(phi);
                    double z = r * Math.sin(phi) * Math.sin(theta);

                    Location pLoc = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0.0, lightDust, true);
                    if (random.nextInt(4) == 0) center.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0.0, null, true);
                }
                r += 1.5; // Tốc độ phình to
            }
        }.runTaskTimer(plugin, 0L, 1L);

        int stunTicks = (int) ((1.5 + (level * 0.1)) * 20);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                if (target instanceof Player) {
                    StatusEffectManager.applyAbsoluteRoot((Player) target, stunTicks); // Vẫn dùng Root tuyệt đối (Jumping disable) cho Player
                } else {
                    // Trói 100% tốc chạy quái
                    StatusEffectManager.applySlow(target, stunTicks, 1.0, 10);
                }
                StatusEffectManager.applyEffect(target, PotionEffectType.BLINDNESS, stunTicks, 1);

                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.LIGHT);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * 0.7 * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }
    }

    // ==========================================
    // 6. BÓNG TỐI: KHE NỨT DIỆT VONG (ĐẦU RỒNG KHẠC HƯ KHÔNG)
    // ==========================================
    private void triggerDarkBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        // 1. CHỐT TỌA ĐỘ NGANG (Không bị ảnh hưởng khi ngước lên/xuống)
        Location startLoc = player.getLocation();
        Vector forwardBase = startLoc.getDirection().setY(0).normalize();

        // 2. VỊ TRÍ ĐẦU RỒNG: Cách người chơi 1 block về phía trước, cao ngang ngực (1.4)
        Location headBaseLoc = startLoc.clone().add(forwardBase).add(0, 1.4, 0);
        headBaseLoc.setDirection(forwardBase); // Nhìn thẳng lúc đầu

        // 3. TRIỆU HỒI ĐẦU RỒNG
        org.bukkit.entity.ItemDisplay dragonHead = null;
        try {
            dragonHead = player.getWorld().spawn(headBaseLoc, org.bukkit.entity.ItemDisplay.class, display -> {
                display.setItemStack(new org.bukkit.inventory.ItemStack(Material.DRAGON_HEAD));
                display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.HEAD);

                org.bukkit.util.Transformation transform = display.getTransformation();
                // Phóng to đầu rồng lên 1.5 lần
                transform.getScale().set(1.5f, 1.5f, 1.5f);

                // [ĐÃ SỬA]: Xoay mô hình item lật lại 180 độ để nó nhìn đúng hướng!
                transform.getLeftRotation().rotateY((float) Math.PI);

                display.setTransformation(transform);

                // TeleportDuration = 1 tick giúp nó quay đầu cực mượt
                display.setTeleportDuration(1);
            });
        } catch (Exception ignored) {}

        final org.bukkit.entity.ItemDisplay finalHead = dragonHead;

        player.getWorld().playSound(headBaseLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);

        Particle.DustOptions darkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(20, 0, 40), 1.2f);
        Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 255), 0.8f);
        Set<UUID> hitTargets = new HashSet<>();

        // Tăng số tick lên 30 (1.5 giây) để nó quét 180 độ không bị quá nhanh
        int maxTicks = 30;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > maxTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Quét 180 độ: từ -90 độ (Trái) sang +90 độ (Phải)
                double angle = -90.0 + (180.0 * ((double) ticks / maxTicks));

                // Toán học: Quay Vector quanh trục Y
                Vector sweepDir = forwardBase.clone().rotateAroundY(Math.toRadians(angle));

                // CẬP NHẬT GÓC NHÌN ĐẦU RỒNG
                if (finalHead != null && finalHead.isValid()) {
                    Location newHeadLoc = headBaseLoc.clone();
                    newHeadLoc.setDirection(sweepDir); // Đầu rồng luôn nhìn theo hướng khạc
                    finalHead.teleport(newHeadLoc);
                }

                if (ticks % 3 == 0) {
                    headBaseLoc.getWorld().playSound(headBaseLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.3f, 1.2f);
                }

                // TẠO KHOẢNG LÕM: Thay vì bắt đầu từ d=0, ta bắt đầu từ d=2.5 (Tạo khoảng trống cho đầu rồng)
                for (double d = 2.5; d <= 8.0; d += 0.8) {
                    Location partLoc = headBaseLoc.clone().add(sweepDir.clone().multiply(d));

                    // Lõi tia hư không
                    headBaseLoc.getWorld().spawnParticle(Particle.DUST, partLoc, 3, 0.1, 0.1, 0.1, 0.0, darkDust);

                    // Tia nhiễu
                    if (ticks % 2 == 0) {
                        headBaseLoc.getWorld().spawnParticle(Particle.DUST, partLoc, 1, 0.05, 0.05, 0.05, 0.0, pinkDust);
                        headBaseLoc.getWorld().spawnParticle(Particle.SQUID_INK, partLoc, 1, 0.2, 0.2, 0.2, 0.0);
                    }

                    // Xử lý sát thương
                    for (org.bukkit.entity.Entity ent : partLoc.getWorld().getNearbyEntities(partLoc, 1.2, 1.2, 1.2)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            if (!hitTargets.contains(target.getUniqueId())) {

                                StatusEffectManager.applyEffect(target, PotionEffectType.BLINDNESS, 80, 1);
                                StatusEffectManager.applyEffect(target, PotionEffectType.WITHER, 80, 1);

                                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.DARK);
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * 1.091 * reaction.damageMultiplier, 0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);

                                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                                hitTargets.add(target.getUniqueId());
                            }
                        }
                    }
                }
                ticks++;
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                // Xóa đầu rồng khi kết thúc chiêu
                if (finalHead != null && finalHead.isValid()) {
                    finalHead.getWorld().spawnParticle(Particle.SQUID_INK, finalHead.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
                    finalHead.remove();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}