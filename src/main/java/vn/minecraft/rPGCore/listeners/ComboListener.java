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
    private final Map<UUID, Long> lastClickTime = new HashMap<>();

    public ComboListener(JavaPlugin plugin, PlayerManager playerManager, SkillManager skillManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.skillManager = skillManager;
    }

    // ==========================================
    // 1. BẮT SỰ KIỆN CLICK KHÔNG KHÍ / BLOCK
    // ==========================================
    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;

        String click = (action.name().contains("LEFT")) ? "L" : "R";
        processComboInput(player, click);
    }

    // ==========================================
    // 2. BẮT SỰ KIỆN CLICK TRÚNG QUÁI VẬT (MỚI)
    // ==========================================
    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            // Khóa mõm L ma do CombatManager gây ra
            if (player.hasMetadata("Ghost_L_Blocker")) {
                return;
            }

            // [ĐÃ SỬA LỖI TYPO]: Kiểm tra metadata trên 'player' thay vì 'event.getEntity()'
            if (player.hasMetadata("Skill_Damage_Lock")) {
                return;
            }

            // Nếu đang múa Combo mà lỡ tay chém trúng quái
            if (player.hasMetadata("Is_Combo_Casting")) {
                processComboInput(player, "L");
            }
        }
    }

    // ==========================================
    // BẮT SỰ KIỆN CLICK PHẢI TRÚNG QUÁI VẬT / ENTITY
    // ==========================================
    @EventHandler
    public void onEntityRightClick(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();

        // Bất kể là click vào ai, cứ click phải trúng Entity là ném chữ "R" vào bộ lọc
        processComboInput(player, "R");
    }

    // ==========================================
    // 3. LÕI XỬ LÝ CHUỖI COMBO (DÙNG CHUNG)
    // ==========================================
    private void processComboInput(Player player, String click) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // ==========================================
        // [BỘ LỌC CHỐNG NHIỄU]: Bỏ qua nếu 2 tín hiệu cách nhau < 50ms
        // ==========================================
        if (now - lastClickTime.getOrDefault(uuid, 0L) < 50) {
            return;
        }
        lastClickTime.put(uuid, now);
        // ==========================================

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
        if (rpgPlayer == null) return;

        ItemStack mainItem = player.getInventory().getItemInMainHand();
        String classId = rpgPlayer.getCurrentClass().getId();

        if (!PlayerListener.isValidWeaponForClass(mainItem, classId)) return;

        EquipmentListener eqListener = new EquipmentListener(playerManager);
        if (!eqListener.canPlayerUse(player, mainItem)) {
            return;
        }

        if (player.hasMetadata("Is_Casting")) return;

        String current = currentCombos.getOrDefault(uuid, "");

        // Chữ cái đầu tiên luôn phải là R
        if (current.isEmpty() && click.equals("L")) return;

        String combo = current + click;
        if (combo.length() > 4) combo = click.equals("R") ? "R" : "";
        if (combo.isEmpty()) return;

        currentCombos.put(uuid, combo);
        player.sendActionBar("§e⚡ Đang niệm chú: §b§l" + combo + "...");

        // [KHÓA ĐÁNH THƯỜNG]: Gắn cờ ngay khi bắt đầu bấm
        player.setMetadata("Is_Combo_Casting", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        if (resetTasks.containsKey(uuid)) resetTasks.get(uuid).cancel();

        BukkitRunnable resetTask = new BukkitRunnable() {
            @Override
            public void run() {
                currentCombos.remove(uuid);
                resetTasks.remove(uuid);
                player.sendActionBar(" ");
                // [MỞ KHÓA]: Khi hết 1 giây (Timeout)
                if (player.hasMetadata("Is_Combo_Casting")) player.removeMetadata("Is_Combo_Casting", plugin);
            }
        };
        resetTask.runTaskLater(plugin, 20L); // Có 1 giây để bấm phím tiếp theo
        resetTasks.put(uuid, resetTask);

        // KÍCH HOẠT CHIÊU
        if (combo.length() == 4) {
            String mappedSkillId = rpgPlayer.getSkillData().getSkillByCombo(combo);
            if (mappedSkillId != null) {
                ISkill skill = skillManager.getSkill(mappedSkillId);
                if (skill != null) {
                    int level = rpgPlayer.getSkillData().getSkillLevel(mappedSkillId);

                    if (skill.execute(player, rpgPlayer, level)) {
                        player.sendActionBar("§a§l✔ Kích hoạt thành công!");

                        player.setMetadata("Skill_Just_Cast", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
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
            // [MỞ KHÓA]: Khi đã nhập xong 4 phím
            if (player.hasMetadata("Is_Combo_Casting")) player.removeMetadata("Is_Combo_Casting", plugin);
        }
    }
}