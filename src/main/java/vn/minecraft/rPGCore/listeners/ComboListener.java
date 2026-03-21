package vn.minecraft.rPGCore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
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

    // ==========================================
    // 1. LẮNG NGHE CLICK CHUỘT TRÁI/PHẢI VÀO KHÔNG KHÍ HOẶC BLOCK
    // ==========================================
    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;

        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;

        String click = (action.name().contains("LEFT")) ? "L" : "R";
        processInput(event.getPlayer(), click);
    }

    // ==========================================
    // 2. LẮNG NGHE CLICK CHUỘT TRÁI (L) VÀO QUÁI VẬT
    // Priority LOWEST: Chạy trước khi bị PlayerListener chặn sát thương!
    // ==========================================
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        // Nếu sát thương này do Kỹ năng gây ra thì không tính là bấm chuột
        if (player.hasMetadata("Skill_Damage_Lock")) return;

        // Đánh vào quái vật mặc định luôn luôn là Left Click (L)
        processInput(player, "L");
    }

    // ==========================================
    // 3. BỘ XỬ LÝ LÕI COMBO (DÙNG CHUNG CHO CẢ 2 EVENT)
    // ==========================================
    private void processInput(Player player, String click) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        ItemStack mainItem = player.getInventory().getItemInMainHand();
        String classId = rpgPlayer.getCurrentClass().getId();

        // Lọc rác: Cầm sai vũ khí
        if (!PlayerListener.isValidWeaponForClass(mainItem, classId)) return;

        EquipmentListener eqListener = new EquipmentListener(playerManager);
        if (!eqListener.canPlayerUse(player, mainItem)) return;
        if (player.hasMetadata("Is_Casting")) return;

        UUID uuid = player.getUniqueId();
        String current = currentCombos.getOrDefault(uuid, "");

        // Chữ cái đầu tiên luôn phải là R (Chuột phải)
        if (current.isEmpty() && click.equals("L")) return;

        String combo = current + click;
        if (combo.length() > 4) combo = click.equals("R") ? "R" : "";
        if (combo.isEmpty()) return;

        currentCombos.put(uuid, combo);
        player.sendActionBar("§e⚡ Đang niệm chú: §b§l" + combo + "...");

        // Khóa đánh thường ngay khi bắt đầu chuỗi
        player.setMetadata("Is_Combo_Casting", new FixedMetadataValue(plugin, true));

        if (resetTasks.containsKey(uuid)) resetTasks.get(uuid).cancel();

        BukkitRunnable resetTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentCombos.remove(uuid);
                resetTasks.remove(uuid);
                player.sendActionBar(" ");
                // Mở khóa đánh thường khi hết thời gian bấm combo
                if (player.hasMetadata("Is_Combo_Casting")) player.removeMetadata("Is_Combo_Casting", plugin);
            }
        };
        resetTask.runTaskLater(plugin, 20L); // Có 1 giây để bấm phím tiếp theo
        resetTasks.put(uuid, resetTask);

        // ==========================================
        // 4. KÍCH HOẠT KỸ NĂNG KHI ĐỦ 4 PHÍM
        // ==========================================
        if (combo.length() == 4) {
            String mappedSkillId = rpgPlayer.getSkillData().getSkillByCombo(combo);
            if (mappedSkillId != null) {
                ISkill skill = skillManager.getSkill(mappedSkillId);
                if (skill != null) {
                    int level = rpgPlayer.getSkillData().getSkillLevel(mappedSkillId);

                    if (skill.execute(player, rpgPlayer, level)) {
                        // Phá tàng hình khi xài chiêu
                        if (!mappedSkillId.equals("assassin_smoke_bomb") &&
                                !mappedSkillId.equals("assassin_shadow_decoy") &&
                                !mappedSkillId.equals("assassin_shadow_step")) {

                            if (player.hasMetadata("Assassin_Stealth") || player.hasMetadata("Assassin_Smoke_Bomb_Active")) {
                                player.removeMetadata("Assassin_Stealth", plugin);
                                player.removeMetadata("Assassin_Smoke_Bomb_Active", plugin);
                                player.sendMessage("§c§l[!] Bạn đã lộ diện do sử dụng kỹ năng!");
                                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) p.showPlayer(plugin, player);
                            }
                        }

                        player.sendActionBar("§a§l✔ Kích hoạt thành công!");

                        // Cắm cờ chống kẹt sát thương tay khi vừa ra chiêu
                        player.setMetadata("Skill_Just_Cast", new FixedMetadataValue(plugin, true));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.hasMetadata("Skill_Just_Cast")) player.removeMetadata("Skill_Just_Cast", plugin);
                            }
                        }.runTaskLater(plugin, 10L);
                    }
                }
            } else {
                player.sendActionBar("§c§l✖ Sai tổ hợp phím!");
            }

            currentCombos.remove(uuid);
            resetTask.cancel();
            // Mở khóa đánh thường khi hoàn thành chuỗi (dù đúng hay sai)
            if (player.hasMetadata("Is_Combo_Casting")) player.removeMetadata("Is_Combo_Casting", plugin);
        }
    }
}