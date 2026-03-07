package vn.minecraft.rPGCore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.classes.Skill.Archer.ArcherSkillManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;
    private final ArcherSkillManager archerSkillManager; // Class thực thi kỹ năng

    // Lưu trữ chuỗi Combo hiện tại của người chơi
    private final Map<UUID, String> currentCombos = new HashMap<>();
    // Lưu trữ Task để tự động reset Combo nếu ngừng bấm
    private final Map<UUID, BukkitRunnable> resetTasks = new HashMap<>();

    public ComboListener(JavaPlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.archerSkillManager = new ArcherSkillManager(plugin, playerManager);
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        // 1. SỬA LỖI "DOUBLE R": Bỏ qua sự kiện nếu nó phát ra từ tay trái (Off-hand)
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        // Bỏ qua nếu đạp trúng Physical block (Pressure plate...)
        if (action == Action.PHYSICAL) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // 2. KIỂM TRA VŨ KHÍ CỦA CLASS (Vũ khí chính chủ mới được dùng Combo)
        org.bukkit.inventory.ItemStack mainItem = player.getInventory().getItemInMainHand();
        org.bukkit.Material mainType = mainItem.getType();
        String classId = rpgPlayer.getCurrentClass().getId().toLowerCase();

        boolean isValidWeapon = false;

        // Kiểm tra cho class Archer
        if (classId.equals("archer")) {
            if (mainType == org.bukkit.Material.BOW || mainType == org.bukkit.Material.CROSSBOW) {
                isValidWeapon = true;
            }
        }
        // TODO: Sau này bạn thêm các Class khác vào đây.
        // Ví dụ: else if (classId.equals("warrior")) { if (mainType.name().endsWith("_SWORD")) isValidWeapon = true; }

        // Nếu đang không cầm vũ khí hợp lệ của Class, không cho phép niệm chú
        if (!isValidWeapon) {
            return;
        }

        // 3. XỬ LÝ LOGIC NỐI CHUỖI COMBO (Đã áp dụng quy tắc R mở đầu)
        String click = (action.name().contains("LEFT")) ? "L" : "R";
        UUID uuid = player.getUniqueId();

        String current = currentCombos.getOrDefault(uuid, "");

        // Chỉ cho phép bắt đầu chuỗi bằng R. Nếu đang đánh thường (L), bỏ qua luôn.
        if (current.isEmpty() && click.equals("L")) {
            return;
        }

        // Nối thêm nút mới vào chuỗi
        String combo = current + click;

        // Giới hạn tối đa 4 nút
        if (combo.length() > 4) {
            combo = click.equals("R") ? "R" : "";
        }

        if (combo.isEmpty()) return;

        currentCombos.put(uuid, combo);

        // Hiển thị Combo đang bấm lên Action Bar
        player.sendActionBar("§e⚡ Đang niệm chú: §b§l" + combo + "...");

        // Hủy Task reset cũ (nếu có)
        if (resetTasks.containsKey(uuid)) {
            resetTasks.get(uuid).cancel();
        }

        // Đặt Task reset mới sau 1 giây (20 ticks)
        BukkitRunnable resetTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentCombos.remove(uuid);
                resetTasks.remove(uuid);
                player.sendActionBar(" ");
            }
        };
        resetTask.runTaskLater(plugin, 20L);
        resetTasks.put(uuid, resetTask);

        // KIỂM TRA SKILL KHI ĐỦ 4 NÚT
        if (combo.length() == 4) {
            String mappedSkill = rpgPlayer.getSkillByCombo(combo);
            if (mappedSkill != null) {
                boolean success = archerSkillManager.executeSkill(player, rpgPlayer, mappedSkill);
                if (success) {
                    player.sendActionBar("§a§l✔ Kích hoạt thành công!");
                }
            } else {
                player.sendActionBar("§c§l✖ Sai tổ hợp phím!");
            }

            // Xóa chuỗi bất kể thành công hay thất bại
            currentCombos.remove(uuid);
            resetTask.cancel();
        }
    }
}