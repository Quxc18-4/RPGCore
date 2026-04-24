package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElementalSphereSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ElementalSphereSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_elemental_sphere"; }
    @Override public String getName() { return "Ma Pháp Trận"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.HEART_OF_THE_SEA; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 10; }
    @Override public java.util.Map<String, Integer> getRequiredSkills() { return new java.util.HashMap<>(); }

    @Override
    public double getManaCost(int level) {
        double cost = 70.0;
        if (level >= 3) cost += 15.0;
        if (level >= 6) cost += 15.0;
        if (level >= 9) cost += 15.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 13.0;
        if (level >= 3) cd -= 0.6;
        if (level >= 6) cd -= 0.6;
        if (level >= 9) cd -= 0.6;
        return (long) (cd * 1000);
    }

    private double getProjectileDamageMulti(int level) {
        return 1.2 + ((level - 1) * 0.05);
    }
    private double getDomainDamageMulti(int level) {
        return 1.5 + ((level - 1) * 0.10);
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

        int projDmgPct = (int) (getProjectileDamageMulti(displayLevel) * 100);
        int domDmgPct = (int) (getDomainDamageMulti(displayLevel) * 100);

        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§8[Nội tại - Nén Ma Lực]");
        lore.add("§f- Viên đạn gây sát thương: §c" + projDmgPct + "% §fcho mục tiêu dính đòn.");
        lore.add("§f- Nếu bay quá lâu sẽ trở nên thiếu ổn định và phát nổ.");
        lore.add("");

        switch (element) {
            case FIRE:
                lore.add("§c[Thi Triển: Bạo Liệt Viêm Kích]");
                lore.add("§f- Phát nổ bộc phá diện rộng tại vị trí tiếp xúc.");
                lore.add("§f- Gây §c" + (domDmgPct * 2) + "% Sát thương§f và thiêu đốt mọi kẻ địch.");
                break;
            case WATER:
                lore.add("§b[Thi Triển: Thủy Triều Dâng]");
                lore.add("§f- Kích nổ 3 đợt sóng liên tiếp (Mỗi 1.5s).");
                lore.add("§f- Gây §c" + domDmgPct + "% Sát thương§f và hất văng kẻ địch lên trời.");
                break;
            case WIND:
                lore.add("§a[Thi Triển: Cuồng Phong]");
                lore.add("§f- Giải phóng lõi lốc xoáy duy trì §a6s §f.");
                lore.add("§f- Liên tục hút kẻ địch vào tâm với tổng sát");
                lore.add("§f  thương bằng §c" + domDmgPct + "% Sát thương§f.");
                break;
            case NATURE:
                lore.add("§2[Thi Triển: Dây Leo Cuồng Nộ]");
                lore.add("§f- Tạo rừng dây leo khóa chặt vị trí kẻ địch");
                lore.add("§f  trong §a3.5s §f, gây §c" + domDmgPct + "% Sát thương§f.");
                lore.add("§f- Rút sinh lực kẻ địch bị trói về cho bản");
                lore.add("§f  thân: Hồi §a0.5% Máu/s §f(Tối đa 4% / Đợt).");
                break;
            case LIGHT:
                lore.add("§e[Thi Triển: Quả Cầu Ánh Sáng]");
                lore.add("§f- Tạo ra Thánh Vực duy trì §a8s §f.");
                lore.add("§f- Gây §c" + domDmgPct + "% Sát thương§f.");
                lore.add("§f- Kẻ địch bên trong: Nhận thêm §c15% Sát thương§f.");
                lore.add("§f- Đồng minh bên trong: Giảm §a8% Sát thương nhận vào§f.");
                break;
            case DARK:
                lore.add("§5[Thi Triển: Khe Nứt Bóng Tối]");
                lore.add("§f- Khởi tạo gây §c" + (int)(domDmgPct * 0.7) + "% Sát thương§f và xé rách");
                lore.add("§f  không gian thành một Rãnh nứt tồn tại §a10s§f.");
                lore.add("§f- Rãnh nứt liên tục phóng sét Hư không vào tối đa");
                lore.add("§f  §c3 mục tiêu§f, gây §c100% Sát thương §fmỗi 1.5s.");
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
        double projMulti = getProjectileDamageMulti(level);
        double domMulti = getDomainDamageMulti(level);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
        fireElementalSphere(player, rpgPlayer, element, projMulti, domMulti);

        return true;
    }

    private void fireElementalSphere(Player player, RPGPlayer rpgPlayer, MageElement element, double projMulti, double domMulti) {
        Location startLoc = player.getEyeLocation();
        // [CẬP NHẬT 1]: Tăng tốc độ bay lên 2.0 blocks/tick
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                // [CẬP NHẬT 2]: 11 ticks * 2.0 = 22 blocks tầm xa
                if (ticks >= 11 || !player.isOnline()) {
                    triggerAirBurst(player, rpgPlayer, currentLoc, element, projMulti);
                    this.cancel();
                    return;
                }

                // Hiện sớm hơn vì đạn bay nhanh
                if (ticks > 1) {
                    org.bukkit.Color elColor = getElementColor(element);
                    // [CẬP NHẬT 3]: Nén kích thước hạt Dust xuống 1.0f
                    Particle.DustOptions dust = new Particle.DustOptions(elColor, 1.0f);

                    // Lõi End Rod nén chặt hơn (0.02)
                    currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 4, 0.02, 0.02, 0.02, 0.0, null, true);
                    // Lớp bụi bên ngoài nén chặt (0.1) và tăng số lượng lên 15
                    currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 15, 0.1, 0.1, 0.1, 0.0, dust, true);
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    triggerDetonation(player, rpgPlayer, currentLoc, null, element, domMulti);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;
                        double hitDmg = CombatManager.calculateDamage(player, rpgPlayer, target, projMulti, 0, true, false);
                        CombatManager.applySkillDamage(player, target, hitDmg);
                        triggerDetonation(player, rpgPlayer, currentLoc, target, element, domMulti);
                        this.cancel();
                        return;
                    }
                }

                currentLoc.add(direction);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private org.bukkit.Color getElementColor(MageElement element) {
        switch (element) {
            case FIRE: return org.bukkit.Color.RED;
            case WATER: return org.bukkit.Color.AQUA;
            case WIND: return org.bukkit.Color.fromRGB(100, 255, 200); // Xanh neon / Trắng bạc
            case NATURE: return org.bukkit.Color.GREEN;
            case LIGHT: return org.bukkit.Color.YELLOW;
            case DARK: return org.bukkit.Color.PURPLE;
            default: return org.bukkit.Color.WHITE;
        }
    }

    private void triggerAirBurst(Player player, RPGPlayer rpgPlayer, Location center, MageElement element, double projMulti) {
        center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1f);
        Particle.DustOptions dust = new Particle.DustOptions(getElementColor(element), 2.0f);

        center.getWorld().spawnParticle(Particle.FIREWORK, center, 60, 1.0, 1.0, 1.0, 0.15, null, true);

        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > 4.0) { this.cancel(); return; }
                int particles = (int) (50 * r);
                for (int i = 0; i < particles; i++) {
                    double phi = Math.acos(1 - 2 * random.nextDouble());
                    double theta = 2 * Math.PI * random.nextDouble();

                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.cos(phi);
                    double z = r * Math.sin(phi) * Math.sin(theta);

                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0.0, dust, true);
                }
                r += 1.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, element);
                double finalDmgMulti = projMulti * reaction.damageMultiplier;

                double hitDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(player, target, hitDmg);

                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }
    }

    private void triggerDetonation(Player player, RPGPlayer rpgPlayer, Location hitLoc, LivingEntity hitEntity, MageElement element, double domMulti) {
        Location center = hitLoc.clone();
        boolean isDomainType = (element == MageElement.LIGHT || element == MageElement.NATURE || element == MageElement.WATER);

        if (isDomainType) {
            Location groundSeek = (hitEntity != null) ? hitEntity.getLocation() : center.clone();
            boolean foundGround = false;

            for (int i = 0; i < 10; i++) {
                if (groundSeek.getBlock().getType().isSolid()) {
                    groundSeek.add(0, 1.1, 0);
                    foundGround = true;
                    break;
                }
                groundSeek.subtract(0, 1, 0);
            }

            if (foundGround) {
                center = groundSeek;
            } else {
                center = hitLoc.clone().subtract(0, 1.5, 0);
                drawMagicPlatform(center, element);
            }
        }

        // [CẬP NHẬT 4]: Đổi âm thanh nổ thường thành âm thanh thủy tinh vỡ pha phép thuật
        center.getWorld().playSound(center, Sound.ENTITY_SPLASH_POTION_BREAK, 1.2f, 0.8f);
        center.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);

        switch (element) {
            case FIRE: triggerFireExplosion(player, rpgPlayer, center, domMulti); break;
            case WATER: triggerWaterDomain(player, rpgPlayer, center, domMulti); break;
            case WIND: triggerWindExplosion(player, rpgPlayer, center, domMulti); break;
            case NATURE: triggerNatureDomain(player, rpgPlayer, center, domMulti); break;
            case LIGHT: triggerLightDomain(player, rpgPlayer, center, domMulti); break;
            case DARK: triggerDarkExplosion(player, rpgPlayer, center, domMulti); break;
        }
    }

    private void drawMagicPlatform(Location center, MageElement element) {
        Particle.DustOptions dust = new Particle.DustOptions(getElementColor(element), 1.0f);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 160) { this.cancel(); return; }
                for (double t = 0; t < Math.PI * 2; t += Math.PI / 8) {
                    double x = 4 * Math.cos(t);
                    double z = 4 * Math.sin(t);
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, 0.0, dust, true);
                }
                ticks+=5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void triggerFireExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2, 0.5, 0.5, 0.5, 0.0, null, true);
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > 4.0) { this.cancel(); return; }

                int particles = (int) (80 * r);
                for (int i = 0; i < particles; i++) {
                    double phi = Math.acos(1 - 2 * random.nextDouble());
                    double theta = 2 * Math.PI * random.nextDouble();

                    double dirX = Math.sin(phi) * Math.cos(theta);
                    double dirY = Math.cos(phi);
                    double dirZ = Math.sin(phi) * Math.sin(theta);

                    Location partLoc = center.clone().add(dirX * r, dirY * r, dirZ * r);

                    center.getWorld().spawnParticle(Particle.FLAME, partLoc, 0, dirX, dirY, dirZ, 0.15, null, true);
                    if (random.nextInt(4) == 0) {
                        center.getWorld().spawnParticle(Particle.LAVA, partLoc, 1, 0, 0, 0, 0.0, null, true);
                    }
                }
                r += 1.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.FIRE);
                target.setFireTicks(100);

                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 1.5 * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);

                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }
    }

    private void triggerWaterDomain(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave >= 3 || !player.isOnline()) { this.cancel(); return; }

                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1f, 0.8f);
                for (double r = 1; r <= 4; r++) {
                    for (double t = 0; t < Math.PI * 2; t += Math.PI / (r * 4)) {
                        center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(r * Math.cos(t), 0.2, r * Math.sin(t)), 5, 0.2, 0.5, 0.2, 0.1, null, true);
                    }
                }

                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;
                        target.setVelocity(new Vector(0, 1.2, 0));

                        if (wave == 0) {
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WATER);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 0.5 * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                        } else {
                            ReactionEngine.applyMarkOnly(target, MageElement.WATER);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 0.5, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                        }
                    }
                }
                wave++;
            }
        }.runTaskTimer(plugin, 0L, 30L);
    }

    private void triggerWindExplosion(Player player, RPGPlayer rpgPlayer, Location hitCenter, double domMulti) {
        double dpsMulti = domMulti / 6.0;
        Location center = hitCenter.clone();
        if (center.getBlock().getType().isSolid()) center.add(0, 1.2, 0);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 120 || !player.isOnline()) { this.cancel(); return; }

                double angle = ticks * 0.4;
                for (int i = 0; i < 5; i++) {
                    double y = i * 0.8;
                    double r = 0.5 + (y * 0.6);
                    double x = r * Math.cos(angle + i);
                    double z = r * Math.sin(angle + i);
                    center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(x, y, z), 2, 0.1, 0.1, 0.1, 0.02, null, true);
                }

                if (ticks % 10 == 0) center.getWorld().playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.3f, 1.5f);

                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        Vector pull = center.toVector().subtract(ent.getLocation().toVector()).normalize().multiply(0.12);
                        ent.setVelocity(ent.getVelocity().add(pull));
                    }
                }

                if (ticks % 20 == 0) {
                    for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            if (ticks == 0) {
                                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WIND);
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dpsMulti * reaction.damageMultiplier, 0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);
                                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                            } else {
                                ReactionEngine.applyMarkOnly(target, MageElement.WIND);
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dpsMulti, 0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerNatureDomain(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 1f, 0.5f);
        List<LivingEntity> rootedTargets = new ArrayList<>();

        Particle.DustOptions vineDark = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 100, 0), 1.2f);
        Particle.DustOptions vineLight = new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.8f);

        new BukkitRunnable() {
            double r = 1;
            @Override
            public void run() {
                if (r > 5.0) { this.cancel(); return; }
                for (double t = 0; t < Math.PI * 2; t += Math.PI / (r * 2)) {
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(r * Math.cos(t), 0.1, r * Math.sin(t)), 1, 0, 0, 0, 0.0, vineDark, true);
                }
                r += 1.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                rootedTargets.add(target);
                StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 70, 10);

                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.NATURE);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);

                new BukkitRunnable() {
                    @Override
                    public void run() { target.setVelocity(new Vector(0, -0.1, 0)); }
                }.runTaskLater(plugin, 1L);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 70 || !player.isOnline()) { this.cancel(); return; }

                for (LivingEntity target : rootedTargets) {
                    if (target.isValid() && !target.isDead()) {
                        double y = (ticks * 0.1) % 2.0;
                        double angle = ticks * 0.5;
                        Location p1 = target.getLocation().add(0.6 * Math.cos(angle), y, 0.6 * Math.sin(angle));
                        Location p2 = target.getLocation().add(0.6 * Math.cos(angle + Math.PI), 2.0 - y, 0.6 * Math.sin(angle + Math.PI));
                        target.getWorld().spawnParticle(Particle.DUST, p1, 1, 0, 0, 0, 0.0, vineDark, true);
                        target.getWorld().spawnParticle(Particle.DUST, p2, 1, 0, 0, 0, 0.0, vineLight, true);
                    }
                }

                if (ticks % 20 == 0 && !rootedTargets.isEmpty()) {
                    int validCount = 0;
                    for (LivingEntity target : rootedTargets) {
                        if (target.isValid() && !target.isDead()) {
                            validCount++;
                            spawnGreenDustMissile(target.getLocation().add(0, 1, 0), player);
                        }
                    }
                    if (validCount > 0) {
                        double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                        rpgPlayer.heal(Math.min(maxHp * 0.005 * validCount, maxHp * 0.04));
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnGreenDustMissile(Location startLoc, Player targetPlayer) {
        Particle.DustOptions green = new Particle.DustOptions(org.bukkit.Color.LIME, 1.5f);
        new BukkitRunnable() {
            Location current = startLoc.clone();
            int lifespan = 0;
            @Override
            public void run() {
                if (!targetPlayer.isOnline() || lifespan > 40) { this.cancel(); return; }
                Location pLoc = targetPlayer.getLocation().add(0, 1, 0);
                Vector dir = pLoc.toVector().subtract(current.toVector());
                if (dir.length() < 1.0) { this.cancel(); return; }

                current.add(dir.normalize().multiply(0.8));
                current.getWorld().spawnParticle(Particle.DUST, current, 2, 0.1, 0.1, 0.1, 0.0, green, true);
                lifespan++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerLightDomain(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        new BukkitRunnable() {
            double r = 0;
            @Override
            public void run() {
                if (r > 6.0) { this.cancel(); return; }
                for (double t = 0; t < Math.PI * 2; t += Math.PI / 16) {
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(r * Math.cos(t), 0.2, r * Math.sin(t)), 1, 0, 0, 0, 0.05, null, true);
                }
                r += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.LIGHT);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 160) { this.cancel(); return; }

                if (ticks % 5 == 0) {
                    for (double t = 0; t < Math.PI * 2; t += Math.PI / 12) {
                        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(6 * Math.cos(t), 0.2, 6 * Math.sin(t)), 1, 0, 0, 0, 0.0, null, true);
                    }
                    center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add((random.nextDouble() - 0.5) * 10, 0.2, (random.nextDouble() - 0.5) * 10), 5, 0, 1.5, 0, 0.05, null, true);
                }

                if (ticks % 10 == 0) {
                    for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
                        if (ent instanceof LivingEntity) {
                            if (CombatManager.canTarget(player, ent)) {
                                ent.setMetadata("Mage_Exposure_Debuff", new FixedMetadataValue(plugin, true));
                            } else {
                                ent.setMetadata("Light_Domain_Protection", new FixedMetadataValue(plugin, System.currentTimeMillis() + 1000L));
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void triggerDarkExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().spawnParticle(Particle.PORTAL, center, 100, 3.0, 3.0, 3.0, 0.5, null, true);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.DARK);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 0.7 * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }

        Location riftLoc = center.clone().add(0, 2, 0);

        List<Vector> spine = new ArrayList<>();
        for (double y = -2.0; y <= 2.0; y += 0.4) {
            spine.add(new Vector(0, y, 0));
        }

        Particle.DustOptions riftOuter = new Particle.DustOptions(org.bukkit.Color.fromRGB(60, 0, 100), 1.0f);
        Particle.DustOptions riftInner = new Particle.DustOptions(org.bukkit.Color.BLACK, 0.6f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 200 || !player.isOnline()) { this.cancel(); return; }

                if (ticks % 4 == 0) {
                    for (int i = 0; i < spine.size() - 1; i++) {
                        double progress = (double) i / (spine.size() - 1);
                        double thickness = 4.0 * progress * (1.0 - progress) * 0.4;

                        drawRiftSegment(riftLoc.clone().add(spine.get(i)), riftLoc.clone().add(spine.get(i+1)), riftLoc.getWorld(), riftOuter, riftInner, thickness);
                    }
                }

                if (ticks % 20 == 0) riftLoc.getWorld().playSound(riftLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f);

                if (ticks > 0 && ticks % 30 == 0) {
                    int targetsHit = 0;
                    for (org.bukkit.entity.Entity ent : riftLoc.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            ReactionEngine.applyMarkOnly(target, MageElement.DARK);

                            drawCurvedBlackLightning(riftLoc, target.getLocation().add(0, 1, 0), riftLoc.getWorld());
                            riftLoc.getWorld().playSound(riftLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.5f, 2.0f);

                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 1.0, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);

                            targetsHit++;
                            if (targetsHit >= 3) break;
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawRiftSegment(Location p1, Location p2, org.bukkit.World world, Particle.DustOptions outer, Particle.DustOptions inner, double thickness) {
        Vector dir = p2.toVector().subtract(p1.toVector());
        double dist = dir.length();
        if (dist == 0) return;
        dir.normalize();

        for(double t = 0; t <= dist; t += 0.2) {
            Location loc = p1.clone().add(dir.clone().multiply(t));
            world.spawnParticle(Particle.DUST, loc, 2, thickness, 0, thickness, 0.0, outer, true);
            world.spawnParticle(Particle.DUST, loc, 1, thickness * 0.3, 0, thickness * 0.3, 0.0, inner, true);
        }
    }

    private void drawCurvedBlackLightning(Location start, Location end, org.bukkit.World world) {
        Vector p0 = start.toVector();
        Vector p2 = end.toVector();
        Vector dir = p2.clone().subtract(p0);
        double dist = dir.length();
        if (dist == 0) return;

        // Điểm uốn cong chính (Giữ nguyên form Bezier)
        Vector mid = p0.clone().add(dir.clone().multiply(0.5));
        Vector cross = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (cross.lengthSquared() == 0) cross = new Vector(1, 0, 0);

        cross.multiply((random.nextBoolean() ? 1 : -1) * (dist * 0.3));
        Vector p1 = mid.add(cross).add(new Vector(0, dist * 0.2, 0));

        Particle.DustOptions blackDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(15, 0, 25), 0.7f);

        Location currentLoc = start.clone(); // Lưu điểm hiện tại để nối nét liền

        // Chia đường cong thành các đoạn nhỏ và làm "nhiễu" chúng
        for (double t = 0.05; t <= 1.0; t += 0.05) {
            double u = 1 - t;
            double tt = t * t;
            double uu = u * u;
            // Tính điểm trên đường cong mượt
            Vector point = p0.clone().multiply(uu).add(p1.clone().multiply(2 * u * t)).add(p2.clone().multiply(tt));

            // [MỚI]: Thêm yếu tố ngẫu nhiên (Jitter) dạng hình Sin (Giật mạnh ở giữa, 2 đầu cố định)
            double jitterStrength = 0.6 * Math.sin(t * Math.PI);
            Vector jitter = new Vector(
                    (random.nextDouble() - 0.5) * jitterStrength,
                    (random.nextDouble() - 0.5) * jitterStrength,
                    (random.nextDouble() - 0.5) * jitterStrength
            );
            point.add(jitter); // Làm lệch tọa độ

            Location nextLoc = new Location(world, point.getX(), point.getY(), point.getZ());
            if (t >= 0.95) nextLoc = end.clone(); // Điểm cuối cùng chốt chuẩn xác vào mục tiêu

            // Nối nét liền từ điểm cũ sang điểm mới (Tạo thành tia sét gấp khúc)
            Vector segmentDir = nextLoc.toVector().subtract(currentLoc.toVector());
            double segmentDist = segmentDir.length();
            segmentDir.normalize();
            for (double st = 0; st <= segmentDist; st += 0.2) {
                Location pLoc = currentLoc.clone().add(segmentDir.clone().multiply(st));
                world.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0.0, blackDust, true);
            }

            // Cập nhật điểm nối
            currentLoc = nextLoc;
        }
    }
}