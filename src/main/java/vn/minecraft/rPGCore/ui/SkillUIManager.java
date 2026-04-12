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

            // 1. LẤY LINH HOẠT TÊN TÀI NGUYÊN (Mana, Rage, Stamina...)
            String resourceName = rpgPlayer.getCurrentClass().getResourceType().getName();
            double cost = skill.getManaCost(skillLevel);

            if (cost > 0) {
                // Đổi màu hiển thị cho hợp lý: Nộ (Đỏ/Tím), Mana (Xanh), Stamina (Vàng)
                String costColor = resourceName.equals("Rage") ? "§c" : (resourceName.equals("Stamina") ? "§e" : "§b");

                // [ĐÃ FIX]: Nếu là Nội tại mà có tốn năng lượng -> Ghi chú thêm (Khi Bật)
                String condition = skill.isPassiveSkill() ? " §7(Khi Bật)" : "";

                lore.add("§fTiêu hao: " + costColor + cost + " " + resourceName + condition);
            } else {
                // Nếu thực sự không tốn năng lượng
                lore.add("§fTiêu hao: §aKhông");
            }

            // 2. CHỈ HIỂN THỊ HỒI CHIÊU NẾU > 0
            long cooldown = skill.getCooldown(skillLevel);
            if (cooldown > 0) {
                lore.add("§fHồi chiêu: §6" + (cooldown / 1000.0) + "s");
            }

            lore.add("");
            lore.addAll(skill.getDescription(skillLevel));

            ItemStack icon = createItem(skill.getIcon(), "§a§l" + skill.getName(), lore.toArray(new String[0]));
            inv.setItem(ICON_SLOTS[i], icon);

            Material paneMat = Material.GRAY_STAINED_GLASS_PANE;
            String status = "§7Chưa mở khóa";
            if (skillLevel > 0) {
                if (skill.isPassiveSkill()) {
                    boolean isOff = "OFF".equals(rpgPlayer.getSkillData().getBinds().get("TOGGLE_" + skillId));
                    paneMat = isOff ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
                    status = isOff ? "§cĐang Tắt" : "§aĐang Bật";
                } else {
                    paneMat = (boundCombo != null) ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                    status = (boundCombo != null) ? "§aĐã gán: §l" + boundCombo : "§cChưa gán phím";
                }
            }

            // 1. KIỂM TRA ĐIỀU KIỆN MỞ KHÓA (SKILL TREE)
            boolean meetsRequirements = true;
            List<String> reqLore = new ArrayList<>();

            // KIỂM TRA CẤP ĐỘ NGƯỜI CHƠI (Sử dụng Level mặc định của Minecraft)
            if (player.getLevel() < skill.getRequiredPlayerLevel()) {
                meetsRequirements = false;
                reqLore.add("§c✖ Yêu cầu: Đạt Cấp độ " + skill.getRequiredPlayerLevel());
            } else if (skill.getRequiredPlayerLevel() > 1) {
                reqLore.add("§a✔ Đạt yêu cầu: Cấp độ " + skill.getRequiredPlayerLevel());
            }

            Map<String, Integer> reqs = skill.getRequiredSkills();
            if (reqs != null && !reqs.isEmpty()) {
                reqLore.add("§8-----------------");
                for (Map.Entry<String, Integer> req : reqs.entrySet()) {
                    int pLevel = rpgPlayer.getSkillData().getSkillLevel(req.getKey());
                    ISkill reqSkill = skillManager.getSkill(req.getKey());
                    String reqName = (reqSkill != null) ? reqSkill.getName() : req.getKey();

                    if (pLevel < req.getValue()) {
                        meetsRequirements = false;
                        reqLore.add("§c✖ Yêu cầu: " + reqName + " (Lv " + req.getValue() + ")");
                    } else {
                        reqLore.add("§a✔ Đạt yêu cầu: " + reqName);
                    }
                }
            }

            // 2. VẼ UI DỰA TRÊN TRẠNG THÁI
            if (targetComboToBind != null) {
                // CHẶN GÁN PASSIVE VÀO COMBO HOẶC KỸ NĂNG KHÔNG CHO GÁN
                if (skill.isPassiveSkill()) {
                    inv.setItem(PANE_SLOTS[i], createItem(Material.BARRIER, "§c§lKHÔNG THỂ GÁN", "§7Kỹ năng nội tại hoặc không dùng phím."));
                } else {
                    inv.setItem(PANE_SLOTS[i], createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lCLICK ĐỂ CHỌN", "§7Gán vào nút: §b" + targetComboToBind));
                }
            } else {

                // ==========================================
                // [ĐÃ SỬA]: KIỂM TRA ACTION CHUỘT PHẢI HIỂN THỊ TRÊN LORE
                // ==========================================
                String rightClickAction;
                if (skillId.equals("mage_elemental_swap")) {
                    rightClickAction = "§f[Chuột Phải] §bMở bảng sắp xếp Nguyên tố";
                } else if (skill.isPassiveSkill()) {
                    rightClickAction = "§f[Chuột Phải] §bBật/Tắt nội tại";
                } else {
                    rightClickAction = "§f[Chuột Phải] §bGán phím";
                }

                if (skillLevel >= skill.getMaxLevel()) {
                    inv.setItem(PANE_SLOTS[i], createItem(paneMat, status, "§6§l✦ ĐÃ THÀNH THẠO ✦", "", rightClickAction));
                } else if (!meetsRequirements && skillLevel == 0) {
                    // NẾU CHƯA ĐẠT YÊU CẦU: Khóa nâng cấp và hiện dòng chữ đỏ
                    List<String> lockedLore = new ArrayList<>();
                    lockedLore.add("§c§lCHƯA ĐỦ ĐIỀU KIỆN MỞ KHÓA!");
                    lockedLore.addAll(reqLore);
                    inv.setItem(PANE_SLOTS[i], createItem(Material.BARRIER, status, lockedLore.toArray(new String[0])));
                } else {
                    int upgradeCost = skill.getUpgradeCost(skillLevel);
                    List<String> upgLore = new ArrayList<>();
                    upgLore.add("§eĐiểm SP hiện có: " + sp);
                    upgLore.add("§cTiêu hao: " + upgradeCost + " SP");
                    upgLore.addAll(reqLore); // Hiện list yêu cầu (Màu xanh) cho đẹp
                    upgLore.add("");
                    upgLore.add("§f[Chuột Trái] §aNâng cấp");
                    upgLore.add(rightClickAction); // Lắp text đã lọc vào đây

                    inv.setItem(PANE_SLOTS[i], createItem(paneMat, status, upgLore.toArray(new String[0])));
                }
            }
        }

        if (page > 0) inv.setItem(26, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l◀ TRANG TRƯỚC"));
        if (startIndex + 12 < availableSkills.size()) inv.setItem(35, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lTRANG TIẾP ▶"));

        player.openInventory(inv);
    }

    public void openBindsGUI(Player player, String targetSkillToBind) {
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

                // Kéo biến skill lên trên cùng để tất cả các nhánh đều có thể kiểm tra
                ISkill skill = availableSkills.get(skillIndex);

                if (pendingCombo != null) {
                    // ==========================================
                    // [ĐÃ FIX]: CHẶN LỖ HỔNG GÁN KỸ NĂNG NỘI TẠI TỪ LỆNH /BINDS
                    // ==========================================
                    if (skill.isPassiveSkill()) {
                        player.sendMessage("§c§l[!] §fKỹ năng này là Nội tại hoặc không hỗ trợ dùng phím tắt!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return; // Ngừng chạy tiếp, không cho gán!
                    }

                    rpgPlayer.getSkillData().bindCombo(pendingCombo, skillId);
                    player.sendMessage("§aĐã gán kỹ năng vào tổ hợp " + pendingCombo);
                    openBindsGUI(player, null);
                } else {
                    // Nhánh Click Trái / Phải để nâng cấp hoặc Bật/Tắt
                    if (event.getClick() == ClickType.LEFT) {
                        int currentLevel = rpgPlayer.getSkillData().getSkillLevel(skillId);

                        int upgradeCost = skill.getUpgradeCost(currentLevel);

                        // Lấy yêu cầu để check lại lần nữa khi click
                        boolean canUpgrade = true;
                        if (skill.getRequiredSkills() != null) {
                            for (Map.Entry<String, Integer> req : skill.getRequiredSkills().entrySet()) {
                                if (rpgPlayer.getSkillData().getSkillLevel(req.getKey()) < req.getValue()) {
                                    canUpgrade = false;
                                    break;
                                }
                            }
                        }

                        if (currentLevel >= skill.getMaxLevel()) {
                            player.sendMessage("§cKỹ năng này đã đạt cấp độ tối đa!");
                        } else if (!canUpgrade && currentLevel == 0) {
                            player.sendMessage("§cBạn chưa đạt yêu cầu của Cây Kỹ Năng để mở khóa chiêu này!");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        } else if (rpgPlayer.getSkillData().getSkillPoints() >= upgradeCost) {
                            rpgPlayer.getSkillData().setSkillLevel(skillId, currentLevel + 1);
                            rpgPlayer.getSkillData().setSkillPoints(rpgPlayer.getSkillData().getSkillPoints() - upgradeCost);

                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                            openSkillGUI(player, null, page);
                        } else {
                            player.sendMessage("§cKhông đủ điểm Kỹ Năng (SP)! Cần " + upgradeCost + " SP.");
                        }
                    } else if (event.getClick() == ClickType.RIGHT) {
                        if (rpgPlayer.getSkillData().getSkillLevel(skillId) > 0) {

                            // NẾU LÀ NỘI TẠI -> THỰC HIỆN BẬT / TẮT
                            if (skill.isPassiveSkill()) {
                                // [MỚI]: Bắt riêng sự kiện cho Kỹ năng Chuyển Hóa Nguyên Tố
                                if (skillId.equals("mage_elemental_swap")) {
                                    vn.minecraft.rPGCore.ui.ElementRotationGUI.openGUI(player, rpgPlayer);
                                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
                                    return;
                                }

                                // Logic bật tắt cũ giữ nguyên
                                boolean isOff = "OFF".equals(rpgPlayer.getSkillData().getBinds().get("TOGGLE_" + skillId));
                                if (isOff) {
                                    rpgPlayer.getSkillData().getBinds().remove("TOGGLE_" + skillId); // Bật lại
                                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
                                } else {
                                    rpgPlayer.getSkillData().getBinds().put("TOGGLE_" + skillId, "OFF"); // Tắt
                                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
                                }
                                openSkillGUI(player, null, page);
                            }
                            // NẾU LÀ CHỦ ĐỘNG -> MỞ MENU GÁN PHÍM
                            else {
                                openBindsGUI(player, skillId);
                            }
                        } else {
                            player.sendMessage("§cBạn phải nâng cấp kỹ năng này trước khi thao tác!");
                        }
                    }
                }
            }
        }

        if (title.equals(BINDS_TITLE)) {
            if (slot == 17) { openSkillGUI(player, null, 0); return; }

            String comboName = clickedItem.getItemMeta().getDisplayName().substring(4);
            String pendingSkill = pendingSkillBind.get(player.getUniqueId());

            if (pendingSkill != null) {
                // ==========================================
                // [PHÒNG THỦ KÉP]: Chặn đứng mọi nỗ lực gán Passive
                // ==========================================
                ISkill targetSkill = skillManager.getSkill(pendingSkill);
                if (targetSkill != null && targetSkill.isPassiveSkill()) {
                    player.sendMessage("§c§l[!] §fLỗi hệ thống: Kỹ năng nội tại không thể gán phím!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    pendingSkillBind.remove(player.getUniqueId()); // Xóa rác
                    openSkillGUI(player, null, 0);
                    return;
                }
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

            // [MỚI]: Ép các cờ (Flags) ẩn toàn bộ chỉ số Vanilla rườm rà để UI chuyên nghiệp
            meta.addItemFlags(
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                    org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE
            );

            item.setItemMeta(meta);
        }
        return item;
    }
}