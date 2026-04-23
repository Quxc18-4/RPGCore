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

        lore.add("§7Bắn ra một Khối ma lực nén với tốc độ cao.");
        lore.add("§7Nếu va chạm mục tiêu hoặc địa hình, khối");
        lore.add("§7ma lực sẽ bung nở thành Ma Pháp Trận.");
        lore.add("§7Nếu bay trên không quá lâu, nó sẽ tự bạo.");
        lore.add("");

        double mana = getManaCost(displayLevel);
        double cd = getCooldown(displayLevel) / 1000.0;
        int projDmgPct = (int) (getProjectileDamageMulti(displayLevel) * 100);
        int domDmgPct = (int) (getDomainDamageMulti(displayLevel) * 100);

        lore.add("§8▪ §fTiêu hao: §b" + mana + " Năng lượng");
        lore.add("§8▪ §fHồi chiêu: §a" + cd + "s");
        lore.add("§8▪ §fST Đạn đập trúng: §c" + projDmgPct + "%");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");

        switch (element) {
            case FIRE:
                lore.add("§c[Ma Pháp Trận: Hỏa Ngục]");
                lore.add("§f- Phát nổ 1 lần duy nhất gây §c" + (domDmgPct * 2) + "% ST§f.");
                lore.add("§f- Thiêu đốt mọi kẻ địch (Bán kính 4m).");
                break;
            case WATER:
                lore.add("§b[Ma Pháp Trận: Sóng Thần]");
                lore.add("§f- Kích nổ 3 đợt liên tiếp (Mỗi 1.5s).");
                lore.add("§f- Hất tung kẻ địch lên không trung.");
                break;
            case WIND:
                lore.add("§a[Ma Pháp Trận: Mắt Bão]");
                lore.add("§f- Tạo lõi lốc xoáy duy trì §a6s §f(Bán kính 4m).");
                lore.add("§f- Liên tục hút và gây ST lên kẻ địch.");
                break;
            case NATURE:
                lore.add("§2[Ma Pháp Trận: Khu Rừng Trói Buộc]");
                lore.add("§f- Khóa cứng kẻ địch trong §a3.5s §f(Bán kính 5m).");
                lore.add("§f- Rút sinh lực kẻ địch bị trói về cho bản");
                lore.add("§f  thân: Hồi §a0.5% Máu/s §f(Tối đa 8%).");
                break;
            case LIGHT:
                lore.add("§e[Ma Pháp Trận: Thánh Vực]");
                lore.add("§f- Tồn tại §a8s §f(Bán kính 6m).");
                lore.add("§f- Kẻ địch bước vào: Nhận thêm §c15% ST§f.");
                lore.add("§f- Bản thân bước vào: Giảm §a8% ST nhận vào§f.");
                break;
            case DARK:
                lore.add("§5[Ma Pháp Trận: Vực Thẳm Hư Không]");
                lore.add("§f- Sát thương nổ ban đầu giảm còn §c70%§f.");
                lore.add("§f- Mở Khe nứt tồn tại §a16s §f(Bán kính 5m).");
                lore.add("§f- Khe nứt liên tục phóng sét Hư không gây");
                lore.add("§f  §c100% ST vũ khí §flên kẻ địch mỗi 1.5s.");
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

    // ==========================================
    // LOGIC ĐƯỜNG ĐẠN BAY
    // ==========================================
    private void fireElementalSphere(Player player, RPGPlayer rpgPlayer, MageElement element, double projMulti, double domMulti) {
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(1.5);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            double distanceTraveled = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (distanceTraveled >= 15.0 || !player.isOnline()) {
                    triggerAirBurst(currentLoc, element);
                    this.cancel();
                    return;
                }

                org.bukkit.Color elColor = getElementColor(element);
                Particle.DustOptions dust = new Particle.DustOptions(elColor, 1.5f);

                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 8, 0.2, 0.2, 0.2, dust);

                // Va chạm Địa hình
                if (currentLoc.getBlock().getType().isSolid()) {
                    triggerDetonation(player, rpgPlayer, currentLoc, null, element, domMulti);
                    this.cancel();
                    return;
                }

                // Va chạm Thực thể (Đập thẳng mặt)
                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;

                        // Nổ DMG Đập Mặt trước
                        double hitDmg = CombatManager.calculateDamage(player, rpgPlayer, target, projMulti, 0, true, false);
                        CombatManager.applySkillDamage(player, target, hitDmg);

                        // Nở Lãnh Vực (Truyền target vào để lấy tọa độ chân)
                        triggerDetonation(player, rpgPlayer, currentLoc, target, element, domMulti);
                        this.cancel();
                        return;
                    }
                }

                currentLoc.add(direction);
                distanceTraveled += direction.length();
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
    // TỰ BẠO TRÊN KHÔNG (PHÁO HOA THEO MÀU NGUYÊN TỐ)
    // ==========================================
    private void triggerAirBurst(Location center, MageElement element) {
        center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1f);
        Particle.DustOptions dust = new Particle.DustOptions(getElementColor(element), 2.0f);

        // Nổ dạng hình cầu từ tâm
        for (int i = 0; i < 40; i++) {
            Vector v = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(random.nextDouble() * 2.0);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(v), 1, 0, 0, 0, dust);
        }
    }

    // ==========================================
    // TRẠM KÍCH NỔ / CHỌN TỌA ĐỘ
    // ==========================================
    private void triggerDetonation(Player player, RPGPlayer rpgPlayer, Location hitLoc, LivingEntity hitEntity, MageElement element, double domMulti) {
        Location center = hitLoc.clone();

        // PHÂN LOẠI 2 DẠNG KỸ NĂNG: Lãnh vực (Cần mặt đất) và Bùng nổ (Nổ ngay tâm)
        boolean isDomainType = (element == MageElement.LIGHT || element == MageElement.NATURE || element == MageElement.WATER);

        if (isDomainType) {
            if (hitEntity != null) {
                // Nếu dính người -> Lấy tọa độ dưới chân người đó
                center = hitEntity.getLocation();
            } else {
                // Nếu dính tường -> Rơi xuống tìm mặt đất
                for (int i = 0; i < 5; i++) {
                    if (center.getBlock().getType().isSolid()) {
                        center.add(0, 1, 0); // Đứng trên block
                        break;
                    }
                    center.subtract(0, 1, 0);
                }
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

    // ==========================================
    // 6 HIỆU ỨNG (REWORKED VFX TỪ TÂM LAN RA)
    // ==========================================

    // 1. HỎA NGỤC (Nổ lan từ tâm ra R=4)
    private void triggerFireExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2);

        new BukkitRunnable() {
            double r = 0;
            @Override
            public void run() {
                if (r > 4.0) { this.cancel(); return; }

                // Vẽ vòng lửa lan tỏa
                for (double t = 0; t < Math.PI * 2; t += Math.PI / 8) {
                    double x = r * Math.cos(t);
                    double z = r * Math.sin(t);
                    center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, 0.5, z), 2, 0.2, 0.2, 0.2, 0.05);
                }
                r += 1.0; // Lan cực nhanh trong 4 ticks
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.applyMarkOnly(target, MageElement.FIRE);
                target.setFireTicks(100);

                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 2.0, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
            }
        }
    }

    // 2. SÓNG THẦN (Hất thẳng lên trời mạnh mẽ)
    private void triggerWaterDomain(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave >= 3 || !player.isOnline()) { this.cancel(); return; }

                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1f, 0.8f);

                // Sóng lan từ tâm
                for (double r = 1; r <= 4; r++) {
                    for (double t = 0; t < Math.PI * 2; t += Math.PI / (r * 4)) {
                        double x = r * Math.cos(t);
                        double z = r * Math.sin(t);
                        center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(x, 0.2, z), 5, 0.2, 0.5, 0.2, 0.1);
                    }
                }

                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;
                        ReactionEngine.applyMarkOnly(target, MageElement.WATER);

                        // [ĐÃ SỬA]: Hất tung lên trời mạnh hơn
                        target.setVelocity(new Vector(0, 1.2, 0));

                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti, 0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                    }
                }
                wave++;
            }
        }.runTaskTimer(plugin, 0L, 30L);
    }

    // 3. MẮT BÃO (Vẽ Lốc Xoáy 3D)
    private void triggerWindExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        double dpsMulti = domMulti / 6.0;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 120 || !player.isOnline()) { this.cancel(); return; }

                // Vẽ Lốc xoáy (Tornado)
                double angle = ticks * 0.4;
                for (int i = 0; i < 4; i++) { // 4 tầng lốc
                    double y = i * 1.0;
                    double r = 1.0 + (y * 0.5); // Càng lên cao lốc càng to
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
                            ReactionEngine.applyMarkOnly(target, MageElement.WIND);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dpsMulti, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 4. KHU RỪNG TRÓI BUỘC (VFX Dây leo Xanh Đậm + Chặn KB)
    private void triggerNatureDomain(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_BREAK, 1f, 0.5f);

        List<LivingEntity> rootedTargets = new ArrayList<>();
        Particle.DustOptions vineColor = new Particle.DustOptions(org.bukkit.Color.fromRGB(34, 139, 34), 1.2f); // Xanh rừng rậm

        // Lan cỏ từ tâm ra
        for (double r = 1; r <= 5; r++) {
            for (double t = 0; t < Math.PI * 2; t += Math.PI / (r * 2)) {
                center.getWorld().spawnParticle(Particle.DUST, center.clone().add(r * Math.cos(t), 0.1, r * Math.sin(t)), 1, 0, 0, 0, vineColor);
            }
        }

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                rootedTargets.add(target);
                ReactionEngine.applyMarkOnly(target, MageElement.NATURE);
                StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 70, 10);

                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);

                // [FIX LỖI KNOCKBACK]: Ép tốc độ về 0 ngay sau khi nhận sát thương để chặn Vanilla KB
                new BukkitRunnable() {
                    @Override
                    public void run() { target.setVelocity(new Vector(0, -0.1, 0)); }
                }.runTaskLater(plugin, 1L);
            }
        }

        // Logic Trói & Hút Máu (3.5 giây = 70 ticks)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 70 || !player.isOnline()) { this.cancel(); return; } // ĐỒNG BỘ 70 TICKS

                // Vẽ dây leo quấn quanh người mục tiêu
                for (LivingEntity target : rootedTargets) {
                    if (target.isValid() && !target.isDead()) {
                        double y = (ticks * 0.1) % 2.0;
                        double angle = ticks * 0.5;
                        double x = 0.6 * Math.cos(angle);
                        double z = 0.6 * Math.sin(angle);
                        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(x, y, z), 1, 0, 0, 0, vineColor);
                    }
                }

                // Bắn tia hồi máu mỗi giây
                if (ticks % 20 == 0 && !rootedTargets.isEmpty()) {
                    int validCount = 0;
                    for (LivingEntity target : rootedTargets) {
                        if (target.isValid() && !target.isDead()) {
                            validCount++;
                            spawnHomingHealMissile(target.getLocation().add(0, 1, 0), player);
                        }
                    }
                    if (validCount > 0) {
                        double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                        double healRaw = maxHp * 0.005 * validCount;
                        double healCapped = Math.min(healRaw, maxHp * 0.08);
                        rpgPlayer.heal(healCapped);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnHomingHealMissile(Location startLoc, Player targetPlayer) {
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
                // Đã đổi thành HAPPY_VILLAGER (Hạt lấp lánh xanh lá)
                current.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, current, 2, 0.1, 0.1, 0.1, 0);
                lifespan++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 5. THÁNH VỰC (Bán kính 6m + Làn sóng lan ra)
    private void triggerLightDomain(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        // Làn sóng ánh sáng lan ra R=6
        new BukkitRunnable() {
            double r = 0;
            @Override
            public void run() {
                if (r > 6.0) { this.cancel(); return; }
                for (double t = 0; t < Math.PI * 2; t += Math.PI / 16) {
                    double x = r * Math.cos(t);
                    double z = r * Math.sin(t);
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0.05);
                }
                r += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.applyMarkOnly(target, MageElement.LIGHT);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 160) { this.cancel(); return; }

                // Viền Lãnh Vực R=6
                if (ticks % 5 == 0) {
                    for (double t = 0; t < Math.PI * 2; t += Math.PI / 12) {
                        double x = 6 * Math.cos(t);
                        double z = 6 * Math.sin(t);
                        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
                    }
                    double rX = (random.nextDouble() - 0.5) * 10;
                    double rZ = (random.nextDouble() - 0.5) * 10;
                    center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(rX, 0.2, rZ), 5, 0, 1.5, 0, 0.05);
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

    // 6. VỰC THẲM HƯ KHÔNG (Sét Đen mỏng, ngầu)
    private void triggerDarkExplosion(Player player, RPGPlayer rpgPlayer, Location center, double domMulti) {
        center.getWorld().spawnParticle(Particle.PORTAL, center, 100, 3.0, 3.0, 3.0, 0.5);

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;
                ReactionEngine.applyMarkOnly(target, MageElement.DARK);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, domMulti * 0.7, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);
            }
        }

        Location riftLoc = center.clone().add(0, 2, 0);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 320 || !player.isOnline()) { this.cancel(); return; }

                riftLoc.getWorld().spawnParticle(Particle.PORTAL, riftLoc, 15, 0.5, 0.5, 0.5, 0.1);
                if (ticks % 20 == 0) riftLoc.getWorld().playSound(riftLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f);

                if (ticks > 0 && ticks % 30 == 0) {
                    for (org.bukkit.entity.Entity ent : riftLoc.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            ReactionEngine.applyMarkOnly(target, MageElement.DARK);

                            // Vẽ tia sét đen
                            drawBlackLightning(riftLoc, target.getLocation().add(0, 1, 0), riftLoc.getWorld());
                            riftLoc.getWorld().playSound(riftLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.5f, 2.0f);

                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 1.0, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            break;
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Vẽ Sét Đen mỏng cong bằng DustOptions
    private void drawBlackLightning(Location start, Location end, org.bukkit.World world) {
        Vector dir = end.toVector().subtract(start.toVector());
        double dist = dir.length();
        if (dist == 0) return;
        dir.normalize();

        Location current = start.clone();
        int segments = (int) (dist * 3.0);
        double segmentLength = dist / segments;

        // Bụi màu Xám/Đen đậm pha Tím nhẹ
        Particle.DustOptions blackDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(20, 10, 30), 1.5f);

        for (int i = 0; i < segments; i++) {
            Vector offset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(0.3);
            Location next = start.clone().add(dir.clone().multiply((i + 1) * segmentLength)).add(offset);
            if (i == segments - 1) next = end.clone();

            Vector drawDir = next.toVector().subtract(current.toVector());
            double drawDist = drawDir.length();
            drawDir.normalize();
            for (double t = 0; t <= drawDist; t += 0.2) {
                world.spawnParticle(Particle.DUST, current.clone().add(drawDir.clone().multiply(t)), 1, 0, 0, 0, blackDust);
            }
            current = next;
        }
    }
}