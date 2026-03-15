package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.SkillManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;
    private final SkillManager skillManager;

    private final Map<UUID, String> currentCombos = new HashMap<>();
    private final Map<UUID, BukkitRunnable> resetTasks = new HashMap<>();

    public ComboListener(JavaPlugin plugin, PlayerManager playerManager, SkillManager skillManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        org.bukkit.inventory.ItemStack mainItem = player.getInventory().getItemInMainHand();
        Material mainType = mainItem.getType();
        String classId = rpgPlayer.getCurrentClass().getId().toLowerCase();

        // KIỂM TRA VŨ KHÍ HỢP LỆ THEO CLASS
        boolean isValidWeapon = false;
        switch (classId) {
            case "archer":
                isValidWeapon = (mainType == Material.BOW || mainType == Material.CROSSBOW);
                break;
            case "assassin":
                isValidWeapon = (mainType == Material.SHEARS); // Dao găm
                break;
            case "mage":
                isValidWeapon = (mainType == Material.STICK || mainType == Material.BLAZE_ROD); // Trượng
                break;
            case "summoner":
                isValidWeapon = (mainType == Material.LEAD); // Roi da
                break;
            case "reaper":
                isValidWeapon = mainType.name().endsWith("_HOE"); // Lưỡi hái
                break;
            case "warrior":
            case "guardian":
            case "commoner":
            default:
                isValidWeapon = mainType.name().endsWith("_SWORD") || mainType.name().endsWith("_AXE"); // Kiếm, Rìu
                break;
        }

        if (!isValidWeapon) return; // Nếu không cầm đúng vũ khí thì không ghi nhận Combo

        // KHÓA COMBO NẾU ĐANG NIỆM CHÚ (Liên Hoàn Tên / Khống chế)
        if (player.hasMetadata("Is_Casting")) {
            return;
        }

        String click = (action.name().contains("LEFT")) ? "L" : "R";
        UUID uuid = player.getUniqueId();
        String current = currentCombos.getOrDefault(uuid, "");

        if (current.isEmpty() && click.equals("L")) return;

        String combo = current + click;
        if (combo.length() > 4) combo = click.equals("R") ? "R" : "";
        if (combo.isEmpty()) return;

        currentCombos.put(uuid, combo);
        player.sendActionBar("§e⚡ Đang niệm chú: §b§l" + combo + "...");

        if (resetTasks.containsKey(uuid)) resetTasks.get(uuid).cancel();

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

        // KIỂM TRA SKILL TỪ INTERFACE
        if (combo.length() == 4) {
            String mappedSkillId = rpgPlayer.getSkillData().getSkillByCombo(combo);
            if (mappedSkillId != null) {
                ISkill skill = skillManager.getSkill(mappedSkillId);
                if (skill != null) {
                    int level = rpgPlayer.getSkillData().getSkillLevel(mappedSkillId);

                    // Gọi hàm execute() của Interface
                    if (skill.execute(player, rpgPlayer, level)) {
                        player.sendActionBar("§a§l✔ Kích hoạt thành công!");
                    }
                }
            } else {
                player.sendActionBar("§c§l✖ Sai tổ hợp phím!");
            }

            currentCombos.remove(uuid);
            resetTask.cancel();
        }
    }
}