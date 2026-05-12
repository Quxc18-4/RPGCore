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

    // SÁT THƯƠNG GỐC: 250% - 280% - 310% cho mỗi viên đạn
    private double getBulletBaseDamage(int level) { return 2.5 + (level * 0.3); }

    @Override
    public List<String> getDescription(int level) {
        int bulletDmg = (int) (getBulletBaseDamage(level) * 100);
        return Arrays.asList(
                "§6§o\"Trời đất cuồng nộ, vạn pháp tuôn trào...\"",
                "",
                "§f§l[THÔNG SỐ TUYỆT KỸ]",
                "§7Khởi động trận pháp, liên tục triệu hồi các cổng ma thuật",
                "§7chớp nhoáng xung quanh để nã pháo bão hòa về phía kẻ địch.",
                "",
                "§f▪ Thời gian bùng nổ: §a5 giây",
                "§f▪ Tốc độ xả đạn: §e0.15 giây / Viên",
                "§f▪ Sát thương đạn: §c" + bulletDmg + "% §fMỗi viên",
                "",
                "§e§l❂ THUẬT TOÁN CỘNG HƯỞNG ❂",
                "§7Pháp trận tự động lựa chọn nguyên tố tiếp theo sao cho",
                "§7tối đa hóa khả năng kích nổ §dReaction Engine§7.",
                "",
                "§8§o*Một cơn bão sát thương không thể cản phá."
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

        player.sendMessage("§6§l[!] VẠN PHÁP QUY TÔNG: BÙNG NỔ!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);

        double baseBulletDmg = getBulletBaseDamage(level);
        List<ItemDisplay> activeGates = new ArrayList<>();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 5 giây
            MageElement lastFiredElement = null;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= maxTicks) {
                    activeGates.forEach(ItemDisplay::remove);
                    activeGates.clear();
                    this.cancel();
                    return;
                }

                // ==========================================
                // BẮN MỖI 3 TICKS (0.15s)
                // ==========================================
                if (ticks % 3 == 0) {
                    MageElement element = getSmartComboElement(lastFiredElement, unlockedElements);
                    lastFiredElement = element;

                    // 1. TÍNH VỊ TRÍ XUẤT HIỆN TRONG KHÔNG GIAN 3D RỘNG LỚN
                    Location eyeLoc = player.getEyeLocation();
                    Vector lookDir = eyeLoc.getDirection().normalize();
                    Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
                    Vector up = right.clone().crossProduct(lookDir).normalize();

                    // Rải rác theo không gian 3D để không đè nhau
                    double offsetX = (random.nextDouble() - 0.5) * 6.0; // Rộng 6 block
                    double offsetY = (random.nextDouble() - 0.5) * 4.0 + 1.0; // Cao 4 block
                    double offsetZ = -0.5 - random.nextDouble() * 3.0; // Sâu 3.5 block sau lưng

                    Location gateLoc = eyeLoc.clone()
                            .add(right.clone().multiply(offsetX))
                            .add(up.clone().multiply(offsetY))
                            .add(lookDir.clone().multiply(offsetZ));

                    gateLoc.setDirection(lookDir);

                    // 2. TRIỆU HỒI GATE
                    ItemDisplay gate = player.getWorld().spawn(gateLoc, ItemDisplay.class);
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    meta.setCustomModelData(elementModelIds.get(element));
                    item.setItemMeta(meta);

                    gate.setItemStack(item);
                    gate.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    activeGates.add(gate);

                    // Random kích thước vòng (Từ 1.2 đến 1.8) để nhìn chân thực hơn
                    float gateScale = 1.2f + random.nextFloat() * 0.6f;

                    org.bukkit.util.Transformation trans = gate.getTransformation();
                    trans.getScale().set(0f, 0f, 0f);
                    gate.setTransformation(trans);

                    gate.setInterpolationDelay(0);
                    gate.setInterpolationDuration(4);

                    trans.getScale().set(gateScale, gateScale, gateScale);
                    gate.setTransformation(trans);

                    // 3. BẮN ĐẠN & THU GỌN
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!gate.isValid()) return;

                            Location target = findTarget(player);
                            fireHighSpeedBullet(player, rpgPlayer, gate.getLocation(), target, element, baseBulletDmg);

                            gate.setInterpolationDelay(0);
                            gate.setInterpolationDuration(3);
                            org.bukkit.util.Transformation shrinkTrans = gate.getTransformation();
                            shrinkTrans.getScale().set(0f, 0f, 0f);
                            gate.setTransformation(shrinkTrans);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    activeGates.remove(gate);
                                    gate.remove();
                                }
                            }.runTaskLater(plugin, 3L);
                        }
                    }.runTaskLater(plugin, 4L);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private MageElement getSmartComboElement(MageElement last, List<MageElement> available) {
        if (last == null || available.size() <= 1) return available.get(random.nextInt(available.size()));

        List<MageElement> comboMatches = new ArrayList<>();
        for (MageElement e : available) {
            if (isReactionPair(last, e)) comboMatches.add(e);
        }

        if (!comboMatches.isEmpty() && random.nextInt(100) < 80) {
            return comboMatches.get(random.nextInt(comboMatches.size()));
        }

        return available.get(random.nextInt(available.size()));
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
        RayTraceResult ray = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 50);
        return (ray != null && ray.getHitPosition() != null) ? ray.getHitPosition().toLocation(p.getWorld()) : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(50));
    }

    // ==========================================
    // ĐẠN TỐC ĐỘ CAO VỚI HẠT NHÂN (CORE)
    // ==========================================
    private void fireHighSpeedBullet(Player shooter, RPGPlayer rpgPlayer, Location start, Location end, MageElement element, double dmg) {
        shooter.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 2.0f);

        Vector p0 = start.toVector();
        Vector p2 = end.toVector();

        Vector dir = p2.clone().subtract(p0).normalize();
        Vector randomVec = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(8.0);
        Vector p1 = p0.clone().add(p2).multiply(0.5).add(randomVec);

        new BukkitRunnable() {
            double t = 0.0;
            final double speed = 0.15;

            @Override
            public void run() {
                if (t > 1.0) {
                    impact(end, shooter, rpgPlayer, element, dmg);
                    this.cancel();
                    return;
                }

                double invT = 1.0 - t;
                Vector current = p0.clone().multiply(invT * invT)
                        .add(p1.clone().multiply(2 * invT * t))
                        .add(p2.clone().multiply(t * t));
                Location loc = current.toLocation(start.getWorld());

                // [ĐÃ SỬA LỖI CRASH]: Thay Particle.FLASH thành Particle.END_ROD để an toàn trên 1.21+
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);

                Particle.DustOptions coreColor = getCoreColor(element);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0, coreColor);
                spawnBulletTrail(element, loc);

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

                t += speed;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    // ==========================================
    // VỤ NỔ ĐẠN VÀ TÍCH HỢP REACTION ENGINE
    // ==========================================
    private void impact(Location loc, Player shooter, RPGPlayer rpgPlayer, MageElement element, double dmg) {
        // [ĐÃ SỬA]: Thay Particle.EXPLOSION thành LARGE_SMOKE để chống crash
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

    // ==========================================
    // HIỆU ỨNG THỊ GIÁC (ĐÃ FIX LỖI DATA CLASS)
    // ==========================================
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
        Particle p = (e == MageElement.FIRE) ? Particle.SMALL_FLAME : (e == MageElement.WATER) ? Particle.SPLASH : (e == MageElement.WIND) ? Particle.SNOWFLAKE : (e == MageElement.NATURE) ? Particle.CHERRY_LEAVES : (e == MageElement.LIGHT) ? Particle.GLOW : Particle.SQUID_INK;
        l.getWorld().spawnParticle(p, l, 5, 0.2, 0.2, 0.2, 0.01);
    }

    private void spawnExplosionParticle(MageElement e, Location l) {
        // [ĐÃ SỬA]: Thay Particle.EXPLOSION của hệ WIND thành Particle.CLOUD
        Particle p = (e == MageElement.FIRE) ? Particle.LAVA : (e == MageElement.WATER) ? Particle.NAUTILUS : (e == MageElement.WIND) ? Particle.CLOUD : (e == MageElement.NATURE) ? Particle.COMPOSTER : (e == MageElement.LIGHT) ? Particle.FIREWORK : Particle.DRAGON_BREATH;
        l.getWorld().spawnParticle(p, l, 30, 1.2, 1.2, 1.2, 0.1);
    }
}