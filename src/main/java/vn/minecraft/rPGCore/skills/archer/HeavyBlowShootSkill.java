package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class HeavyBlowShootSkill implements ISkill, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Lưu trữ những người chơi vừa bắn Trọng Tiễn để lấy thông tin hoàn Mana
    private final Map<UUID, RPGPlayer> activeShooters = new HashMap<>();

    public HeavyBlowShootSkill(JavaPlugin plugin) {
        this.plugin = plugin;
        // Tự đăng ký Listener cho chính nó để xử lý vụ nổ độc lập
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override public String getId() { return "archer_heavy_blow"; }
    @Override public String getName() { return "Trọng Tiễn"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.MACE; } // Icon chùy xịn xò
    @Override public int getMaxLevel() { return 3; } // Nâng cấp 2 lần = Max lv 3
    @Override public int getUpgradeCost(int currentLevel) { return 2; } // Tiêu tốn 2 SP mỗi lần

    @Override public int getRequiredPlayerLevel() { return 34; } // Yêu cầu người chơi lv 34

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("archer_triple_shot", 10); // Cần Tam Tiễn lv 10
    }

    @Override
    public double getManaCost(int level) {
        return 90.0 + (level - 1) * 15.0; // 90 / 105 / 120
    }

    @Override
    public long getCooldown(int level) {
        double cd = 20.0 - (level - 1) * 1.5; // 20 / 18.5 / 17
        return (long) (cd * 1000);
    }

    // --- CÁC THÔNG SỐ SÁT THƯƠNG ---
    private double getDirectDmg(int level) { return (level == 3) ? 22.0 : (level == 2) ? 18.0 : 15.0; }
    private double getHitAoEMulti(int level) { return (level == 3) ? 6.0 : (level == 2) ? 4.8 : 4.0; }
    private double getMissAoEMulti(int level) { return (level == 3) ? 6.5 : (level == 2) ? 5.0 : 4.0; }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Nảy bổng lên không trung và tụ lực bắn ra");
        lore.add("§7một mũi tên mang sức mạnh hủy diệt.");

        lore.add("§7Sát thương (Trúng đích): §c" + (int)(getDirectDmg(displayLevel)*100) + "%");
        lore.add("§7Sát thương Nổ (Xung quanh): §6" + (int)(getHitAoEMulti(displayLevel)*100) + "%");
        lore.add("");
        lore.add("§e§lNẾU BẮN TRƯỢT:");
        lore.add("§7Sát thương Nổ: §6" + (int)(getMissAoEMulti(displayLevel)*100) + "%");
        lore.add("§7Hoàn trả: §b40% Mana §7đã tiêu hao");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Mana!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastUsed < getCooldown(level)) {
            double timeLeft = (getCooldown(level) - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        player.sendMessage("§c§l[!] TRỌNG TIỄN!");

        // 1. HIỆU ỨNG NHẢY BỔNG VÀ BẬT LÙI (Evasion Style)
        Location currentEyeLoc = player.getEyeLocation();
        Vector lookDir = currentEyeLoc.getDirection();

        // Lấy vector chỉ hướng ngang và đảo ngược nó lại (đẩy lùi về sau)
        Vector backward = new Vector(-lookDir.getX(), 0, -lookDir.getZ());

        // Chống lỗi Toán học (NaN) nếu người chơi nhìn thẳng tắp lên trời/xuống đất
        if (backward.lengthSquared() > 0) {
            backward.normalize();
        } else {
            backward = new Vector(-1, 0, 0);
        }

        // Lực nảy lên (Y) = 1.25 (Cao khoảng 5 block, nảy cao hơn 2 block so với cũ)
        // Lực lùi (XZ) = 1.2 (Tạo góc bật lùi khoảng 40-45 độ về phía sau)
        Vector jumpVector = backward.multiply(1.2).setY(1.25);

        player.setVelocity(jumpVector);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.0f);

        // 2. DELAY 15 TICKS (0.75s) ĐỂ BAY ĐẾN ĐỈNH TRƯỚC KHI BẮN
        // Do nảy cao hơn, thời gian đạt đỉnh (Apex) sẽ lâu hơn, phải tăng Delay lên 15 ticks
        new BukkitRunnable() {
            @Override
            public void run() {
                // Lấy lại vị trí và hướng NHÌN MỚI sau khi đã bị đẩy lùi
                Location eyeLoc = player.getEyeLocation();
                Vector dir = eyeLoc.getDirection();

                // Bắn mũi tên với tốc độ cực nhanh (4.5f)
                Arrow arrow = player.getWorld().spawnArrow(eyeLoc, dir, 4.5f, 0f);
                arrow.setShooter(player);

                // Dán nhãn cho ArcherListener xử lý Dmg trực tiếp
                arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
                arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, getDirectDmg(level)));
                arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, 0.40));

                // Dán nhãn để xử lý Vụ nổ
                arrow.setMetadata("HeavyBlow_Level", new FixedMetadataValue(plugin, level));
                activeShooters.put(arrow.getUniqueId(), rpgPlayer);

                // Tạo vệt lửa đen cực ngầu
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isOnGround()) {
                            this.cancel();
                            return;
                        }
                        arrow.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, arrow.getLocation(), 2, 0, 0, 0, 0);
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                // Xóa rác Map dự phòng nếu mũi tên bay vào hư không
                new BukkitRunnable() {
                    @Override
                    public void run() { activeShooters.remove(arrow.getUniqueId()); }
                }.runTaskLater(plugin, 200L);
            }
        }.runTaskLater(plugin, 15L); // <--- Đã sửa Delay thành 15L để khớp với độ cao mới

        return true;
    }

    // ===============================================
    // LẮNG NGHE SỰ KIỆN CHẠM ĐÍCH (ĐỂ GÂY NỔ AOE)
    // ===============================================
    @EventHandler
    public void onHeavyArrowHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();

        // Kiểm tra xem mũi tên này có phải Trọng Tiễn không
        if (!activeShooters.containsKey(arrow.getUniqueId())) return;

        // Ngăn kích nổ 2 lần
        if (arrow.hasMetadata("HeavyBlow_Exploded")) return;
        arrow.setMetadata("HeavyBlow_Exploded", new FixedMetadataValue(plugin, true));

        RPGPlayer rpgPlayer = activeShooters.remove(arrow.getUniqueId());
        Player shooter = (Player) arrow.getShooter();
        int level = arrow.getMetadata("HeavyBlow_Level").get(0).asInt();

        // Tạo hiệu ứng nổ hoành tráng
        arrow.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, arrow.getLocation(), 1);
        arrow.getWorld().playSound(arrow.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        // Lấy Sức mạnh thô (Base Dmg + Weapon Dmg) để tính cho vụ nổ
        double weaponDmg = (shooter.getInventory().getItemInMainHand().getType() == Material.CROSSBOW) ? 25.0 : 15.0;
        double baseDmg = rpgPlayer.getAttributeData().getAttribute("STR") * 2.0;
        double rawDmg = baseDmg + weaponDmg;

        if (event.getHitEntity() != null) {
            // --- TRƯỜNG HỢP: TRÚNG QUÁI ---
            double aoeDmg = rawDmg * getHitAoEMulti(level);
            damageNearby(arrow.getLocation(), shooter, event.getHitEntity(), aoeDmg);

        } else if (event.getHitBlock() != null) {
            // --- TRƯỜNG HỢP: BẮN TRƯỢT XUỐNG ĐẤT ---
            double aoeDmg = rawDmg * getMissAoEMulti(level);
            damageNearby(arrow.getLocation(), shooter, null, aoeDmg);

            // HOÀN TRẢ MANA
            double refund = getManaCost(level) * 0.40;
            double newMana = Math.min(rpgPlayer.getTotalMaxResource(), rpgPlayer.getCurrentResource() + refund);
            rpgPlayer.setCurrentResource(newMana);
            shooter.sendMessage("§b⚡ Đã hoàn lại " + (int)refund + " Mana do bắn trượt!");
        }

        // Dọn dẹp mũi tên ngay lập tức
        arrow.remove();
    }

    // Hàm gây sát thương diện rộng
    private void damageNearby(Location loc, Player shooter, org.bukkit.entity.Entity ignoreEntity, double damage) {
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
            if (e instanceof org.bukkit.entity.LivingEntity && e != shooter && e != ignoreEntity) {
                // Sát thương này là sát thương vật lý thuần túy bỏ qua I-Frame nhưng vẫn bị cản bởi Giáp
                ((org.bukkit.entity.LivingEntity) e).setNoDamageTicks(0);
                ((org.bukkit.entity.LivingEntity) e).damage(damage, shooter);
            }
        }
    }
}