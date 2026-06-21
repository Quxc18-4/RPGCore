package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.FixedTrinketManager;
import vn.minecraft.rPGCore.managers.FixedTrinketManager.FixedRecipe;

import java.util.*;

public class TrinketRecipeCraftGUI implements Listener {

    public static final String GUI_NAME = "§6§lChế Tác Chuyên Sâu";

    private final int[] INGREDIENT_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
    private final int[] RECIPE_SLOTS = {46, 47, 48, 49, 50, 51, 52};
    private final int CONFIRM_SLOT = 16;
    private final int RESULT_SLOT = 34;
    private final int PREV_PAGE_SLOT = 45;
    private final int NEXT_PAGE_SLOT = 53;

    private static final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public void openGUI(Player player) {
        sessions.put(player.getUniqueId(), new PlayerSession());

        // 1. Chỉ tạo Inventory đúng 1 lần duy nhất khi mở
        Inventory inv = Bukkit.createInventory(null, 54, GUI_NAME);

        // 2. Truyền Inventory đó vào hàm vẽ để sắp xếp đồ
        renderGUI(player, inv);

        // 3. Mở lên cho người chơi xem
        player.openInventory(inv);
    }

    // [CẬP NHẬT]: Hàm vẽ nhận thẳng cái Inventory đang mở, không tạo mới nữa
    private void renderGUI(Player player, Inventory inv) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        // Xóa sạch đồ cũ trên giao diện trước khi vẽ lại trang mới
        inv.clear();

        List<FixedRecipe> recipes = FixedTrinketManager.getInstance().getRecipes();

        ItemStack bgBlack = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack bgRed = createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c[ Ô trống ]");
        ItemStack bgGray = createGuiItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7[ Chưa mở khóa ]");

        for (int i = 0; i < 54; i++) inv.setItem(i, bgBlack);

        int totalPages = (int) Math.ceil(recipes.size() / 7.0);
        if (totalPages == 0) totalPages = 1;

        if (session.page > 0) inv.setItem(PREV_PAGE_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c§l◀ TRANG TRƯỚC", "§7Trang " + session.page));
        if (session.page < totalPages - 1) inv.setItem(NEXT_PAGE_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§lTRANG TIẾP ▶", "§7Trang " + (session.page + 2)));

        int startIndex = session.page * 7;
        for (int i = 0; i < 7; i++) {
            int slot = RECIPE_SLOTS[i];
            int recipeIndex = startIndex + i;

            if (recipeIndex < recipes.size()) {
                ItemStack displayItem = recipes.get(recipeIndex).result.clone();
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(session.selectedRecipe == recipeIndex ? "§a§l▶ ĐANG CHỌN" : "§e▶ Click để xem Nguyên liệu");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);

                inv.setItem(slot, displayItem);
            } else {
                inv.setItem(slot, bgGray);
            }
        }

        if (session.selectedRecipe != -1 && session.selectedRecipe < recipes.size()) {
            FixedRecipe recipe = recipes.get(session.selectedRecipe);

            inv.setItem(RESULT_SLOT, recipe.result);
            inv.setItem(CONFIRM_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§l[✔] TIẾN HÀNH CHẾ TÁC", "§7Nhấp vào để chế tạo trang sức này."));

            // [FIX 1]: Tách ảo hiển thị UI để giải quyết giới hạn 64 items/stack
            List<ItemStack> displayIngredients = new ArrayList<>();
            for (ItemStack req : recipe.ingredients) {
                int amountLeft = req.getAmount();
                ItemStack baseClone = req.clone();
                while (amountLeft > 0) {
                    int stackSize = Math.min(amountLeft, 64);
                    ItemStack splitStack = baseClone.clone();
                    splitStack.setAmount(stackSize);
                    displayIngredients.add(splitStack);
                    amountLeft -= stackSize;
                }
            }

            for (int i = 0; i < INGREDIENT_SLOTS.length; i++) {
                if (i < displayIngredients.size()) inv.setItem(INGREDIENT_SLOTS[i], displayIngredients.get(i));
                else inv.setItem(INGREDIENT_SLOTS[i], bgRed);
            }
        } else {
            inv.setItem(RESULT_SLOT, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "§9Thành Phẩm", "§7Hãy chọn một công thức ở dưới."));
            inv.setItem(CONFIRM_SLOT, createGuiItem(Material.BARRIER, "§c§lCHƯA CHỌN CÔNG THỨC"));
            for (int slot : INGREDIENT_SLOTS) inv.setItem(slot, bgRed);
        }

        // Ép client của Minecraft cập nhật ngay lập tức mà không chớp màn hình
        player.updateInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;
        event.setCancelled(true);

        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND ||
                event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        PlayerSession session = sessions.get(player.getUniqueId());

        if (session == null) {
            player.sendMessage("§c§l[!] §cPhiên giao dịch bị lỗi. Vui lòng tắt và mở lại Menu!");
            player.closeInventory();
            return;
        }

        if (slot >= 54 || slot < 0) return;

        List<FixedRecipe> recipes = FixedTrinketManager.getInstance().getRecipes();
        int totalPages = (int) Math.ceil(recipes.size() / 7.0);

        // Bắt lấy cái Inventory hiện tại đang mở thay vì tạo mới
        Inventory inv = event.getInventory();

        if (slot == PREV_PAGE_SLOT && session.page > 0) {
            session.page--;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            renderGUI(player, inv);
        }
        else if (slot == NEXT_PAGE_SLOT && session.page < totalPages - 1) {
            session.page++;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            renderGUI(player, inv);
        }
        else if (isRecipeSlot(slot)) {
            int indexInPage = getRecipeIndexFromSlot(slot);
            int absoluteIndex = (session.page * 7) + indexInPage;

            if (absoluteIndex < recipes.size()) {
                session.selectedRecipe = absoluteIndex;
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                renderGUI(player, inv);
            }
        }
        // ==========================================
        // [ĐÃ TỐI ƯU THẬT SỰ]: XÓA SẠCH SPAM LOG, CHỈ BÁO 1 DÒNG
        // ==========================================
        else if (slot == CONFIRM_SLOT) {
            if (session.selectedRecipe == -1) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§c§l[!] §cVui lòng chọn một công thức ở thanh bên dưới trước khi chế tác!");
                return;
            }

            FixedRecipe recipe = recipes.get(session.selectedRecipe);
            boolean canCraft = true;
            List<String> missingList = new ArrayList<>();

            for (ItemStack req : recipe.ingredients) {
                int currentAmount = RPGCore.getInstance().getItemManager().countItem(player, req);
                if (currentAmount < req.getAmount()) {
                    canCraft = false;
                    String itemName = req.hasItemMeta() && req.getItemMeta().hasDisplayName() ? req.getItemMeta().getDisplayName() : req.getType().name();
                    missingList.add(itemName + " §c(" + currentAmount + "/" + req.getAmount() + ")");
                }
            }

            if (canCraft) {
                consumeItems(player, recipe.ingredients);
                player.getInventory().addItem(recipe.result.clone());

                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage("§6§l[Chế Tác] §aChế tạo thành công " + recipe.result.getItemMeta().getDisplayName() + "§a!");
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§c§l[!] §cChế tác thất bại! Bạn thiếu: §e" + String.join("§c, §e", missingList));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME)) {
            sessions.remove(event.getPlayer().getUniqueId());
        }
    }

    private void consumeItems(Player player, List<ItemStack> requirements) {
        for (ItemStack req : requirements) {
            RPGCore.getInstance().getItemManager().takeItem(player, req, req.getAmount());
        }
    }

    private boolean isRecipeSlot(int slot) {
        for (int s : RECIPE_SLOTS) if (s == slot) return true;
        return false;
    }

    private int getRecipeIndexFromSlot(int slot) {
        for (int i = 0; i < RECIPE_SLOTS.length; i++) {
            if (RECIPE_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static class PlayerSession {
        int page = 0;
        int selectedRecipe = -1;
    }
}