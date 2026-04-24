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

    private double getProjectileDamageMulti(int level) { return 1.8 + ((level - 1) * 0.1); }
    private double getDomainDamageMulti(int level) { return 2.5 + ((level - 1) * 0.15); }

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
        lore.add("§8[Nội tại - Nén]");
        lore.add("§f- Viên đạn gây sát thương: §c" + projDmgPct + "% §fcho mục tiêu dính đòn.");
        lore.add("§f- Nếu bay quá lâu sẽ trở nên thiếu ổn định và phát nổ.");
        lore.add("");

        switch (element) {
            case FIRE:
                lore.add("§c[Thi Triển: Bạo Liệt Viêm Kích]");
                lore.add("§f- Phát nổ bộc phá lan rộng (Bán kính 4m).");
                lore.add("§f- Gây §c" + (domDmgPct * 2) + "% ST§f và thiêu đốt mọi kẻ địch.");
                break;
            case WATER:
                lore.add("§b[Thi Triển: Thủy Triều Dâng]");
                lore.add("§f- Kích nổ 3 đợt sóng liên tiếp (Mỗi 1.5s).");
                lore.add("§f- Gây §c" + domDmgPct + "% ST§f và hất văng kẻ địch lên trời.");
                break;
            case WIND:
                lore.add("§a[Thi Triển: Gió Xoáy]");
                lore.add("§f- Giải phóng lõi lốc xoáy duy trì §a6s §f(Bán kính 4m).");
                lore.add("§f- Liên tục hút kẻ địch vào tâm với tổng sát");
                lore.add("§f  thương bằng §c" + domDmgPct + "% ST§f.");
                break;
            case NATURE:
                lore.add("§2[Thi Triển: Dây Leo Cuồng Nộ]");
                lore.add("§f- Tạo rừng dây leo khóa chặt vị trí kẻ địch");
                lore.add("§f  trong §a3.5s §f(Bán kính 5m), gây §c" + domDmgPct + "% ST§f.");
                lore.add("§f- Rút sinh lực kẻ địch bị trói về cho bản");
                lore.add("§f  thân: Hồi §a0.5% Máu/s §f(Tối đa 8%).");
                break;
            case LIGHT:
                lore.add("§e[Thi Triển: Quả Cầu Ánh Sáng]");
                lore.add("§f- Nở ra Thánh Vực tồn tại §a8s §f(Bán kính 6m).");
                lore.add("§f- Gây §c" + domDmgPct + "% ST§f khởi điểm.");
                lore.add("§f- Kẻ địch bên trong: Nhận thêm §c15% ST§f.");
                lore.add("§f- Bản thân bên trong: Giảm §a8% ST nhận vào§f.");
                break;
            case DARK:
                lore.add("§5[Thi Triển: Khe Nứt Bóng Tối]");
                lore.add("§f- Khởi tạo gây §c" + (int)(domDmgPct * 0.7) + "% ST§f và xé rách");
                lore.add("§f  không gian thành một Rãnh nứt tồn tại §a16s§f.");
                lore.add("§f- Rãnh nứt liên tục phóng sét Hư không vào tối đa");
                lore.add("§f  §c3 mục tiêu§f, gây §c100% ST vũ khí §fmỗi 1.5s.");
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
        Vector direction = startLoc.getDirection().normalize().multiply(1.5);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 70 || !player.isOnline()) {
                    triggerAirBurst(player, rpgPlayer, currentLoc, element, projMulti);
                    this.cancel();
                    return;
                }

                if (ticks > 3) {
                    org.bukkit.Color elColor = getElementColor(element);
                    Particle.DustOptions dust = new Particle.DustOptions(elColor, 1.5f);

                    currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 8, 0.2, 0.2, 0.2, dust);
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
            case WIND: return org.bukkit.Color.LIME;
            case NATURE: return org.bukkit.Color.GREEN;
            case LIGHT: return org.bukkit.Color.YELLOW;
            case DARK: return org.bukkit.Color.PURPLE;
            default: return org.bukkit.Color.WHITE;
        }
    }

    // ==========================================
    // TỰ BẠO TRÊN KHÔNG (REWORKED: PHÁO HOA RÕ NÉT R=4)
    // ==========================================
    private void triggerAirBurst(Player player, RPGPlayer rpgPlayer, Location center, MageElement element, double projMulti) {
        center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1f);
        Particle.DustOptions dust = new Particle.DustOptions(getElementColor(element), 2.0f);

        // Nổ gốc pháo hoa cho sinh động
        center.getWorld().spawnParticle(Particle.FIREWORK, center, 60, 1.5, 1.5, 1.5, 0.2);

        // Bóng ma thuật hình cầu lan tỏa (R=4)
        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > 4.0) { this.cancel(); return; }
                int particles = (int) (40 * r); // Càng to càng nhiều hạt
                for (int i = 0; i < particles; i++) {
                    double phi = Math.acos(1 - 2 * random.nextDouble());
                    double theta = 2 * Math.PI * random.nextDouble();

                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.cos(phi);
                    double z = r * Math.sin(phi) * Math.sin(theta);

                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, dust);
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

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);

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
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, dust);
                }
                ticks+=5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // ==========================================
    // HỎA NGỤC (REWORKED: BỘC PHÁ HÌNH CẦU SIÊU MẠNH)
    // ==========================================
    private void triggerFireExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2); // Nổ gốc chà bá
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        new BukkitRunnable() {
            double r = 0.5;
            @Override
            public void run() {
                if (r > 4.0) { this.cancel(); return; }

                int particles = (int) (80 * r); // Hạt dày đặc
                for (int i = 0; i < particles; i++) {
                    double phi = Math.acos(1 - 2 * random.nextDouble());
                    double theta = 2 * Math.PI * random.nextDouble();

                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.cos(phi);
                    double z = r * Math.sin(phi) * Math.sin(theta);

                    Location partLoc = center.clone().add(x, y, z);
                    // Hạt tản văng ra ngoài với tốc độ 0.15 tạo cảm giác áp lực nổ
                    center.getWorld().spawnParticle(Particle.FLAME, partLoc, 1, 0.1, 0.1, 0.1, 0.15);
                    if (random.nextInt(4) == 0) {
                        center.getWorld().spawnParticle(Particle.LAVA, partLoc, 1, 0, 0, 0, 0);
                    }
                }
                r += 1.0; // Tốc độ bung 1.0 để mắt thường kịp thu nhận hình cầu
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.FIRE);
                target.setFireTicks(100);

                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 2.0 * reaction.damageMultiplier, 0, true, false);
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
                        center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(r * Math.cos(t), 0.2, r * Math.sin(t)), 5, 0.2, 0.5, 0.2, 0.1);
                    }
                }

                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;
                        target.setVelocity(new Vector(0, 1.2, 0));

                        if (wave == 0) {
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WATER);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                        } else {
                            ReactionEngine.applyMarkOnly(target, MageElement.WATER);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti, 0, true, false);
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
                    center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(x, y, z), 2, 0.1, 0.1, 0.1, 0.02);
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
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(r * Math.cos(t), 0.1, r * Math.sin(t)), 1, 0, 0, 0, vineDark);
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
                        target.getWorld().spawnParticle(Particle.DUST, p1, 1, 0, 0, 0, vineDark);
                        target.getWorld().spawnParticle(Particle.DUST, p2, 1, 0, 0, 0, vineLight);
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
                        rpgPlayer.heal(Math.min(maxHp * 0.005 * validCount, maxHp * 0.08));
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
                current.getWorld().spawnParticle(Particle.DUST, current, 2, 0.1, 0.1, 0.1, green);
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
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(r * Math.cos(t), 0.2, r * Math.sin(t)), 1, 0, 0, 0, 0.05);
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
                        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(6 * Math.cos(t), 0.2, 6 * Math.sin(t)), 1, 0, 0, 0, 0);
                    }
                    center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add((random.nextDouble() - 0.5) * 10, 0.2, (random.nextDouble() - 0.5) * 10), 5, 0, 1.5, 0, 0.05);
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

    // ==========================================
    // KHE NỨT BÓNG TỐI (REWORKED: NÉT HƠN, ÍT RÁC HƠN)
    // ==========================================
    private void triggerDarkExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().spawnParticle(Particle.PORTAL, center, 100, 3.0, 3.0, 3.0, 0.5);

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

        List<Vector> riftPoints = new ArrayList<>();
        Vector currentPoint = new Vector(0, -1.5, 0);
        riftPoints.add(currentPoint.clone());
        for (int i = 0; i < 6; i++) {
            currentPoint.add(new Vector((random.nextDouble() - 0.5) * 1.5, 0.6, (random.nextDouble() - 0.5) * 1.5));
            riftPoints.add(currentPoint.clone());
        }

        // Mỏng và sắc nét hơn
        Particle.DustOptions riftOuter = new Particle.DustOptions(org.bukkit.Color.fromRGB(60, 0, 100), 1.2f);
        Particle.DustOptions riftInner = new Particle.DustOptions(org.bukkit.Color.BLACK, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 320 || !player.isOnline()) { this.cancel(); return; }

                // Giảm tần suất vẽ Rãnh Nứt (4 tick 1 lần thay vì mỗi tick) để bớt nhiễu hạt
                if (ticks % 4 == 0) {
                    for (int i = 0; i < riftPoints.size() - 1; i++) {
                        drawRiftSegment(riftLoc.clone().add(riftPoints.get(i)), riftLoc.clone().add(riftPoints.get(i+1)), riftLoc.getWorld(), riftOuter, riftInner);
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

    private void drawRiftSegment(Location p1, Location p2, org.bukkit.World world, Particle.DustOptions outer, Particle.DustOptions inner) {
        Vector dir = p2.toVector().subtract(p1.toVector());
        double dist = dir.length();
        if (dist == 0) return;
        dir.normalize();
        // Tăng step lên 0.4 để tạo độ mảnh
        for(double t = 0; t <= dist; t += 0.4) {
            Location loc = p1.clone().add(dir.clone().multiply(t));
            world.spawnParticle(Particle.DUST, loc, 1, 0.02, 0.02, 0.02, outer);
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, inner);
        }
    }

    private void drawCurvedBlackLightning(Location start, Location end, org.bukkit.World world) {
        Vector p0 = start.toVector();
        Vector p2 = end.toVector();
        Vector dir = p2.clone().subtract(p0);
        double dist = dir.length();
        if (dist == 0) return;

        Vector mid = p0.clone().add(dir.clone().multiply(0.5));
        Vector cross = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (cross.lengthSquared() == 0) cross = new Vector(1, 0, 0);

        cross.multiply((random.nextBoolean() ? 1 : -1) * (dist * 0.3));
        Vector p1 = mid.add(cross).add(new Vector(0, dist * 0.2, 0));

        Particle.DustOptions blackDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(15, 0, 25), 0.7f);

        for (double t = 0; t <= 1.0; t += 0.05) {
            double u = 1 - t;
            double tt = t * t;
            double uu = u * u;
            Vector point = p0.clone().multiply(uu).add(p1.clone().multiply(2 * u * t)).add(p2.clone().multiply(tt));
            world.spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1, 0, 0, 0, 0, blackDust);
        }
    }
}