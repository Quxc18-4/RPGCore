package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.SkillManager;

import java.util.*;

public class SkillUIManager implements Listener {

    private final PlayerManager playerManager;
    private final SkillManager skillManager;

    private final String SKILL_TITLE = "§8§lKỸ NĂNG NHÂN VẬT";
    private final String BINDS_TITLE = "§8§lTỔ HỢP PHÍM (COMBO)";

    private final Map<UUID, String> pendingComboBind = new HashMap<>();
    private final Map<UUID, String> pendingSkillBind = new HashMap<>();
    private final Map<UUID, Integer> playerSkillPages = new HashMap<>();

    private final String[] SUPPORTED_COMBOS = { "RLLL", "RLLR", "RLRL", "RLRR", "RRLL", "RRLR", "RRRL", "RRRR" };
    private final int[] ICON_SLOTS = {10, 12, 14, 19, 21, 23, 28, 30, 32, 37, 39, 41};
    private final int[] PANE_SLOTS = {11, 13, 15, 20, 22, 24, 29, 31, 33, 38, 40, 42};

    public SkillUIManager(PlayerManager playerManager, SkillManager skillManager) {
        this.playerManager = playerManager;
        this.skillManager = skillManager;
    }

    public void openSkillGUI(Player player, String targetComboToBind, int page) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        playerSkillPages.put(player.getUniqueId(), page);
        if (targetComboToBind != null) pendingComboBind.put(player.getUniqueId(), targetComboToBind);
        else pendingComboBind.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, SKILL_TITLE);
        fillBackground(inv);

        List<ISkill> availableSkills = skillManager.getSkillsForClass(rpgPlayer.getCurrentClass().getId());

        int sp = rpgPlayer.getSkillData().getSkillPoints();
        int startIndex = page * 12;

        for (int i = 0; i < 12; i++) {
            int skillIndex = startIndex + i;
            if (skillIndex >= availableSkills.size()) break;

            ISkill skill = availableSkills.get(skillIndex);
            String skillId = skill.getId();
            int skillLevel = rpgPlayer.getSkillData().getSkillLevel(skillId);
            String boundCombo = getComboBySkill(rpgPlayer, skillId);

            List<String> lore = new ArrayList<>();
            lore.add("§fCấp độ: §e" + skillLevel + "/" + skill.getMaxLevel());
            lore.add("§fTiêu hao: §b" + skill.getManaCost(skillLevel) + " Mana");
            lore.add("§fHồi chiêu: §6" + (skill.getCooldown(skillLevel)/1000.0) + "s");
            lore.add("");
            lore.addAll(skill.getDescription(skillLevel));

            ItemStack icon = createItem(skill.getIcon(), "§a§l" + skill.getName(), lore.toArray(new String[0]));
            inv.setItem(ICON_SLOTS[i], icon);

            Material paneMat = Material.GRAY_STAINED_GLASS_PANE;
            String status = "§7Chưa mở khóa";
            if (skillLevel > 0) {
                paneMat = (boundCombo != null) ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                status = (boundCombo != null) ? "§aĐã gán: §l" + boundCombo : "§cChưa gán phím";
            }

            if (targetComboToBind != null) {
                inv.setItem(PANE_SLOTS[i], createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lCLICK ĐỂ CHỌN", "§7Gán vào nút: §b" + targetComboToBind));
            } else {
                // UI MỚI: Tách biệt hiển thị Max Level và Chưa Max Level
                if (skillLevel >= skill.getMaxLevel()) {
                    inv.setItem(PANE_SLOTS[i], createItem(paneMat, status,
                            "§6§l✦ ĐÃ THÀNH THẠO ✦", // Không hiện SP, không hiện nút nâng cấp
                            "",
                            "§f[Chuột Phải] §bGán phím"));
                } else {
                    // Gọi hàm getUpgradeCost() thay vì check tên skill
                    int upgradeCost = skill.getUpgradeCost(skillLevel);
                    inv.setItem(PANE_SLOTS[i], createItem(paneMat, status,
                            "§eĐiểm SP hiện có: " + sp,
                            "§cTiêu hao: " + upgradeCost + " SP", "",
                            "§f[Chuột Trái] §aNâng cấp", "§f[Chuột Phải] §bGán phím"));
                }
            }
        }

        if (page > 0) inv.setItem(26, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l◀ TRANG TRƯỚC"));
        if (startIndex + 12 < availableSkills.size()) inv.setItem(35, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lTRANG TIẾP ▶"));

        player.openInventory(inv);
    }

    public void openBindsGUI(Player player, String targetSkillToBind) {
        // ... (Phần BindsGUI giữ nguyên không thay đổi) ...
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        if (targetSkillToBind != null) pendingSkillBind.put(player.getUniqueId(), targetSkillToBind);
        else pendingSkillBind.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, BINDS_TITLE);
        fillBackground(inv);

        int[] comboSlots = {9, 10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < SUPPORTED_COMBOS.length; i++) {
            String combo = SUPPORTED_COMBOS[i];
            String boundSkillId = rpgPlayer.getSkillData().getSkillByCombo(combo);

            if (targetSkillToBind != null) {
                inv.setItem(comboSlots[i], createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l" + combo, "§7Click để gán vào đây"));
            } else {
                if (boundSkillId != null) {
                    ISkill boundSkill = skillManager.getSkill(boundSkillId);
                    String displayName = (boundSkill != null) ? boundSkill.getName() : boundSkillId;

                    inv.setItem(comboSlots[i], createItem(Material.LIME_STAINED_GLASS_PANE, "§a§l" + combo,
                            "§fĐang gán: §b" + displayName, "", "§f[Chuột Phải] §cHủy gán"));
                } else {
                    inv.setItem(comboSlots[i], createItem(Material.RED_STAINED_GLASS_PANE, "§c§l" + combo,
                            "§7Ô trống", "", "§f[Chuột Trái] §aGán kỹ năng"));
                }
            }
        }
        inv.setItem(17, createItem(Material.BARRIER, "§c§l◀ QUAY LẠI", "§7Trở về danh sách kỹ năng"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(SKILL_TITLE) && !title.equals(BINDS_TITLE)) return;
        event.setCancelled(true);

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        int slot = event.getSlot();

        if (title.equals(SKILL_TITLE)) {
            int page = playerSkillPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 26) { openSkillGUI(player, pendingComboBind.get(player.getUniqueId()), page - 1); return; }
            if (slot == 35) { openSkillGUI(player, pendingComboBind.get(player.getUniqueId()), page + 1); return; }

            int index = getIndexFromSlots(PANE_SLOTS, slot);
            if (index != -1) {
                List<ISkill> availableSkills = skillManager.getSkillsForClass(rpgPlayer.getCurrentClass().getId());
                int skillIndex = (page * 12) + index;

                if (skillIndex >= availableSkills.size()) return;

                String skillId = availableSkills.get(skillIndex).getId();
                String pendingCombo = pendingComboBind.get(player.getUniqueId());

                if (pendingCombo != null) {
                    rpgPlayer.getSkillData().bindCombo(pendingCombo, skillId);
                    player.sendMessage("§aĐã gán kỹ năng vào tổ hợp " + pendingCombo);
                    openBindsGUI(player, null);
                } else {
                    if (event.getClick() == ClickType.LEFT) {
                        ISkill skill = availableSkills.get(skillIndex);
                        int currentLevel = rpgPlayer.getSkillData().getSkillLevel(skillId);

                        // Gọi hàm lấy Cost thay vì kiểm tra If
                        int upgradeCost = skill.getUpgradeCost(currentLevel);

                        if (currentLevel >= skill.getMaxLevel()) {
                            player.sendMessage("§cKỹ năng này đã đạt cấp độ tối đa!");
                        } else if (rpgPlayer.getSkillData().getSkillPoints() >= upgradeCost) {
                            rpgPlayer.getSkillData().setSkillLevel(skillId, currentLevel + 1);
                            rpgPlayer.getSkillData().setSkillPoints(rpgPlayer.getSkillData().getSkillPoints() - upgradeCost);

                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                            openSkillGUI(player, null, page);
                        } else {
                            player.sendMessage("§cKhông đủ điểm Kỹ Năng (SP)! Cần " + upgradeCost + " SP.");
                        }
                    } else if (event.getClick() == ClickType.RIGHT) {
                        if (rpgPlayer.getSkillData().getSkillLevel(skillId) > 0) openBindsGUI(player, skillId);
                        else player.sendMessage("§cBạn phải nâng cấp kỹ năng này trước khi gán phím!");
                    }
                }
            }
        }

        if (title.equals(BINDS_TITLE)) {
            if (slot == 17) { openSkillGUI(player, null, 0); return; }

            String comboName = clickedItem.getItemMeta().getDisplayName().substring(4);
            String pendingSkill = pendingSkillBind.get(player.getUniqueId());

            if (pendingSkill != null) {
                rpgPlayer.getSkillData().bindCombo(comboName, pendingSkill);
                player.sendMessage("§aĐã gán kỹ năng vào tổ hợp " + comboName);
                openSkillGUI(player, null, 0);
            } else {
                if (clickedItem.getType() == Material.RED_STAINED_GLASS_PANE && event.getClick() == ClickType.LEFT) {
                    openSkillGUI(player, comboName, 0);
                } else if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE && event.getClick() == ClickType.RIGHT) {
                    rpgPlayer.getSkillData().unbindCombo(comboName);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                    openBindsGUI(player, null);
                }
            }
        }
    }

    private void fillBackground(Inventory inv) {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, bg);
    }

    private int getIndexFromSlots(int[] array, int value) {
        for (int i = 0; i < array.length; i++) if (array[i] == value) return i;
        return -1;
    }

    private String getComboBySkill(RPGPlayer player, String skillId) {
        for (Map.Entry<String, String> entry : player.getSkillData().getBinds().entrySet()) {
            if (entry.getValue().equals(skillId)) return entry.getKey();
        }
        return null;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}