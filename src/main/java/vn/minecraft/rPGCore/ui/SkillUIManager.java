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

import java.util.*;

public class SkillUIManager implements Listener {

    private final PlayerManager playerManager;
    private final String SKILL_TITLE = "§8§lKỸ NĂNG NHÂN VẬT";
    private final String BINDS_TITLE = "§8§lTỔ HỢP PHÍM (COMBO)";

    private final Map<UUID, String> pendingComboBind = new HashMap<>();
    private final Map<UUID, String> pendingSkillBind = new HashMap<>();
    private final Map<UUID, Integer> playerSkillPages = new HashMap<>(); // Lưu trang hiện tại

    // 16 Tổ hợp phím chuẩn (Chỉ có 4 phím)
    private final String[] SUPPORTED_COMBOS = {
            "LLLL", "LLLR", "LLRL", "LLRR",
            "LRLL", "LRLR", "LRRL", "LRRR",
            "RLLL", "RLLR", "RLRL", "RLRR",
            "RRLL", "RRLR", "RRRL", "RRRR"
    };

    // Tọa độ 12 Kỹ năng (Icon và Kính trạng thái xếp cạnh nhau)
    private final int[] ICON_SLOTS = {10, 12, 14, 19, 21, 23, 28, 30, 32, 37, 39, 41};
    private final int[] PANE_SLOTS = {11, 13, 15, 20, 22, 24, 29, 31, 33, 38, 40, 42};

    public SkillUIManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    // ==========================================
    // 1. GIAO DIỆN /SKILL (CÓ PHÂN TRANG)
    // ==========================================
    public void openSkillGUI(Player player, String targetComboToBind, int page) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        playerSkillPages.put(player.getUniqueId(), page);
        if (targetComboToBind != null) pendingComboBind.put(player.getUniqueId(), targetComboToBind);
        else pendingComboBind.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, SKILL_TITLE);
        fillBackground(inv); // Phủ kính đen

        // Lấy danh sách kỹ năng (Hiện tại giả lập 1 kỹ năng test)
        List<String> availableSkills = new ArrayList<>();
        if (rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("archer")) {
            availableSkills.add("archer_spread");
            // Thêm các kỹ năng ảo để test phân trang nếu cần:
            // for(int i=0; i<15; i++) availableSkills.add("dummy_skill_" + i);
        }

        int sp = rpgPlayer.getSkillPoints();
        int startIndex = page * 12;

        for (int i = 0; i < 12; i++) {
            int skillIndex = startIndex + i;
            if (skillIndex >= availableSkills.size()) break;

            String skillId = availableSkills.get(skillIndex);
            int skillLevel = rpgPlayer.getSkillLevel(skillId);
            String boundCombo = getComboBySkill(rpgPlayer, skillId);

            // Vẽ Icon (Tùy chỉnh nội dung theo skillId)
            ItemStack icon;
            if (skillId.equals("archer_spread")) {
                icon = createItem(Material.ARROW, "§a§lMưa Tên Kép",
                        "§fCấp độ: §e" + skillLevel, "§fTiêu hao: §b30 Mana", "§fHồi chiêu: §65.0s", "",
                        "§7Sát thương: §c" + (45 + (Math.max(0, skillLevel - 1)) * 5) + "%",
                        "§7Xuyên giáp: §e" + (20 + (Math.max(0, skillLevel - 1)) * 1) + "%");
            } else {
                icon = createItem(Material.BOOK, "§7" + skillId);
            }
            inv.setItem(ICON_SLOTS[i], icon);

            // Vẽ Kính Trạng Thái
            Material paneMat = Material.GRAY_STAINED_GLASS_PANE;
            String status = "§7Chưa mở khóa";
            if (skillLevel > 0) {
                paneMat = (boundCombo != null) ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                status = (boundCombo != null) ? "§aĐã gán: §l" + boundCombo : "§cChưa gán phím";
            }

            if (targetComboToBind != null) {
                inv.setItem(PANE_SLOTS[i], createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lCLICK ĐỂ CHỌN", "§7Gán vào nút: §b" + targetComboToBind));
            } else {
                inv.setItem(PANE_SLOTS[i], createItem(paneMat, status,
                        "§eĐiểm SP hiện có: " + sp, "",
                        "§f[Chuột Trái] §aNâng cấp kỹ năng", "§f[Chuột Phải] §bGán tổ hợp phím"));
            }
        }

        // Vẽ Nút Phân Trang
        if (page > 0) {
            inv.setItem(26, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l◀ TRANG TRƯỚC"));
        }
        if (startIndex + 12 < availableSkills.size()) {
            inv.setItem(35, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lTRANG TIẾP ▶"));
        }

        player.openInventory(inv);
    }

    // ==========================================
    // 2. GIAO DIỆN /BINDS
    // ==========================================
    public void openBindsGUI(Player player, String targetSkillToBind) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        if (targetSkillToBind != null) pendingSkillBind.put(player.getUniqueId(), targetSkillToBind);
        else pendingSkillBind.remove(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, BINDS_TITLE);
        fillBackground(inv);

        // Xếp 16 ô ra giữa rương cho đẹp (4 hàng x 4 cột)
        int[] comboSlots = {11, 12, 14, 15, 20, 21, 23, 24, 29, 30, 32, 33, 38, 39, 41, 42};

        for (int i = 0; i < SUPPORTED_COMBOS.length; i++) {
            String combo = SUPPORTED_COMBOS[i];
            String boundSkill = rpgPlayer.getSkillByCombo(combo);

            if (targetSkillToBind != null) {
                inv.setItem(comboSlots[i], createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l" + combo, "§7Click để gán kỹ năng vào đây"));
            } else {
                if (boundSkill != null) {
                    inv.setItem(comboSlots[i], createItem(Material.LIME_STAINED_GLASS_PANE, "§a§l" + combo,
                            "§fĐang gán: §b" + boundSkill, "", "§f[Chuột Phải] §cHủy gán (Unbind)"));
                } else {
                    inv.setItem(comboSlots[i], createItem(Material.RED_STAINED_GLASS_PANE, "§c§l" + combo,
                            "§7Ô trống", "", "§f[Chuột Trái] §aGán kỹ năng"));
                }
            }
        }
        player.openInventory(inv);
    }

    // ==========================================
    // 3. XỬ LÝ CLICK EVENT
    // ==========================================
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

        // --- XỬ LÝ MENU SKILL ---
        if (title.equals(SKILL_TITLE)) {
            int page = playerSkillPages.getOrDefault(player.getUniqueId(), 0);

            if (slot == 26) openSkillGUI(player, pendingComboBind.get(player.getUniqueId()), page - 1);
            else if (slot == 35) openSkillGUI(player, pendingComboBind.get(player.getUniqueId()), page + 1);

            // Tìm xem người chơi có click vào ô Trạng Thái (Pane) không
            int index = getIndexFromSlots(PANE_SLOTS, slot);
            if (index != -1) {
                // Hiện tại giả định ô đầu tiên luôn là archer_spread
                String skillId = "archer_spread";
                String pendingCombo = pendingComboBind.get(player.getUniqueId());

                if (pendingCombo != null) {
                    rpgPlayer.bindCombo(pendingCombo, skillId);
                    player.sendMessage("§aĐã gán kỹ năng vào tổ hợp " + pendingCombo);
                    openBindsGUI(player, null);
                } else {
                    if (event.getClick() == ClickType.LEFT) {
                        if (rpgPlayer.getSkillPoints() > 0) {
                            rpgPlayer.setSkillLevel(skillId, rpgPlayer.getSkillLevel(skillId) + 1);
                            rpgPlayer.setSkillPoints(rpgPlayer.getSkillPoints() - 1);
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                            openSkillGUI(player, null, page);
                        } else {
                            player.sendMessage("§cKhông đủ điểm Kỹ Năng (SP)!");
                        }
                    } else if (event.getClick() == ClickType.RIGHT) {
                        if (rpgPlayer.getSkillLevel(skillId) > 0) openBindsGUI(player, skillId);
                        else player.sendMessage("§cBạn phải nâng cấp kỹ năng này trước khi gán phím!");
                    }
                }
            }
        }

        // --- XỬ LÝ MENU BINDS ---
        if (title.equals(BINDS_TITLE)) {
            String comboName = clickedItem.getItemMeta().getDisplayName().substring(4); // Lấy chữ LLLR
            String pendingSkill = pendingSkillBind.get(player.getUniqueId());

            if (pendingSkill != null) {
                rpgPlayer.bindCombo(comboName, pendingSkill);
                player.sendMessage("§aĐã gán kỹ năng vào tổ hợp " + comboName);
                openSkillGUI(player, null, 0);
            } else {
                if (clickedItem.getType() == Material.RED_STAINED_GLASS_PANE && event.getClick() == ClickType.LEFT) {
                    openSkillGUI(player, comboName, 0);
                } else if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE && event.getClick() == ClickType.RIGHT) {
                    rpgPlayer.unbindCombo(comboName);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                    openBindsGUI(player, null);
                }
            }
        }
    }

    // Các hàm phụ trợ
    private void fillBackground(Inventory inv) {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);
    }

    private int getIndexFromSlots(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    private String getComboBySkill(RPGPlayer player, String skillId) {
        for (Map.Entry<String, String> entry : player.getBinds().entrySet()) {
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