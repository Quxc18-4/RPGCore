package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BloodSiphonSkill extends AbstractEnhancementSkill {

    public static final String META_UNSTABLE_ORBS = "Reaper_Unstable_Orbs";
    private static final Map<UUID, UnstableOrbitState> ACTIVE_ORBITS = new ConcurrentHashMap<>();

    public BloodSiphonSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "reaper_blood_siphon"; }
    @Override public String getName() { return "Đoạt Huyết"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.FERMENTED_SPIDER_EYE; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Map.of(); }

    @Override
    public double getManaCost(int level) {
        return 35.0 + (level * 2.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 14.0 - (level * 0.5); // 13.5s -> 9.0s
        return (long) (Math.max(7.0, cd) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);

        int dmgMulti = 150 + (displayLvl * 15); // 165% -> 300% ST Vũ Khí
        double healPct = 2.55 + (displayLvl * 0.45); // 3.0% -> 7.0% Max HP / viên

        return List.of(
                "§7Phóng một tia Huyết Hồn vô hình xuyên thấu",
                "§7tới mục tiêu phía trước (Tầm xa 15 block).",
                "",
                "§e[Thi Triển - Đoạt Huyết]",
                "§f- Gây §c" + dmgMulti + "% ST Vũ Khí§f lên mục tiêu và",
                "§f  rút ra §c3 Huyết Cầu§f bay ngược về cơ thể.",
                "§f- Mỗi Huyết Cầu hồi §a" + String.format("%.1f", healPct) + "% Máu Tối Đa§f khi chạm vào.",
                "",
                "§4[Nội Tại - Huyết Cầu Bất Ổn]",
                "§f- Nếu máu đã đầy §a100%§f, Huyết Cầu bay về sẽ",
                "§f  chuyển sang trạng thái §cBất Ổn§f xoay quanh người",
                "§f  trong §c12 giây§f (Tối đa 3 viên).",
                "§f- Đòn đánh thường sẽ phóng §c1 Huyết Cầu§f phát nổ",
                "§f  hình cầu, gây sát thương bằng §c50% ST đã hút§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                double timeLeft = (player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực để xuất chiêu!");
            return false;
        }

        World world = player.getWorld();
        if (world == null) return false;

        // ==========================================
        // 1. TIA RAY TRACING VÔ HÌNH TÌM MỤC TIÊU
        // ==========================================
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        RayTraceResult rayTrace = world.rayTraceEntities(
                eyeLoc,
                direction,
                15.0,
                0.5,
                e -> e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)
        );

        if (rayTrace == null || !(rayTrace.getHitEntity() instanceof LivingEntity victim)) {
            player.sendMessage("§c§l[!] §fKhông có mục tiêu nào trong tầm ngắm!");
            return false;
        }

        // ==========================================
        // 2. THANH TOÁN TÀI NGUYÊN & GÂY SÁT THƯƠNG GỐC
        // ==========================================
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        double baseDmgMulti = 1.50 + (level * 0.15); // 165% -> 300%
        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, baseDmgMulti, 0.0, false, false);

        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
        CombatManager.applySkillDamage(player, victim, finalDmg);
        player.removeMetadata("Ghost_L_Blocker", plugin);

        // Hiệu ứng tia linh hồn mờ khi găm trúng mục tiêu
        renderSoulRayTrace(eyeLoc.clone().subtract(0, 0.15, 0), victim.getLocation().add(0, 1.0, 0));
        world.playSound(victim.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.0f, 1.5f);
        world.playSound(victim.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.9f, 0.6f);

        // ==========================================
        // 3. RÚT RA 3 VIÊN BI MÁU RIÊNG BIỆT BAY VỀ
        // ==========================================
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double healPerOrb = maxHp * (0.0255 + (level * 0.0045)); // 3.0% -> 7.0% Max HP
        double unstableExplosionDmg = finalDmg * 0.50; // 1/2 sát thương đã gây cho mục tiêu

        spawnReturningOrbsTask(player, rpgPlayer, victim.getLocation().add(0, 1.0, 0), healPerOrb, unstableExplosionDmg);
        return true;
    }

    private void renderSoulRayTrace(Location start, Location end) {
        World world = start.getWorld();
        if (world == null) return;

        Vector diff = end.toVector().subtract(start.toVector());
        double dist = diff.length();
        if (dist <= 0.1) return;

        Vector step = diff.normalize().multiply(0.4);
        Location point = start.clone();
        Particle.DustOptions soulDust = new Particle.DustOptions(Color.fromRGB(160, 15, 35), 0.75f);

        for (double d = 0; d < dist; d += 0.4) {
            world.spawnParticle(Particle.DUST, point, 1, 0.02, 0.02, 0.02, 0, soulDust);
            point.add(step);
        }
    }

    // ==========================================
    // 3 THỰC THỂ BI MÁU TÁCH RA & BAY NGƯỢC VỀ NGƯỜI CHƠI
    // ==========================================
    private void spawnReturningOrbsTask(Player player, RPGPlayer rpgPlayer, Location victimOrigin,
                                        double healPerOrb, double explosionDmgPerOrb) {
        Vector toPlayer = player.getLocation().add(0, 1.1, 0).toVector().subtract(victimOrigin.toVector());
        if (toPlayer.lengthSquared() < 1e-4) {
            toPlayer = player.getLocation().getDirection().multiply(-1);
        }
        toPlayer.normalize();

        Vector up = new Vector(0, 1, 0);
        Vector right = toPlayer.clone().crossProduct(up);
        if (right.lengthSquared() < 1e-4) {
            right = toPlayer.clone().crossProduct(new Vector(1, 0, 0));
        }
        right.normalize();
        up = right.clone().crossProduct(toPlayer).normalize();

        // 3 hướng bung tách biệt (Trái-trên, Phải-trên, Đỉnh-sau) để thấy rõ 3 thực thể riêng biệt
        Vector[] burstDirs = new Vector[]{
                right.clone().multiply(0.85).add(up.clone().multiply(0.55)).subtract(toPlayer.clone().multiply(0.3)).normalize(),
                right.clone().multiply(-0.85).add(up.clone().multiply(0.55)).subtract(toPlayer.clone().multiply(0.3)).normalize(),
                up.clone().multiply(0.95).subtract(toPlayer.clone().multiply(0.4)).normalize()
        };

        final List<ReturningOrb> orbs = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            // Tốc độ thu hồi hơi lệch nhẹ giữa 3 viên để đảm bảo thứ tự chạm người cực kỳ mượt mà
            double homingSpeed = 0.85 + (i * 0.07);
            orbs.add(new ReturningOrb(victimOrigin.clone(), burstDirs[i].multiply(0.55), homingSpeed));
        }

        final Particle.DustOptions coreRed = new Particle.DustOptions(Color.fromRGB(235, 20, 45), 1.35f);
        final Particle.DustOptions outerDark = new Particle.DustOptions(Color.fromRGB(110, 0, 15), 0.95f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks > 120) {
                    this.cancel();
                    return;
                }

                World world = player.getWorld();
                Location chestLoc = player.getLocation().add(0, 1.1, 0);
                boolean anyRemaining = false;

                for (ReturningOrb orb : orbs) {
                    if (orb.absorbed) continue;
                    if (!world.equals(orb.loc.getWorld())) {
                        orb.absorbed = true;
                        continue;
                    }

                    anyRemaining = true;

                    // 6 tick đầu bung tỏa ra ngoài, sau đó bẻ lái hút về ngực người chơi
                    if (ticks < 6) {
                        orb.loc.add(orb.vel);
                        orb.vel.multiply(0.85);
                    } else {
                        Vector dir = chestLoc.toVector().subtract(orb.loc.toVector());
                        double dist = dir.length();

                        if (dist <= 1.05) {
                            orb.absorbed = true;
                            handleOrbArrival(player, rpgPlayer, healPerOrb, explosionDmgPerOrb);
                            continue;
                        }

                        Vector desired = dir.normalize().multiply(orb.homingSpeed);
                        orb.vel = orb.vel.multiply(0.55).add(desired.multiply(0.45));
                        orb.loc.add(orb.vel);
                    }

                    // Render thực thể bi máu
                    world.spawnParticle(Particle.DUST, orb.loc, 3, 0.04, 0.04, 0.04, 0, coreRed);
                    world.spawnParticle(Particle.DUST, orb.loc, 1, 0.08, 0.08, 0.08, 0, outerDark);
                }

                if (!anyRemaining) {
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // XỬ LÝ TUẦN TỰ KHI TỪNG VIÊN CHẠM VÀO NGƯỜI CHƠI
    // ==========================================
    private void handleOrbArrival(Player player, RPGPlayer rpgPlayer, double healPerOrb, double explosionDmg) {
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double currentHp = player.getHealth();
        Location chestLoc = player.getLocation().add(0, 1.1, 0);

        // Chưa đầy 100% Máu -> Ưu tiên hồi máu trước
        if (currentHp < maxHp - 0.05) {
            rpgPlayer.heal(healPerOrb);
            player.getWorld().playSound(chestLoc, Sound.ENTITY_WITCH_DRINK, 0.6f, 1.8f);
            player.getWorld().spawnParticle(Particle.HEART, chestLoc.clone().add(0, 0.4, 0), 2, 0.25, 0.25, 0.25, 0);
        } else {
            // Đã đầy 100% Máu -> Chuyển vào trạng thái Bất Ổn xoay quanh cơ thể (12s)
            addUnstableOrb(player, explosionDmg);
        }
    }

    private void addUnstableOrb(Player player, double explosionDmg) {
        UUID uuid = player.getUniqueId();
        long expireAt = System.currentTimeMillis() + 12_000L;

        UnstableOrbitState state = ACTIVE_ORBITS.computeIfAbsent(uuid, k -> new UnstableOrbitState());
        if (state.orbDamages.size() >= 3) {
            state.orbDamages.pollFirst(); // Giữ tối đa 3 viên xoay quanh người
        }
        state.orbDamages.addLast(explosionDmg);
        state.expireTimestamp = expireAt;

        player.setMetadata(META_UNSTABLE_ORBS, new FixedMetadataValue(plugin, state.orbDamages.size()));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.7f);

        if (!state.taskRunning) {
            state.taskRunning = true;
            startUnstableOrbitTask(player, uuid, state);
        }
    }

    private void startUnstableOrbitTask(Player player, UUID uuid, UnstableOrbitState state) {
        final Particle.DustOptions unstableCore = new Particle.DustOptions(Color.fromRGB(255, 30, 60), 1.45f);
        final Particle.DustOptions unstableAura = new Particle.DustOptions(Color.fromRGB(45, 0, 10), 1.0f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || state.orbDamages.isEmpty()
                        || System.currentTimeMillis() >= state.expireTimestamp) {
                    ACTIVE_ORBITS.remove(uuid);
                    if (player.isOnline()) {
                        player.removeMetadata(META_UNSTABLE_ORBS, plugin);
                        if (!state.orbDamages.isEmpty() && !player.isDead()) {
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.4f);
                        }
                    }
                    state.orbDamages.clear();
                    state.taskRunning = false;
                    this.cancel();
                    return;
                }

                int count = state.orbDamages.size();
                Location center = player.getLocation().add(0, 1.05, 0);
                double radius = 1.15;
                double baseAngle = ticks * 0.18;

                for (int i = 0; i < count; i++) {
                    double angle = baseAngle + (i * (2.0 * Math.PI / count));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(ticks * 0.25 + i) * 0.12;

                    Location orbLoc = center.clone().add(x, y, z);
                    player.getWorld().spawnParticle(Particle.DUST, orbLoc, 2, 0.03, 0.03, 0.03, 0, unstableCore);
                    player.getWorld().spawnParticle(Particle.DUST, orbLoc, 1, 0.06, 0.06, 0.06, 0, unstableAura);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // HOOK CHO ĐÒN ĐÁNH THƯỜNG: PHÓNG 1 BI & NỔ HÌNH CẦU
    // ==========================================
    public static void triggerUnstableOrbAttack(JavaPlugin plugin, Player attacker, LivingEntity primaryTarget) {
        UUID uuid = attacker.getUniqueId();
        UnstableOrbitState state = ACTIVE_ORBITS.get(uuid);
        if (state == null || state.orbDamages.isEmpty() || System.currentTimeMillis() >= state.expireTimestamp) {
            ACTIVE_ORBITS.remove(uuid);
            attacker.removeMetadata(META_UNSTABLE_ORBS, plugin);
            return;
        }

        Double explosionDmg = state.orbDamages.pollFirst();
        if (explosionDmg == null) return;

        if (state.orbDamages.isEmpty()) {
            ACTIVE_ORBITS.remove(uuid);
            attacker.removeMetadata(META_UNSTABLE_ORBS, plugin);
        } else {
            attacker.setMetadata(META_UNSTABLE_ORBS, new FixedMetadataValue(plugin, state.orbDamages.size()));
        }

        launchExplosionOrbTask(plugin, attacker, primaryTarget, explosionDmg);
    }

    private static void launchExplosionOrbTask(JavaPlugin plugin, Player attacker, LivingEntity target, double explosionDmg) {
        final Location currentLoc = attacker.getLocation().add(0, 1.15, 0);
        final Location fallbackTargetLoc = target.getLocation().add(0, 1.0, 0);
        final Particle.DustOptions missileRed = new Particle.DustOptions(Color.fromRGB(255, 25, 50), 1.5f);
        final Particle.DustOptions missileDark = new Particle.DustOptions(Color.fromRGB(90, 0, 10), 1.0f);

        attacker.getWorld().playSound(currentLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final double speed = 1.35;

            @Override
            public void run() {
                if (!attacker.isOnline() || ticks > 40) {
                    this.cancel();
                    return;
                }

                Location aimLoc = (target.isValid() && !target.isDead() && target.getWorld().equals(currentLoc.getWorld()))
                        ? target.getLocation().add(0, target.getHeight() * 0.5, 0)
                        : fallbackTargetLoc;

                if (!aimLoc.getWorld().equals(currentLoc.getWorld())) {
                    this.cancel();
                    return;
                }

                Vector diff = aimLoc.toVector().subtract(currentLoc.toVector());
                double dist = diff.length();

                if (dist <= 1.1) {
                    detonateBloodSphere(plugin, attacker, aimLoc, explosionDmg);
                    this.cancel();
                    return;
                }

                currentLoc.add(diff.normalize().multiply(Math.min(speed, dist)));
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 3, 0.05, 0.05, 0.05, 0, missileRed);
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 2, 0.08, 0.08, 0.08, 0, missileDark);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void detonateBloodSphere(JavaPlugin plugin, Player attacker, Location center, double explosionDmg) {
        World world = center.getWorld();
        if (world == null) return;

        final double explosionRadius = 3.0;

        // Âm thanh & hiệu ứng vụ nổ máu hình cầu 3D
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.85f, 1.45f);
        world.spawnParticle(Particle.BLOCK_CRUMBLE, center, 35, 1.2, 1.2, 1.2, Material.REDSTONE_BLOCK.createBlockData());

        Particle.DustOptions sphereDust = new Particle.DustOptions(Color.fromRGB(220, 15, 40), 1.3f);
        int samples = 48;
        double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // Phân bố điểm đều trên mặt cầu (Fibonacci Sphere)
        for (int i = 0; i < samples; i++) {
            double y = 1.0 - (i / (double) (samples - 1)) * 2.0;
            double radiusAtY = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = phi * i;

            double x = Math.cos(theta) * radiusAtY * explosionRadius;
            double z = Math.sin(theta) * radiusAtY * explosionRadius;
            world.spawnParticle(Particle.DUST, center.clone().add(x, y * explosionRadius, z), 1, 0, 0, 0, 0, sphereDust);
        }

        // Gây sát thương hình cầu cho kẻ địch xung quanh
        for (Entity e : world.getNearbyEntities(center, explosionRadius, explosionRadius, explosionRadius)) {
            if (e instanceof LivingEntity victim && !e.equals(attacker) && CombatManager.canTarget(attacker, e)) {
                if (victim.getLocation().add(0, victim.getHeight() * 0.5, 0).distanceSquared(center) <= (explosionRadius + 0.6) * (explosionRadius + 0.6)) {
                    attacker.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                    CombatManager.applySkillDamage(attacker, victim, explosionDmg);
                    attacker.removeMetadata("Ghost_L_Blocker", plugin);
                }
            }
        }
    }

    private static class ReturningOrb {
        final Location loc;
        Vector vel;
        final double homingSpeed;
        boolean absorbed = false;

        ReturningOrb(Location loc, Vector vel, double homingSpeed) {
            this.loc = loc;
            this.vel = vel;
            this.homingSpeed = homingSpeed;
        }
    }

    private static class UnstableOrbitState {
        final Deque<Double> orbDamages = new ArrayDeque<>(3);
        long expireTimestamp = 0L;
        boolean taskRunning = false;
    }
}