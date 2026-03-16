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

        // 1. NHẬN DIỆN VŨ KHÍ NÀY LÀ CỦA PHÁI NÀO
        String requiredClass = null;
        boolean isWeapon = false;

        if (mainType == Material.BOW || mainType == Material.CROSSBOW) {
            isWeapon = true;
            if (!classId.equals("archer")) requiredClass = "Cung Thủ (Archer)";
        } else if (mainType == Material.SHEARS) {
            isWeapon = true;
            if (!classId.equals("assassin")) requiredClass = "Sát Thủ (Assassin)";
        } else if (mainType == Material.STICK || mainType == Material.BLAZE_ROD) {
            isWeapon = true;
            if (!classId.equals("mage")) requiredClass = "Pháp Sư (Mage)";
        } else if (mainType == Material.LEAD) {
            isWeapon = true;
            if (!classId.equals("summoner")) requiredClass = "Triệu Hồi Sư (Summoner)";
        } else if (mainType.name().endsWith("_HOE")) {
            isWeapon = true;
            if (!classId.equals("reaper")) requiredClass = "Tử Thần (Reaper)";
        } else if (mainType.name().endsWith("_SWORD") || mainType.name().endsWith("_AXE")) {
            isWeapon = true;
            if (!classId.equals("warrior") && !classId.equals("guardian") && !classId.equals("commoner")) {
                requiredClass = "Chiến Binh / Vệ Binh";
            }
        }

        // Nếu cầm cục đất, lúa mì... thì không phải vũ khí -> Bỏ qua combo
        if (!isWeapon) return;

        // BÁO LỖI: VŨ KHÍ KHÔNG PHẢI CỦA MÌNH
        if (requiredClass != null) {
            player.sendMessage("§c§l[!] §cVũ khí này chỉ dành cho hệ phái " + requiredClass + "!");
            return;
        }

        // 2. VŨ KHÍ ĐÚNG PHÁI -> GỌI EQUIPMENT LISTENER ĐỂ KIỂM TRA LORE (LEVEL)
        EquipmentListener eqListener = new EquipmentListener(playerManager);
        if (!eqListener.canPlayerUse(player, mainItem)) {
            return; // Hàm canPlayerUse của bạn đã tự động gửi tin nhắn báo Level, cấm vận chiêu!
        }

        if (player.hasMetadata("Is_Casting")) return;

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
        }
    }
}