package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.managers.ArrowRecipeManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.*;

public class ArrowCraftingUI implements Listener {

    private final PlayerManager playerManager;
    private final ArrowRecipeManager recipeManager; // Thêm Manager quản lý công thức

    public static final String UI_TITLE = "§8§lCHẾ TẠO MŨI TÊN";

    private final int[] BLUE_GLASS_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private final int RESULT_SLOT = 23;
    private final int CRAFT_BTN_SLOT = 41;
    private final int[] RECIPE_LIST_SLOTS = {16, 25, 34, 43};
    private final int BTN_PREV_PAGE = 26;
    private final int BTN_NEXT_PAGE = 35;

    // Lưu trữ trang hiện tại và công thức đang chọn của người chơi
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> playerSelectedRecipe = new HashMap<>();

    public ArrowCraftingUI(PlayerManager playerManager, ArrowRecipeManager recipeManager) {
        this.playerManager = playerManager;
        this.recipeManager = recipeManager;
    }

    public void openCraftingGUI(Player player, int page) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        if (rpgPlayer.getSkillData().getSkillLevel("archer_arrow_crafting") < 1) {
            player.sendMessage("§c§l(!) §cBạn chưa học kỹ năng [Chế Tác Tiễn] để sử dụng bàn này!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        playerPages.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, UI_TITLE);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        ItemStack blueBg = createItem(Material.BLUE_STAINED_GLASS_PANE, "§bÔ trống");
        for (int slot : BLUE_GLASS_SLOTS) inv.setItem(slot, blueBg);

        inv.setItem(CRAFT_BTN_SLOT, createItem(Material.LIME_STAINED_GLASS_PANE, "§a§lTIẾN HÀNH CHẾ TẠO", "§7Click để chế tạo mũi tên đang chọn"));
        inv.setItem(RESULT_SLOT, createItem(Material.BARRIER, "§c§lChưa chọn công thức", "§7Hãy chọn 1 loại mũi tên ở bên phải"));

        if (page > 0) inv.setItem(BTN_PREV_PAGE, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l◀ TRANG TRƯỚC"));
        else inv.setItem(BTN_PREV_PAGE, bg);

        // ==========================================
        // ĐỔ DỮ LIỆU CÔNG THỨC VÀO CÁC Ô BÊN PHẢI
        // ==========================================
        List<String> recipeIds = new ArrayList<>(recipeManager.getRecipes().keySet());
        int startIndex = page * RECIPE_LIST_SLOTS.length;

        for (int i = 0; i < RECIPE_LIST_SLOTS.length; i++) {
            int slot = RECIPE_LIST_SLOTS[i];
            int recipeIndex = startIndex + i;

            if (recipeIndex < recipeIds.size()) {
                String recipeId = recipeIds.get(recipeIndex);
                ArrowRecipeManager.RecipeData data = recipeManager.getRecipes().get(recipeId);

                // Copy item kết quả ra làm Icon hiển thị
                ItemStack icon = data.result.clone();
                ItemMeta meta = icon.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add("§a§l▶ CLICK ĐỂ XEM CÔNG THỨC");
                lore.add("§8" + recipeId); // Dòng chữ tàng hình để code nhận diện ID công thức
                meta.setLore(lore);
                icon.setItemMeta(meta);

                inv.setItem(slot, icon);
            } else {
                inv.setItem(slot, bg); // Nếu hết công thức thì để ô trống
            }
        }

        if (startIndex + RECIPE_LIST_SLOTS.length < recipeIds.size()) {
            inv.setItem(BTN_NEXT_PAGE, createItem(Material.LIME_STAINED_GLASS_PANE, "§a§lTRANG TIẾP ▶"));
        } else {
            inv.setItem(BTN_NEXT_PAGE, bg);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(UI_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == BTN_PREV_PAGE && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            openCraftingGUI(player, currentPage - 1);
        } else if (slot == BTN_NEXT_PAGE && event.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            openCraftingGUI(player, currentPage + 1);
        } else if (slot == CRAFT_BTN_SLOT) {
            String selected = playerSelectedRecipe.get(player.getUniqueId());
            if (selected == null) {
                player.sendMessage("§c(!) Bạn chưa chọn công thức nào bên phải!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            ArrowRecipeManager.RecipeData data = recipeManager.getRecipes().get(selected);
            if (data == null) return;

            // 1. Kiểm tra không gian túi đồ
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§c(!) Túi đồ của bạn đã đầy, không thể chứa thêm mũi tên!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 2. Kiểm tra xem người chơi có đủ MỌI nguyên liệu không
            boolean hasAllIngredients = true;
            for (ItemStack ingredient : data.ingredients.values()) {
                if (!hasItem(player, ingredient)) {
                    hasAllIngredients = false;
                    break;
                }
            }

            if (!hasAllIngredients) {
                player.sendMessage("§c(!) Bạn không có đủ nguyên liệu yêu cầu trong túi đồ!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 3. Tiến hành thu nguyên liệu (Trừ đồ)
            for (ItemStack ingredient : data.ingredients.values()) {
                consumeItem(player, ingredient);
            }

            // 4. Trả thành phẩm (Mũi tên đặc biệt)
            player.getInventory().addItem(data.result.clone());

            player.sendMessage("§a§l[!] §aChế tạo thành công: §f" + (data.result.getItemMeta().hasDisplayName() ? data.result.getItemMeta().getDisplayName() : data.result.getType().name()));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1.2f);

            // Ép Inventory cập nhật lại để tránh lỗi hiển thị ảo (Ghost Item)
            player.updateInventory();

        } else if (isRecipeSlot(slot) && event.getCurrentItem().getType() != Material.BLACK_STAINED_GLASS_PANE) {

            // ==========================================
            // LOGIC KHI NGƯỜI CHƠI CLICK VÀO CÔNG THỨC
            // ==========================================
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                List<String> lore = clickedItem.getItemMeta().getLore();
                String hiddenId = lore.get(lore.size() - 1).replace("§8", ""); // Lấy ID ẩn

                ArrowRecipeManager.RecipeData data = recipeManager.getRecipes().get(hiddenId);
                if (data != null) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    playerSelectedRecipe.put(player.getUniqueId(), hiddenId);

                    Inventory inv = event.getInventory();

                    // Xóa sạch lưới cũ
                    ItemStack blueBg = createItem(Material.BLUE_STAINED_GLASS_PANE, "§bÔ trống");
                    for (int s : BLUE_GLASS_SLOTS) inv.setItem(s, blueBg);

                    // Rải nguyên liệu ra đúng vị trí (0 -> 8)
                    for (Map.Entry<Integer, ItemStack> entry : data.ingredients.entrySet()) {
                        int gridIndex = entry.getKey();
                        if (gridIndex >= 0 && gridIndex < 9) {
                            inv.setItem(BLUE_GLASS_SLOTS[gridIndex], entry.getValue());
                        }
                    }

                    // Hiển thị kết quả ra giữa
                    inv.setItem(RESULT_SLOT, data.result);
                }
            }
        }
    }

    private boolean isRecipeSlot(int slot) {
        for (int s : RECIPE_LIST_SLOTS) if (s == slot) return true;
        return false;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean hasItem(Player player, ItemStack required) {
        return vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().countItem(player, required) >= required.getAmount();
    }

    private void consumeItem(Player player, ItemStack required) {
        vn.minecraft.rPGCore.RPGCore.getInstance().getItemManager().takeItem(player, required, required.getAmount());
    }

    // =========================================
    // MỞ GIAO DIỆN CHẾ TẠO MŨI TÊN
    // =========================================
    @EventHandler
    public void onFletchingTableClick(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.FLETCHING_TABLE) return;

        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

        if (rpgPlayer != null && rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("archer")) {
            event.setCancelled(true);
            openCraftingGUI(player, 0); // Mở trang 0
        }
    }
}