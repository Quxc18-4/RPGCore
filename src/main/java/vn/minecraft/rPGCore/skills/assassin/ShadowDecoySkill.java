package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ShadowDecoySkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ShadowDecoySkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_shadow_decoy"; }
    @Override public String getName() { return "Ảo Ảnh Giao Tiễn"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.ARMOR_STAND; }

    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.emptyMap();
    }

    @Override
    public double getManaCost(int level) {
        return 50.0 + ((level - 1) * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 20.0 - (level - 1);
        return (long) (cd * 1000);
    }

    private double getDamageMultiplier(int level) {
        return 2.50 + ((level - 1) * 0.30); // 250% -> 370%
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        int dmg = (int) Math.round(getDamageMultiplier(displayLevel) * 100);

        lore.add("§7Búng người lùi về sau, bỏ lại một Tàng");
        lore.add("§7ảnh bóng tối tại vị trí cũ trong §e5 giây§7.");
        // [MỚI FIX]: Thêm mô tả Tàng hình
        lore.add("§7Đồng thời bản thân tiến vào trạng thái");
        lore.add("§fTàng Hình §7trong §a3 giây§7.");
        lore.add("");
        lore.add("§8[Nội tại - Mồi Nhử]:");
        lore.add("§7Tàng ảnh liên tục khiêu khích, thu hút sự");
        lore.add("§7chú ý của toàn bộ quái vật xung quanh.");
        lore.add("");
        lore.add("§8[Nội tại - Bạo Vũ]:");
        lore.add("§7Khi bị quái vật chạm vào (hoặc hết giờ),");
        lore.add("§7tàng ảnh vỡ nát, bắn §c12 Mũi Tên§7 tản ra");
        lore.add("§7khắp các hướng.");
        lore.add("§7Sát thương: §c" + dmg + "% §7(Mỗi mũi tên)");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (currentTime - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (currentTime - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        int fbLevel = 0;
        if (player.hasMetadata("Assassin_FatalBlow_Level")) {
            fbLevel = player.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
            player.removeMetadata("Assassin_FatalBlow_Level", plugin);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            player.sendMessage("§c§l[!] §fẢo Ảnh Giao Tiễn được Cường hóa!");
        }

        // 1. NHẢY THỤT LÙI VỀ SAU (Backstep)
        Vector backDir = player.getEyeLocation().getDirection().setY(0).normalize().multiply(-1.5);
        backDir.setY(0.4);
        player.setVelocity(backDir);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        // 2. TẠO TÀNG ẢNH VÀ TRUYỀN fbLevel VÀO TRONG ĐÓ
        Location decoyLoc = player.getLocation().clone();
        spawnAndHandleDecoy(player, rpgPlayer, level, decoyLoc, fbLevel);

        player.sendMessage("§8§l[!] ẢO ẢNH GIAO TIỄN!");

        // ==========================================
        // [MỚI FIX]: ÁP DỤNG TÀNG HÌNH 3 GIÂY
        // ==========================================
        applyStealth(player);

        return true;
    }

    // Logic Tàng hình 3 giây
    private void applyStealth(final Player player) {
        player.setMetadata("Assassin_Stealth", new FixedMetadataValue(plugin, true));
        player.sendMessage("§8§l[!] Bạn lẩn khuất vào bóng đêm... (3s)");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(plugin, player);
        }

        new BukkitRunnable() {
            int ticks = 0;
            Location lastLoc = player.getLocation();

            @Override
            public void run() {
                // Mất cờ Tàng hình (do xài chiêu khác) HOẶC hết 3s (60 ticks)
                if (!player.isOnline() || !player.hasMetadata("Assassin_Stealth") || ticks >= 60) {
                    if (player.hasMetadata("Assassin_Stealth")) {
                        player.removeMetadata("Assassin_Stealth", plugin);
                        player.sendMessage("§c§l[!] Bạn đã hiện hình!");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.showPlayer(plugin, player);
                        }
                    }
                    this.cancel();
                    return;
                }

                // Hiệu ứng vệt khói đen xì dưới chân khi di chuyển
                Location currentLoc = player.getLocation();
                if (currentLoc.distanceSquared(lastLoc) > 0.01) {
                    player.getWorld().spawnParticle(Particle.SMOKE, currentLoc.clone().add(0, 1, 0), 2, 0.1, 0, 0.1, 0);
                    lastLoc = currentLoc;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnAndHandleDecoy(final Player player, final RPGPlayer rpgPlayer, final int level, Location loc, final int fbLevel) {
        final ArmorStand decoy = loc.getWorld().spawn(loc, ArmorStand.class);
        decoy.setBasePlate(false);
        decoy.setArms(true);
        decoy.setGravity(false);
        decoy.setCustomName("§8§lTàng Ảnh");
        decoy.setCustomNameVisible(true);

        decoy.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        decoy.getEquipment().setChestplate(getColoredLeather(Material.LEATHER_CHESTPLATE, Color.BLACK));
        decoy.getEquipment().setLeggings(getColoredLeather(Material.LEATHER_LEGGINGS, Color.BLACK));
        decoy.getEquipment().setBoots(getColoredLeather(Material.LEATHER_BOOTS, Color.BLACK));

        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.05);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxLifespan = 100;

            @Override
            public void run() {
                if (!player.isOnline() || !decoy.isValid()) {
                    decoy.remove();
                    this.cancel();
                    return;
                }

                if (ticks % 10 == 0) {
                    for (Entity e : decoy.getNearbyEntities(10.0, 5.0, 10.0)) {
                        if (e instanceof Mob) {
                            ((Mob) e).setTarget(decoy);
                        }
                    }
                }

                decoy.getWorld().spawnParticle(Particle.SMOKE, decoy.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0);

                boolean shouldExplode = (ticks >= maxLifespan);
                if (!shouldExplode) {
                    for (Entity e : decoy.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (e instanceof Mob) {
                            shouldExplode = true;
                            break;
                        }
                    }
                }

                if (shouldExplode) {
                    explodeDecoy(player, rpgPlayer, level, decoy.getLocation(), fbLevel);
                    decoy.remove();
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ItemStack getColoredLeather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void explodeDecoy(Player player, RPGPlayer rpgPlayer, int level, Location loc, int fbLevel) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 1);
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.1);

        double baseMulti = getDamageMultiplier(level);

        if (fbLevel > 0) {
            baseMulti += 2.00 + ((fbLevel - 1) * 0.15);
        }

        for (int i = 0; i < 12; i++) {
            double angle = i * (Math.PI / 6);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            Vector arrowDir = new Vector(x, 0, z).normalize();

            launchArrow(player, rpgPlayer, baseMulti, loc.clone().add(0, 1.0, 0), arrowDir);
        }
    }

    private void launchArrow(final Player player, final RPGPlayer rpgPlayer, final double dmgMulti, Location origin, Vector direction) {
        new BukkitRunnable() {
            Location currentLoc = origin.clone();
            int distanceTravelled = 0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (distanceTravelled >= 20 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                currentLoc.add(direction.clone().multiply(0.5));
                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0.05);

                if (currentLoc.getBlock().getType().isSolid()) {
                    this.cancel();
                    return;
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (e instanceof LivingEntity && e != player && !hitTargets.contains(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;
                        hitTargets.add(target.getUniqueId());

                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 0.0, 0.0, 0.0, false, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);

                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
                    }
                }

                distanceTravelled++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}