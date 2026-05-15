package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class UltimateElementGate implements ISkill {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public UltimateElementGate(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_ultimate_element_gate"; }
    @Override public String getName() { return "§6§l[TUYỆT KỸ TỐI THƯỢNG: MA PHÁP HỦY DIỆT]"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.END_CRYSTAL; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 50; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 150.0 + (level * 50.0); }
    @Override public long getCooldown(int level) { return (65 - (level * 5)) * 1000L; }

    // ==========================================
    // CÂN BẰNG SÁT THƯƠNG CHUẨN (GIÁM ĐỐC DUYỆT)
    // Cấp 1: 120% | Cấp 2: 150% | Cấp 3: 180%
    // Tổng 34 viên đạn trong 5 giây.
    // ==========================================
    private double getBulletBaseDamage(int level) {
        // 1.2 (120%) + (level - 1) * 0.3
        return 1.2 + (level - 1) * 0.3;
    }

    @Override
    public List<String> getDescription(int level) {
        double bulletDmgMulti = getBulletBaseDamage(level);
        int bulletDmgPercent = (int) Math.round(bulletDmgMulti * 100);

        return Arrays.asList(
                "§6§o\"Ma tố hội tụ, vạn pháp tuôn trào...\"",
                "",
                "§f§l[THÔNG SỐ TUYỆT KỸ]",
                "§7Khởi động trận pháp ma thuật cực đại rực sáng không gian.",
                "§7Triệu hồi vô số ma pháp trận liên tục tấn công về phía trước.",
                "",
                "§f▪ Thời gian duy trì: §a5 giây",
                "§f▪ Sát thương mỗi viên: §c" + bulletDmgPercent + "% §fSát thương",
                "",
                "§e§l❂ TINH TÚ MA PHÁP ❂",
                "§7Mỗi viên đạn bắn ra đều có khả năng kích hoạt §dPhản Ứng Nguyên Tố§7.",
                "§7Càng lĩnh hội nhiều nguyên tố, ma pháp trận càng trở nên đa dạng.",
                "",
                "§8§o(Sức mạnh của trí tuệ vượt xa mọi lưỡi gươm)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        List<String> unlockedElementNames = rpgPlayer.getUnlockedElements();
        if (unlockedElementNames == null || unlockedElementNames.isEmpty()) {
            player.sendMessage("§cBạn chưa mở khóa bất kỳ nguyên tố nào!");
            return false;
        }

        List<MageElement> unlockedElements = new ArrayList<>();
        for (String elName : unlockedElementNames) {
            try { unlockedElements.add(MageElement.valueOf(elName.toUpperCase())); } catch (Exception ignored) {}
        }

        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;
        applyGlobalCooldown(player, rpgPlayer, level);

        player.sendMessage("§6§l[!] MA PHÁP HỦY DIỆT: KHỞI ĐỘNG!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

        double baseBulletDmg = getBulletBaseDamage(level);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;
            final LinkedList<MageElement> recentElements = new LinkedList<>();
            final List<Location> activeGateLocs = new ArrayList<>();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // [SỬA TỐC ĐỘ]: Bắn mỗi 3 tick (0.15s) -> Tổng cộng 34 viên đạn!
                if (ticks % 3 == 0) {
                    MageElement element = getSmartComboElement(recentElements, unlockedElements);

                    Location eyeLoc = player.getEyeLocation();
                    Vector lookDir = eyeLoc.getDirection().normalize();
                    Location lockedTarget = findTarget(player);

                    Location gateLoc = null;
                    int attempts = 0;
                    while (attempts < 30) {
                        double rYaw = (random.nextDouble() - 0.5) * 110;
                        double rPitch = (random.nextDouble() - 0.5) * 80;

                        if (Math.abs(rYaw) < 35 && Math.abs(rPitch) < 25) {
                            attempts++; continue;
                        }

                        double dist = 3.5 + random.nextDouble() * 2.5;
                        Location candidate = eyeLoc.clone();
                        Vector spawnDir = lookDir.clone();
                        rotateVector(spawnDir, rYaw, rPitch);
                        candidate.add(spawnDir.multiply(dist));

                        boolean overlap = false;
                        for (Location activeLoc : activeGateLocs) {
                            if (activeLoc.distance(candidate) < 2.0) {
                                overlap = true; break;
                            }
                        }

                        if (!overlap && !candidate.getBlock().getType().isSolid()) {
                            gateLoc = candidate; break;
                        }
                        attempts++;
                    }

                    if (gateLoc == null) return;

                    activeGateLocs.add(gateLoc);
                    Vector faceDir = lockedTarget.toVector().subtract(gateLoc.toVector()).normalize();
                    gateLoc.setDirection(faceDir);

                    final Location finalGateLoc = gateLoc.clone();
                    final float finalScale = 0.8f + random.nextFloat() * 0.4f;

                    new BukkitRunnable() {
                        int gateTicks = 0;
                        @Override
                        public void run() {
                            if (gateTicks > 15) {
                                activeGateLocs.remove(finalGateLoc);
                                this.cancel();
                                return;
                            }

                            // [ĐÃ SỬA ẢO GIÁC ĐỒNG TÂM]: Không vẽ hạt trong lúc đang scale
                            // Chỉ vẽ đúng hình dáng vòng và sao khi cổng đã đạt đỉnh (Từ tick 5 đến 10)
                            if (gateTicks >= 5 && gateTicks <= 10) {
                                if (gateTicks % 2 == 0) { // Vẽ 3 lần để giữ cho nó sáng rực
                                    drawMagicCircleParticles(finalGateLoc, element, finalScale);
                                }
                            }

                            // KHAI HỎA Ở TICK 8
                            if (gateTicks == 8) {
                                fireHyperFastBullet(player, rpgPlayer, finalGateLoc, lockedTarget, element, baseBulletDmg);
                            }

                            gateTicks++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==========================================
    // THUẬT TOÁN VẼ MA PHÁP TRẬN (SẮC NÉT - CHUẨN NGÔI SAO LỤC GIÁC)
    // ==========================================
    private void drawMagicCircleParticles(Location loc, MageElement element, float scale) {
        Vector dir = loc.getDirection().normalize();
        Vector v1 = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        if (v1.lengthSquared() < 0.01) v1 = new Vector(1, 0, 0);
        Vector v2 = dir.clone().crossProduct(v1).normalize();

        Particle.DustOptions dust = getThinElementDust(element);
        double radius = 1.0 * scale;

        // 1. Vẽ vòng tròn ngoài cùng (60 hạt rải đều -> siêu mịn)
        int circlePoints = 60;
        for (int i = 0; i < circlePoints; i++) {
            double angle = (i * Math.PI * 2) / circlePoints;
            Location p = loc.clone().add(v1.clone().multiply(Math.cos(angle) * radius))
                    .add(v2.clone().multiply(Math.sin(angle) * radius));
            loc.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, dust);
        }

        // 2. Vẽ Ngôi sao 6 cánh (Đỉnh chạm đúng vào vòng tròn ngoài)
        drawEquilateralTriangle(loc, v1, v2, radius, 0, dust);
        drawEquilateralTriangle(loc, v1, v2, radius, Math.PI, dust);
    }

    private void drawEquilateralTriangle(Location center, Vector v1, Vector v2, double radius, double rotation, Particle.DustOptions dust) {
        Location p1 = center.clone().add(v1.clone().multiply(Math.cos(rotation) * radius)).add(v2.clone().multiply(Math.sin(rotation) * radius));
        Location p2 = center.clone().add(v1.clone().multiply(Math.cos(rotation + 2 * Math.PI / 3) * radius)).add(v2.clone().multiply(Math.sin(rotation + 2 * Math.PI / 3) * radius));
        Location p3 = center.clone().add(v1.clone().multiply(Math.cos(rotation + 4 * Math.PI / 3) * radius)).add(v2.clone().multiply(Math.sin(rotation + 4 * Math.PI / 3) * radius));

        // Vẽ 3 cạnh của tam giác
        drawLine(p1, p2, dust);
        drawLine(p2, p3, dust);
        drawLine(p3, p1, dust);
    }

    private void drawLine(Location start, Location end, Particle.DustOptions dust) {
        double length = start.distance(end);
        // [SỬA MẬT ĐỘ]: 15 hạt cho mỗi 1 block -> Đảm bảo cạnh ngôi sao liền mạch như 1 đường kẻ!
        int points = (int) (length * 15);
        if (points <= 0) return;

        Vector link = end.toVector().subtract(start.toVector()).normalize().multiply(length / points);
        Location curr = start.clone();
        for (int i = 0; i <= points; i++) {
            start.getWorld().spawnParticle(Particle.DUST, curr, 1, 0, 0, 0, 0, dust);
            curr.add(link);
        }
    }

    private Particle.DustOptions getThinElementDust(MageElement element) {
        float thickness = 0.5f; // Nét vẽ mỏng vừa đủ nhìn, không bị cục
        switch (element) {
            case FIRE: return new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 60, 0), thickness);
            case WATER: return new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 150, 255), thickness);
            case WIND: return new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 255, 230), thickness);
            case NATURE: return new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 255, 50), thickness);
            case LIGHT: return new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 120), thickness);
            case DARK: return new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 0, 255), thickness);
            default: return new Particle.DustOptions(org.bukkit.Color.WHITE, thickness);
        }
    }

    // ==========================================
    // CÁC HÀM CÒN LẠI (GIỮ NGUYÊN LOGIC)
    // ==========================================
    private void fireHyperFastBullet(Player shooter, RPGPlayer rpgPlayer, Location start, Location end, MageElement element, double dmg) {
        shooter.getWorld().playSound(start, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2.0f);
        Vector p0 = start.toVector();
        Vector p2 = end.toVector();
        Vector dir = p2.clone().subtract(p0).normalize();
        Vector sideOffset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(8.0);
        Vector p1 = p0.clone().add(dir.clone().multiply(p0.distance(p2) * 0.4)).add(sideOffset);

        new BukkitRunnable() {
            double currentT = 0.0;
            int lifeTicks = 0;
            @Override
            public void run() {
                if (currentT >= 1.0 || lifeTicks > 100) {
                    impact(end, shooter, rpgPlayer, element, dmg);
                    this.cancel(); return;
                }
                double oldT = currentT;
                double currentSpeed = 0.2 + (3.0 * currentT * currentT);
                currentT += currentSpeed;
                if (currentT > 1.0) currentT = 1.0;

                Location loc = null;
                int steps = Math.max(5, (int) ((currentT - oldT) * 60));
                for (int i = 1; i <= steps; i++) {
                    double t = oldT + (currentT - oldT) * ((double) i / steps);
                    double invT = 1.0 - t;
                    Vector subVec = p0.clone().multiply(invT * invT).add(p1.clone().multiply(2 * invT * t)).add(p2.clone().multiply(t * t));
                    loc = subVec.toLocation(start.getWorld());
                    spawnBulletTrail(element, loc);
                    if (loc.getBlock().getType().isSolid()) { impact(loc, shooter, rpgPlayer, element, dmg); this.cancel(); return; }
                }
                if (loc != null) {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0, getCoreColor(element));
                    for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                            impact(loc, shooter, rpgPlayer, element, dmg);
                            this.cancel(); return;
                        }
                    }
                }
                lifeTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void impact(Location loc, Player shooter, RPGPlayer rpgPlayer, MageElement element, double dmg) {
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
        spawnExplosionParticle(element, loc);
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 4.5, 4.5, 4.5)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                LivingEntity target = (LivingEntity) e;
                ReactionEngine.ReactionResult result = ReactionEngine.processHit(shooter, target, element);
                shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                double calcDmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, dmg * result.damageMultiplier, 0, 100.0, 0, true, false);
                CombatManager.applySkillDamage(shooter, target, calcDmg);
                shooter.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }

    private Location findTarget(Player p) {
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 65);
        return (ray != null && ray.getHitPosition() != null) ? ray.getHitPosition().toLocation(p.getWorld()) : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(65));
    }

    private MageElement getSmartComboElement(LinkedList<MageElement> recent, List<MageElement> available) {
        List<MageElement> choices = new ArrayList<>(available);
        if (recent.size() >= 1) choices.remove(recent.getLast());
        MageElement chosen = choices.get(random.nextInt(choices.size()));
        recent.add(chosen);
        if (recent.size() > 3) recent.removeFirst();
        return chosen;
    }

    private void rotateVector(Vector v, double yawDegrees, double pitchOffset) {
        double yaw = Math.toRadians(-yawDegrees);
        double pitch = Math.toRadians(-pitchOffset);
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double x = v.getX() * cosY - v.getZ() * sinY;
        double z = v.getX() * sinY + v.getZ() * cosY;
        v.setX(x).setZ(z);
        v.add(new Vector(0, 1, 0).multiply(Math.tan(pitch))).normalize();
    }

    private Particle.DustOptions getCoreColor(MageElement e) {
        switch (e) {
            case FIRE: return new Particle.DustOptions(Color.RED, 1.5f);
            case WATER: return new Particle.DustOptions(Color.AQUA, 1.5f);
            case WIND: return new Particle.DustOptions(Color.WHITE, 1.5f);
            case NATURE: return new Particle.DustOptions(Color.LIME, 1.5f);
            case LIGHT: return new Particle.DustOptions(Color.YELLOW, 1.5f);
            case DARK: return new Particle.DustOptions(Color.PURPLE, 1.5f);
            default: return new Particle.DustOptions(Color.WHITE, 1.5f);
        }
    }

    private void spawnBulletTrail(MageElement e, Location l) {
        Particle p = (e == MageElement.FIRE) ? Particle.FLAME : (e == MageElement.WATER) ? Particle.SPLASH : (e == MageElement.WIND) ? Particle.SNOWFLAKE : (e == MageElement.NATURE) ? Particle.CHERRY_LEAVES : (e == MageElement.LIGHT) ? Particle.GLOW : Particle.SQUID_INK;
        l.getWorld().spawnParticle(p, l, 2, 0.05, 0.05, 0.05, 0.01);
    }

    private void spawnExplosionParticle(MageElement e, Location l) {
        Particle p = (e == MageElement.FIRE) ? Particle.LAVA : (e == MageElement.WATER) ? Particle.SPLASH : (e == MageElement.WIND) ? Particle.CLOUD : (e == MageElement.NATURE) ? Particle.HAPPY_VILLAGER : (e == MageElement.LIGHT) ? Particle.END_ROD : Particle.SQUID_INK;
        l.getWorld().spawnParticle(p, l, 35, 1.5, 1.5, 1.5, 0.1);
    }
}