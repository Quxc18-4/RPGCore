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

    // Sát thương cao cho mỗi viên
    private double getBulletBaseDamage(int level) { return 2.5 + (level * 0.3); }

    @Override
    public List<String> getDescription(int level) {
        int bulletDmg = (int) (getBulletBaseDamage(level) * 100);
        return Arrays.asList(
                "§6§o\"Trời đất cuồng nộ, vạn pháp tuôn trào...\"",
                "",
                "§f§l[THÔNG SỐ TUYỆT KỸ]",
                "§7Tụ lực triệu hồi hàng loạt cổng không gian ma thuật xung quanh.",
                "§7Sau 1.5 giây chờ đợi, các cổng sẽ đồng loạt nã pháo bão hòa.",
                "",
                "§f▪ Thời gian xuất cổng: §a5 giây",
                "§f▪ Tốc độ xả đạn: §e0.3 giây / Viên",
                "§f▪ Sát thương đạn: §c" + bulletDmg + "% §fMỗi viên",
                "",
                "§e§l❂ ĐẠN ĐẠO GIA TỐC ❂",
                "§7Đạn ma pháp ban đầu bay chậm, uốn lượn biến ảo",
                "§7sau đó đột ngột gia tốc găm thẳng vào kẻ địch.",
                "",
                "§8§o*Vẻ đẹp chí mạng của nghệ thuật pháp sư."
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
            try { unlockedElements.add(MageElement.valueOf(elName.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }

        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;
        applyGlobalCooldown(player, rpgPlayer, level);

        player.sendMessage("§6§l[!] VẠN PHÁP QUY TÔNG: KHỞI ĐỘNG!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        double baseBulletDmg = getBulletBaseDamage(level);
        List<ItemDisplay> activeGates = new ArrayList<>();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // Mở cổng liên tục trong 5 giây

            // [ĐÃ SỬA]: Chuyển trí nhớ vào trong Runnable để mỗi người chơi có trí nhớ riêng
            final LinkedList<MageElement> recentElements = new LinkedList<>();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    activeGates.forEach(ItemDisplay::remove);
                    activeGates.clear();
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                if (ticks % 6 == 0) {
                    // [ĐÃ SỬA]: Gọi hàm với 2 biến mới: Trí nhớ của Runnable này và Danh sách hệ
                    MageElement element = getSmartComboElement(recentElements, unlockedElements);

                    Location eyeLoc = player.getEyeLocation();
                    Vector lookDir = eyeLoc.getDirection().normalize();
                    Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
                    Vector up = right.clone().crossProduct(lookDir).normalize();

                    double offsetX = (random.nextDouble() - 0.5) * 7.0;
                    double offsetY = (random.nextDouble() - 0.5) * 5.0 + 1.0;
                    double offsetZ = -0.5 - random.nextDouble() * 3.5;

                    Location gateLoc = eyeLoc.clone()
                            .add(right.clone().multiply(offsetX))
                            .add(up.clone().multiply(offsetY))
                            .add(lookDir.clone().multiply(offsetZ));

                    gateLoc.setDirection(lookDir);

                    ItemDisplay gate = player.getWorld().spawn(gateLoc, ItemDisplay.class);
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    meta.setCustomModelData(elementModelIds.get(element));
                    item.setItemMeta(meta);

                    gate.setItemStack(item);
                    gate.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    activeGates.add(gate);

                    float gateScale = 1.4f + random.nextFloat() * 0.8f;

                    org.bukkit.util.Transformation trans = gate.getTransformation();
                    trans.getScale().set(0f, 0f, 0f);
                    gate.setTransformation(trans);

                    gate.setInterpolationDelay(0);
                    gate.setInterpolationDuration(20);

                    trans.getScale().set(gateScale, gateScale, gateScale);
                    gate.setTransformation(trans);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!gate.isValid()) return;

                            Location target = findTarget(player);
                            fireAcceleratingCurvedBullet(player, rpgPlayer, gate.getLocation(), target, element, baseBulletDmg);

                            gate.setInterpolationDelay(0);
                            gate.setInterpolationDuration(10);
                            org.bukkit.util.Transformation shrinkTrans = gate.getTransformation();
                            shrinkTrans.getScale().set(0f, 0f, 0f);
                            gate.setTransformation(shrinkTrans);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    activeGates.remove(gate);
                                    gate.remove();
                                }
                            }.runTaskLater(plugin, 10L);
                        }
                    }.runTaskLater(plugin, 20L);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==========================================
    // THUẬT TOÁN TÌM NGUYÊN TỐ COMBO (ĐÃ SỬA LỖI ĐẦU VÀO)
    // ==========================================
    private MageElement getSmartComboElement(LinkedList<MageElement> recentElements, List<MageElement> available) {
        if (available.size() <= 1) return available.get(0);

        List<MageElement> validChoices = new ArrayList<>(available);
        if (recentElements.size() >= 1 && validChoices.size() > 1) {
            validChoices.remove(recentElements.getLast());
        }
        if (recentElements.size() >= 2 && validChoices.size() > 2) {
            validChoices.remove(recentElements.get(recentElements.size() - 2));
        }

        MageElement last = recentElements.isEmpty() ? null : recentElements.getLast();
        List<MageElement> comboMatches = new ArrayList<>();

        if (last != null) {
            for (MageElement e : validChoices) {
                if (isReactionPair(last, e)) comboMatches.add(e);
            }
        }

        MageElement chosen;
        if (!comboMatches.isEmpty() && random.nextInt(100) < 80) {
            chosen = comboMatches.get(random.nextInt(comboMatches.size()));
        } else {
            chosen = validChoices.get(random.nextInt(validChoices.size()));
        }

        recentElements.add(chosen);
        if (recentElements.size() > 3) recentElements.removeFirst();

        return chosen;
    }

    private boolean isReactionPair(MageElement e1, MageElement e2) {
        if (e1 == e2) return true;
        if ((e1 == MageElement.FIRE && e2 == MageElement.WATER) || (e2 == MageElement.FIRE && e1 == MageElement.WATER)) return true;
        if ((e1 == MageElement.LIGHT && e2 == MageElement.DARK) || (e2 == MageElement.LIGHT && e1 == MageElement.DARK)) return true;
        if ((e1 == MageElement.FIRE && e2 == MageElement.LIGHT) || (e2 == MageElement.FIRE && e1 == MageElement.LIGHT)) return true;
        if ((e1 == MageElement.FIRE && e2 == MageElement.NATURE) || (e2 == MageElement.FIRE && e1 == MageElement.NATURE)) return true;
        if ((e1 == MageElement.FIRE && e2 == MageElement.DARK) || (e2 == MageElement.FIRE && e1 == MageElement.DARK)) return true;
        if ((e1 == MageElement.WATER && e2 == MageElement.NATURE) || (e2 == MageElement.WATER && e1 == MageElement.NATURE)) return true;
        return false;
    }

    private Location findTarget(Player p) {
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 60);
        return (ray != null && ray.getHitPosition() != null) ? ray.getHitPosition().toLocation(p.getWorld()) : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(60));
    }

    // ==========================================
    // ĐẠN BÁN KÍNH CONG BIẾN ẢO + GIA TỐC (CHẬM -> NHANH)
    // ==========================================
    private void fireAcceleratingCurvedBullet(Player shooter, RPGPlayer rpgPlayer, Location start, Location end, MageElement element, double dmg) {
        shooter.getWorld().playSound(start, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 2.0f);

        Vector p0 = start.toVector();
        Vector p2 = end.toVector();

        // Điểm kiểm soát uốn cong: Bị đẩy xa ra 5 block ngẫu nhiên về các phía
        Vector dir = p2.clone().subtract(p0).normalize();
        Vector randomOffset = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(5.0);
        Vector p1 = p0.clone().add(dir.clone().multiply(p0.distance(p2) * 0.3)).add(randomOffset);

        new BukkitRunnable() {
            double t = 0.0;
            Vector lastPos = p0.clone();

            @Override
            public void run() {
                if (t > 1.0) {
                    impact(end, shooter, rpgPlayer, element, dmg);
                    this.cancel();
                    return;
                }

                // Bezier Bậc 2
                double invT = 1.0 - t;
                Vector current = p0.clone().multiply(invT * invT)
                        .add(p1.clone().multiply(2 * invT * t))
                        .add(p2.clone().multiply(t * t));
                Location loc = current.toLocation(start.getWorld());

                // ==========================================
                // GIẢI QUYẾT LỖI HẠT BỊ THƯA: NỘI SUY TUYẾN TÍNH
                // ==========================================
                double dist = current.distance(lastPos);
                int steps = Math.max(1, (int) (dist * 4)); // Tối thiểu 4 hạt mỗi block
                Vector stepVec = current.clone().subtract(lastPos).multiply(1.0 / steps);
                Vector tempPos = lastPos.clone();

                for (int i = 0; i < steps; i++) {
                    tempPos.add(stepVec);
                    Location trailLoc = tempPos.toLocation(start.getWorld());
                    spawnBulletTrail(element, trailLoc);
                }

                // Lõi đạn di chuyển
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                Particle.DustOptions coreColor = getCoreColor(element);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0, coreColor);

                for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                        impact(loc, shooter, rpgPlayer, element, dmg);
                        this.cancel(); return;
                    }
                }
                if (loc.getBlock().getType().isSolid()) {
                    impact(loc, shooter, rpgPlayer, element, dmg);
                    this.cancel(); return;
                }

                lastPos = current.clone();

                // ==========================================
                // TOÁN HỌC GIA TỐC [SIÊU TỐC ĐỘ]:
                // Vận tốc gốc x2 (0.08) và Gia tốc x2 (+0.50)
                // ==========================================
                double currentSpeed = 0.08 + (0.50 * t * t);
                t += currentSpeed;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void impact(Location loc, Player shooter, RPGPlayer rpgPlayer, MageElement element, double dmg) {
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
        spawnExplosionParticle(element, loc);

        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                LivingEntity target = (LivingEntity) e;

                ReactionEngine.ReactionResult result = ReactionEngine.processHit(shooter, target, element);
                double finalDmgMulti = dmg * result.damageMultiplier;

                shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                double finalDmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, finalDmgMulti, 0, 100.0, 0, true, false);
                CombatManager.applySkillDamage(shooter, target, finalDmg);
                shooter.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }

    private Particle.DustOptions getCoreColor(MageElement e) {
        switch (e) {
            case FIRE: return new Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
            case WATER: return new Particle.DustOptions(org.bukkit.Color.AQUA, 1.5f);
            case WIND: return new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
            case NATURE: return new Particle.DustOptions(org.bukkit.Color.LIME, 1.5f);
            case LIGHT: return new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
            case DARK: return new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f);
            default: return new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
        }
    }

    private void spawnBulletTrail(MageElement e, Location l) {
        Particle p = Particle.END_ROD;
        switch (e) {
            case FIRE: p = Particle.FLAME; break;
            case WATER: p = Particle.SPLASH; break;
            case WIND: p = Particle.SNOWFLAKE; break;
            case NATURE: p = Particle.CHERRY_LEAVES; break;
            case LIGHT: p = Particle.GLOW; break;
            case DARK: p = Particle.SQUID_INK; break;
        }
        l.getWorld().spawnParticle(p, l, 2, 0.1, 0.1, 0.1, 0.01);
    }

    private void spawnExplosionParticle(MageElement e, Location l) {
        Particle p = Particle.LARGE_SMOKE;
        switch (e) {
            case FIRE: p = Particle.LAVA; break;
            case WATER: p = Particle.SPLASH; break;
            case WIND: p = Particle.CLOUD; break;
            case NATURE: p = Particle.HAPPY_VILLAGER; break;
            case LIGHT: p = Particle.END_ROD; break;
            case DARK: p = Particle.SQUID_INK; break;
        }
        l.getWorld().spawnParticle(p, l, 30, 1.2, 1.2, 1.2, 0.1);
    }
}