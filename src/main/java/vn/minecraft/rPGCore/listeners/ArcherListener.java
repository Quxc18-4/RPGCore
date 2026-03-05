package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcherListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;

    // Lưu trữ Cooldown (Thời gian bắn lần cuối)
    // Biến này sẽ lưu "Thời điểm (millisecond) mà người chơi được phép bắn phát tiếp theo"
    private final Map<UUID, Long> nextShootTime = new HashMap<>();

    public ArcherListener(JavaPlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    // =========================================
    // 1. CƠ CHẾ CLICK TRÁI BẮN CUNG/NỎ
    // =========================================
    @EventHandler
    public void onLeftClickShoot(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Material type = item.getType();

        if (type != Material.BOW && type != Material.CROSSBOW) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // KIỂM TRA CLASS
        if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("archer")) {
            player.sendMessage("§c§l(!) §cChỉ có class Cung Thủ (Archer) mới có thể sử dụng vũ khí này!");
            return;
        }

        long currentTime = System.currentTimeMillis();

        // KIỂM TRA GLOBAL COOLDOWN
        // Lấy mốc thời gian được phép bắn tiếp theo (nếu chưa có thì mặc định là 0)
        long allowedTime = nextShootTime.getOrDefault(player.getUniqueId(), 0L);

        // Nếu thời gian hiện tại vẫn nhỏ hơn thời gian cho phép -> Đang bị Cooldown -> Chặn!
        if (currentTime < allowedTime) {
            // (Tùy chọn) Tính toán số giây còn lại để báo cho người chơi
            // double timeLeft = (allowedTime - currentTime) / 1000.0;
            // player.sendActionBar("§cĐang nạp tên... " + String.format("%.1f", timeLeft) + "s");
            return;
        }

        // VƯỢT QUA COOLDOWN -> XỬ LÝ BẮN
        if (type == Material.BOW) {
            // Bắn Cung: Khóa mọi đòn đánh tầm xa trong 1.2s (1200ms) tiếp theo
            nextShootTime.put(player.getUniqueId(), currentTime + 1200);
            fireArrow(player, "BOW");

        } else if (type == Material.CROSSBOW) {
            // Bắn Nỏ: Khóa mọi đòn đánh tầm xa trong 2.0s (2000ms) tiếp theo
            nextShootTime.put(player.getUniqueId(), currentTime + 2000);
            fireArrow(player, "CROSSBOW");
        }
    }

    // Hàm phụ trợ bắn mũi tên và dán nhãn (Metadata)
    private void fireArrow(Player player, String weaponType) {
        // Bắn ra 1 mũi tên với tốc độ nhân lên
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(arrow.getVelocity().multiply(1.5));

        // Dán nhãn
        arrow.setMetadata("WeaponType", new FixedMetadataValue(plugin, weaponType));

        // Hiệu ứng âm thanh
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        // HẸN GIỜ: Tự động xóa mũi tên sau 2 giây (40 ticks) bay trong không trung
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (arrow != null && arrow.isValid()) {
                    arrow.remove(); // Xóa entity mũi tên khỏi server
                }
            }
        }.runTaskLater(plugin, 40L); // 40L = 40 ticks
    }

    // =========================================
    // 2. CÔNG THỨC SÁT THƯƠNG (DAMAGE FORMULA)
    // =========================================
    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Arrow arrow = (Arrow) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        // Kiểm tra xem mũi tên này có phải do Player bắn từ hệ thống của ta không
        if (!(arrow.getShooter() instanceof Player)) return;
        if (!arrow.hasMetadata("WeaponType")) return;

        Player player = (Player) arrow.getShooter();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // Lấy thông số từ hệ thống của bạn
        String weaponType = arrow.getMetadata("WeaponType").get(0).asString();

        // Tính Player DMG: Lấy điểm STR nhân với hệ số (ví dụ: mỗi điểm STR = 2 Dmg)
        double playerDmg = rpgPlayer.getAttribute("STR") * 2.0;

        // TODO: Hàm lấy sát thương thực tế của vũ khí. Tạm thời set cứng để test.
        double weaponDmg = (weaponType.equals("BOW")) ? 15.0 : 25.0;

        // Lấy lượng giáp (Armor) của mục tiêu
        double targetArmor = 0;
        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) {
            targetArmor = armorAttr.getValue();
        }

        double finalDamage = 0;

        // ================= ÁP DỤNG CÔNG THỨC =================
        if (weaponType.equals("BOW")) {
            // (STR Dmg + Weapon Dmg - Target Armor) * 95%
            double rawDamage = playerDmg + weaponDmg - targetArmor;
            finalDamage = Math.max(1.0, rawDamage) * 0.95; // Đảm bảo st tối thiểu là 1
        }
        else if (weaponType.equals("CROSSBOW")) {
            // Có 33% tỉ lệ bỏ qua 100% giáp
            boolean ignoreArmor = Math.random() <= 0.33;

            if (ignoreArmor) {
                targetArmor = 0; // Xuyên giáp hoàn toàn
                player.sendMessage("§b§lXUYÊN GIÁP! §fMũi tên nỏ đã phớt lờ giáp của mục tiêu!");
            }

            // (STR Dmg + Weapon Dmg - Target Armor) * 155%
            // (Lưu ý: Bạn không đề cập trừ giáp trong công thức Nỏ, nhưng logic xuyên giáp ngầm hiểu là bình thường vẫn trừ giáp)
            double rawDamage = playerDmg + weaponDmg - targetArmor;
            finalDamage = Math.max(1.0, rawDamage) * 1.55;
        }

        // Đặt sát thương cuối cùng cho mũi tên
        event.setDamage(finalDamage);

        if (arrow.hasMetadata("Skill_SpreadArrow")) {
            int skillLevel = arrow.getMetadata("Skill_SpreadArrow").get(0).asInt();

            // Sát thương: 45% + 5% mỗi cấp (Tối đa 60%)
            double dmgMultiplier = Math.min(0.60, 0.45 + (skillLevel - 1) * 0.05);

            // Xuyên giáp: 20% + 1% mỗi cấp (Tối đa 25%)
            double ignoreChance = Math.min(0.25, 0.20 + (skillLevel - 1) * 0.01);

            if (Math.random() <= ignoreChance) {
                targetArmor = 0; // Kích hoạt xuyên giáp
                player.sendMessage("§b§lXUYÊN GIÁP! §fMũi tên xòe đã phớt lờ giáp của mục tiêu!");
            }

            // Tính sát thương kỹ năng
            double rawDamage = (playerDmg + weaponDmg - targetArmor) * dmgMultiplier;
            finalDamage = Math.max(1.0, rawDamage);

            event.setDamage(finalDamage);
            return; // DỪNG LẠI, không chạy tiếp xuống phần đánh thường bên dưới nữa
        }
    }

    // =========================================
    // 3. CHẶN CÁCH BẮN GỒNG MẶC ĐỊNH (VANILLA)
    // =========================================
    @EventHandler
    public void onVanillaShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            // Hủy sự kiện gồng chuột phải của Minecraft để ép người chơi phải click chuột trái
            event.setCancelled(true);
        }
    }

    // =========================================
    // 4. DỌN DẸP MŨI TÊN (CHỐNG LAG SERVER)
    // =========================================
    @EventHandler
    public void onArrowHitTarget(org.bukkit.event.entity.ProjectileHitEvent event) {
        // Chỉ xử lý nếu Projectile đó là Mũi tên
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();

            // Kiểm tra xem mũi tên này có phải từ kỹ năng Archer của chúng ta không
            if (arrow.hasMetadata("WeaponType")) {

                // HẸN GIỜ: Xóa mũi tên sau 1 giây (20 ticks) kể từ lúc chạm đích
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (arrow != null && arrow.isValid()) {
                            arrow.remove();
                        }
                    }
                }.runTaskLater(plugin, 20L); // 20L = 20 ticks
            }
        }
    }
}