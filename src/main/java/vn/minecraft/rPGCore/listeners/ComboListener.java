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
        Player player = event.getPlayer();
        Action action = event.getAction();

        // Bỏ qua nếu đạp trúng Physical block (Pressure plate...)
        if (action == Action.PHYSICAL) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // Xác định là L (Trái) hay R (Phải)
        String click = (action.name().contains("LEFT")) ? "L" : "R";
        UUID uuid = player.getUniqueId();

        // Nối thêm vào chuỗi Combo hiện tại
        String combo = currentCombos.getOrDefault(uuid, " ") + click;

        // Giới hạn tối đa 4 nút
        if (combo.length() > 4) {
            combo = click; // Reset lại từ đầu nếu lỡ bấm quá 4 nút
        }

        currentCombos.put(uuid, combo);

        // Hiển thị Combo đang bấm lên Action Bar để người chơi dễ nhìn
        player.sendActionBar("§eCombo: §b§l" + combo);

        // Hủy Task reset cũ (nếu có)
        if (resetTasks.containsKey(uuid)) {
            resetTasks.get(uuid).cancel();
        }

        // Đặt Task reset mới: Nếu 1 giây (20 ticks) không bấm thêm, sẽ xóa Combo
        BukkitRunnable resetTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentCombos.remove(uuid);
                resetTasks.remove(uuid);
                // Xóa chữ trên ActionBar
                player.sendActionBar(" ");
            }
        };
        resetTask.runTaskLater(plugin, 20L);
        resetTasks.put(uuid, resetTask);

        if (combo.length() == 4) {
            String mappedSkill = rpgPlayer.getSkillByCombo(combo);
            if (mappedSkill != null) {
                boolean success = archerSkillManager.executeSkill(player, rpgPlayer, mappedSkill);
                if (success) {
                    currentCombos.remove(uuid);
                    resetTask.cancel();
                }
            }
        }
    }
}