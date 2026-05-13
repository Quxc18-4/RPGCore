package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class UltimateElementGate implements ISkill {

    private final JavaPlugin plugin;
    private final Map<MageElement, Integer> elementModelIds = new HashMap<>();
    private final Random random = new Random();

    public UltimateElementGate(JavaPlugin plugin) {
        this.plugin = plugin;
        elementModelIds.put(MageElement.FIRE, 1001);
        elementModelIds.put(MageElement.WATER, 1002);
        elementModelIds.put(MageElement.WIND, 1003);
        elementModelIds.put(MageElement.NATURE, 1004);
        elementModelIds.put(MageElement.LIGHT, 1005);
        elementModelIds.put(MageElement.DARK, 1006);
    }

    @Override public String getId() { return "mage_ultimate_element_gate"; }
    @Override public String getName() { return "§6§l[TUYỆT KỸ TỐI THƯỢNG: VẠN PHÁP QUY TÔNG]"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.END_CRYSTAL; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 50; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 150.0 + (level * 50.0); }
    @Override public long getCooldown(int level) { return (65 - (level * 5)) * 1000L; }

    private double getBulletBaseDamage(int level) { return 2.5 + (level * 0.3); }

    @Override
    public List<String> getDescription(int level) {
        int bulletDmg = (int) (getBulletBaseDamage(level) * 100);
        return Arrays.asList(
                "§6§o\"Trời đất cuồng nộ, vạn pháp tuôn trào...\"",
                "",
                "§f§l[THÔNG SỐ TUYỆT KỸ]",
                "§7Khởi động trận pháp ma thuật cực đại phía trước mặt.",
                "§7Các cổng không gian sẽ tự động khóa mục tiêu và nã pháo.",
                "",
                "§f▪ Thời gian duy trì: §a5 giây",
                "§f▪ Tốc độ xả đạn: §e0.3 giây / Viên",
                "§f▪ Sát thương đạn: §c" + bulletDmg + "% §fMỗi viên",
                "",
                "§e§l❂ CÔNG NGHỆ SIÊU GIA TỐC ❂",
                "§7Đạn đạo đạt vận tốc tối đa ngay sau khi rời nòng,",
                "§7không thể né tránh, không thể cản phá.",
                "",
                "§8§o*Vạn vật định hình, ma lực vĩnh hằng."
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

        player.sendMessage("§6§l[!] VẠN PHÁP QUY TÔNG: KHỞI ĐỘNG!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

        double baseBulletDmg = getBulletBaseDamage(level);
        List<ItemDisplay> activeGates = new ArrayList<>();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;
            final LinkedList<MageElement> recentElements = new LinkedList<>();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    activeGates.forEach(ItemDisplay::remove);
                    activeGates.clear();
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) { this.cancel(); return; }

                if (ticks % 6 == 0) {
                    MageElement element = getSmartComboElement(recentElements, unlockedElements);

                    Location eyeLoc = player.getEyeLocation();
                    Vector lookDir = eyeLoc.getDirection().normalize();
                    Location lockedTarget = findTarget(player);

                    // ==========================================
                    // THUẬT TOÁN TÌM VỊ TRÍ CHIẾN THUẬT (TRÁNH CHỒNG LẤP & NÉ TÂM)
                    // ==========================================
                    Location gateLoc = null;
                    int attempts = 0;

                    while (attempts < 15) { // Thử tối đa 15 lần để tìm chỗ trống
                        // Random góc lệch Yaw (-45 đến 45) và Pitch (-35 đến 35)
                        double rYaw = (random.nextDouble() - 0.5) * 90;
                        double rPitch = (random.nextDouble() - 0.5) * 70;

                        // [NÉ TÂM]: Nếu góc lệch quá nhỏ (nằm ngay chính giữa), bỏ qua thử lại
                        if (Math.abs(rYaw) < 18 && Math.abs(rPitch) < 12) {
                            attempts++; continue;
                        }

                        // Khoảng cách kéo gần 20%: 2.2 - 4.5 block
                        double dist = 2.2 + random.nextDouble() * 2.3;

                        Location candidate = eyeLoc.clone();
                        Vector spawnDir = lookDir.clone();
                        rotateVector(spawnDir, rYaw, rPitch);
                        candidate.add(spawnDir.multiply(dist));

                        // [CHỐNG CHỒNG LẤP]: Kiểm tra khoảng cách với các cổng hiện có
                        boolean tooClose = false;
                        for (ItemDisplay other : activeGates) {
                            if (other.isValid() && other.getLocation().distance(candidate) < 1.6) {
                                tooClose = true;
                                break;
                            }
                        }

                        if (!tooClose) {
                            gateLoc = candidate;
                            break;
                        }
                        attempts++;
                    }

                    // Nếu sau 15 lần vẫn không tìm được chỗ (quá chật), lấy đại vị trí cuối cùng
                    if (gateLoc == null) return;

                    // Chống lún đất
                    if (gateLoc.getBlock().getType().isSolid()) gateLoc.add(0, 1.2, 0);

                    // Hướng mặt về mục tiêu
                    gateLoc.setDirection(lockedTarget.toVector().subtract(gateLoc.toVector()).normalize());

                    // TRIỆU HỒI
                    ItemDisplay gate = player.getWorld().spawn(gateLoc, ItemDisplay.class);
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    meta.setCustomModelData(elementModelIds.get(element));
                    item.setItemMeta(meta);
                    gate.setItemStack(item);
                    gate.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    gate.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);

                    activeGates.add(gate);

                    // ANIMATION TÂM CHUẨN (Fix Pivot)
                    float gateScale = 1.6f + random.nextFloat() * 1.0f;
                    Transformation trans = gate.getTransformation();
                    trans.getScale().set(0.001f, 0.001f, 0.001f);
                    trans.getTranslation().set(0, 0, 0); // Đưa về 0 để Minecraft tự hiểu pivot center
                    gate.setTransformation(trans);

                    // NỞ RA
                    gate.setInterpolationDelay(0);
                    gate.setInterpolationDuration(5);
                    trans.getScale().set(gateScale, gateScale, gateScale);
                    gate.setTransformation(trans);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!gate.isValid()) return;

                            fireHyperFastBullet(player, rpgPlayer, gate.getLocation(), lockedTarget, element, baseBulletDmg);

                            // THU VÀO TÂM
                            gate.setInterpolationDelay(0);
                            gate.setInterpolationDuration(5);
                            Transformation shrink = gate.getTransformation();
                            shrink.getScale().set(0.001f, 0.001f, 0.001f);
                            gate.setTransformation(shrink);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    activeGates.remove(gate);
                                    gate.remove();
                                }
                            }.runTaskLater(plugin, 5L);
                        }
                    }.runTaskLater(plugin, 10L);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void rotateVector(Vector v, double yawDegrees, double pitchOffset) {
        double yaw = Math.toRadians(-yawDegrees);
        double pitch = Math.toRadians(-pitchOffset);

        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double x = v.getX() * cosY - v.getZ() * sinY;
        double z = v.getX() * sinY + v.getZ() * cosY;

        // Cập nhật X, Z và sau đó xử lý Pitch bằng cách cộng vector UP
        v.setX(x).setZ(z);
        Vector up = new Vector(0, 1, 0).multiply(Math.tan(pitch));
        v.add(up).normalize();
    }

    private Location findTarget(Player p) {
        // Tăng tầm bắn lên 120 blocks
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 120);
        return (ray != null && ray.getHitPosition() != null) ? ray.getHitPosition().toLocation(p.getWorld()) : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(120));
    }

    private void fireHyperFastBullet(Player shooter, RPGPlayer rpgPlayer, Location start, Location end, MageElement element, double dmg) {
        shooter.getWorld().playSound(start, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2.0f);

        Vector p0 = start.toVector();
        Vector p2 = end.toVector();
        Vector dir = p2.clone().subtract(p0).normalize();

        // Offset 10 và đẩy điểm uốn xa 45% để tạo đường cong Fern-style
        Vector sideOffset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(10.0);
        Vector p1 = p0.clone().add(dir.clone().multiply(p0.distance(p2) * 0.45)).add(sideOffset);

        new BukkitRunnable() {
            double currentT = 0.0;
            int lifeTicks = 0;

            @Override
            public void run() {
                if (currentT >= 1.0 || lifeTicks > 100) {
                    impact(end, shooter, rpgPlayer, element, dmg);
                    this.cancel();
                    return;
                }

                double oldT = currentT;
                // Tốc độ: Khởi đầu 0.2, gia tốc 3.0 (Cực nhanh)
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

    private MageElement getSmartComboElement(LinkedList<MageElement> recent, List<MageElement> available) {
        List<MageElement> choices = new ArrayList<>(available);
        if (recent.size() >= 1) choices.remove(recent.getLast());
        MageElement chosen = choices.get(random.nextInt(choices.size()));
        recent.add(chosen);
        if (recent.size() > 3) recent.removeFirst();
        return chosen;
    }

    private org.bukkit.Particle.DustOptions getCoreColor(MageElement e) {
        switch (e) {
            case FIRE: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
            case WATER: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.AQUA, 1.5f);
            case WIND: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
            case NATURE: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.LIME, 1.5f);
            case LIGHT: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
            case DARK: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f);
            default: return new org.bukkit.Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
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