package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.*;

public class BuilderShopGUI implements Listener, CommandExecutor {

    public static final String MAIN_MENU = "§8§lĐẠI SIÊU THỊ VẬT LIỆU";
    public static final String CAT_MENU_PREFIX = "§8§lVật liệu: ";

    private final long PRICE_PER_STACK = 100L;

    private final Map<String, List<Material>> categories = new LinkedHashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> playerCategories = new HashMap<>();

    // Dải màu chuẩn của Minecraft
    private final String[] COLORS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    // Các loại gỗ chuẩn
    private final String[] WOODS = {
            "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK",
            "MANGROVE", "CHERRY", "BAMBOO", "CRIMSON", "WARPED"
    };

    public BuilderShopGUI() {
        categories.put("Gỗ Tự Nhiên", new ArrayList<>());
        categories.put("Sản Phẩm Từ Gỗ", new ArrayList<>());
        categories.put("Đá Khối & Gạch", new ArrayList<>());
        categories.put("Sản Phẩm Từ Đá", new ArrayList<>());
        categories.put("Bê Tông", new ArrayList<>());
        categories.put("Đất Nung", new ArrayList<>());
        categories.put("Len & Trang Trí", new ArrayList<>());
        categories.put("Kính Màu", new ArrayList<>());
        categories.put("Khối Đồng", new ArrayList<>());
        categories.put("Ánh Sáng & Xích", new ArrayList<>());

        loadCuratedCategories();
    }

    // ==========================================
    // HÀM LẤY MATERIAL AN TOÀN
    // ==========================================
    private void addSafe(List<Material> list, String matName) {
        Material mat = Material.getMaterial(matName);
        if (mat != null && mat.isItem()) {
            list.add(mat);
        }
    }

    // ==========================================
    // THUẬT TOÁN ĐIỀN DANH MỤC THÔNG MINH
    // ==========================================
    private void loadCuratedCategories() {
        List<Material> rawWood = categories.get("Gỗ Tự Nhiên");
        List<Material> woodProd = categories.get("Sản Phẩm Từ Gỗ");
        List<Material> stoneBase = categories.get("Đá Khối & Gạch");
        List<Material> stoneProd = categories.get("Sản Phẩm Từ Đá");
        List<Material> concrete = categories.get("Bê Tông");
        List<Material> terracotta = categories.get("Đất Nung");
        List<Material> woolDecor = categories.get("Len & Trang Trí");
        List<Material> glass = categories.get("Kính Màu");
        List<Material> copper = categories.get("Khối Đồng");
        List<Material> lights = categories.get("Ánh Sáng & Xích");

        // 1 & 2. GỖ TỰ NHIÊN VÀ SẢN PHẨM GỖ
        for (String w : WOODS) {
            if (w.equals("BAMBOO")) {
                addSafe(rawWood, "BAMBOO_BLOCK"); addSafe(rawWood, "STRIPPED_BAMBOO_BLOCK");
                addSafe(woodProd, "BAMBOO_PLANKS"); addSafe(woodProd, "BAMBOO_MOSAIC");
                addSafe(woodProd, "BAMBOO_SLAB"); addSafe(woodProd, "BAMBOO_MOSAIC_SLAB");
                addSafe(woodProd, "BAMBOO_STAIRS"); addSafe(woodProd, "BAMBOO_MOSAIC_STAIRS");
                addSafe(woodProd, "BAMBOO_FENCE"); addSafe(woodProd, "BAMBOO_FENCE_GATE");
                addSafe(woodProd, "BAMBOO_DOOR"); addSafe(woodProd, "BAMBOO_TRAPDOOR");
                addSafe(woodProd, "BAMBOO_PRESSURE_PLATE"); addSafe(woodProd, "BAMBOO_BUTTON");
                addSafe(woodProd, "BAMBOO_SIGN"); addSafe(woodProd, "BAMBOO_HANGING_SIGN");
            } else if (w.equals("CRIMSON") || w.equals("WARPED")) {
                addSafe(rawWood, w + "_STEM"); addSafe(rawWood, w + "_HYPHAE");
                addSafe(rawWood, "STRIPPED_" + w + "_STEM"); addSafe(rawWood, "STRIPPED_" + w + "_HYPHAE");
                addSafe(woodProd, w + "_PLANKS"); addSafe(woodProd, w + "_SLAB");
                addSafe(woodProd, w + "_STAIRS"); addSafe(woodProd, w + "_FENCE");
                addSafe(woodProd, w + "_FENCE_GATE"); addSafe(woodProd, w + "_DOOR");
                addSafe(woodProd, w + "_TRAPDOOR"); addSafe(woodProd, w + "_PRESSURE_PLATE");
                addSafe(woodProd, w + "_BUTTON"); addSafe(woodProd, w + "_SIGN");
                addSafe(woodProd, w + "_HANGING_SIGN");
            } else {
                addSafe(rawWood, w + "_LOG"); addSafe(rawWood, w + "_WOOD");
                addSafe(rawWood, "STRIPPED_" + w + "_LOG"); addSafe(rawWood, "STRIPPED_" + w + "_WOOD");
                addSafe(woodProd, w + "_PLANKS"); addSafe(woodProd, w + "_SLAB");
                addSafe(woodProd, w + "_STAIRS"); addSafe(woodProd, w + "_FENCE");
                addSafe(woodProd, w + "_FENCE_GATE"); addSafe(woodProd, w + "_DOOR");
                addSafe(woodProd, w + "_TRAPDOOR"); addSafe(woodProd, w + "_PRESSURE_PLATE");
                addSafe(woodProd, w + "_BUTTON"); addSafe(woodProd, w + "_SIGN");
                addSafe(woodProd, w + "_HANGING_SIGN");
            }
        }

        // 3 & 4. ĐÁ KHỐI VÀ SẢN PHẨM TỪ ĐÁ
        String[] standardStones = {
                "STONE", "COBBLESTONE", "SMOOTH_STONE", "MOSSY_COBBLESTONE", "STONE_BRICKS",
                "MOSSY_STONE_BRICKS", "CHISELED_STONE_BRICKS", "GRANITE", "POLISHED_GRANITE",
                "DIORITE", "POLISHED_DIORITE", "ANDESITE", "POLISHED_ANDESITE", "DEEPSLATE",
                "COBBLED_DEEPSLATE", "POLISHED_DEEPSLATE", "DEEPSLATE_BRICKS", "DEEPSLATE_TILES",
                "CHISELED_DEEPSLATE", "TUFF", "POLISHED_TUFF", "TUFF_BRICKS", "CHISELED_TUFF",
                "MUD_BRICKS", "SANDSTONE", "SMOOTH_SANDSTONE", "RED_SANDSTONE", "SMOOTH_RED_SANDSTONE",
                "PRISMARINE", "DARK_PRISMARINE", "NETHER_BRICKS", "RED_NETHER_BRICKS", "BLACKSTONE",
                "POLISHED_BLACKSTONE", "POLISHED_BLACKSTONE_BRICKS", "END_STONE_BRICKS"
        };
        for (String base : standardStones) {
            addSafe(stoneBase, base);
            addSafe(stoneProd, base + "_STAIRS");
            addSafe(stoneProd, base + "_SLAB");
            addSafe(stoneProd, base + "_WALL");
        }
        // Đá có cấu trúc tên đặc biệt
        addSafe(stoneBase, "BRICKS"); addSafe(stoneProd, "BRICK_STAIRS"); addSafe(stoneProd, "BRICK_SLAB"); addSafe(stoneProd, "BRICK_WALL");
        addSafe(stoneBase, "QUARTZ_BLOCK"); addSafe(stoneBase, "QUARTZ_BRICKS"); addSafe(stoneBase, "SMOOTH_QUARTZ");
        addSafe(stoneProd, "QUARTZ_STAIRS"); addSafe(stoneProd, "QUARTZ_SLAB"); addSafe(stoneProd, "SMOOTH_QUARTZ_STAIRS"); addSafe(stoneProd, "SMOOTH_QUARTZ_SLAB");
        addSafe(stoneBase, "PURPUR_BLOCK"); addSafe(stoneBase, "PURPUR_PILLAR"); addSafe(stoneProd, "PURPUR_STAIRS"); addSafe(stoneProd, "PURPUR_SLAB");
        addSafe(stoneBase, "PRISMARINE_BRICKS"); addSafe(stoneProd, "PRISMARINE_BRICK_STAIRS"); addSafe(stoneProd, "PRISMARINE_BRICK_SLAB");

        // 5. BÊ TÔNG
        for (String c : COLORS) {
            addSafe(concrete, c + "_CONCRETE");
            addSafe(concrete, c + "_CONCRETE_POWDER");
        }

        // 6. ĐẤT NUNG
        addSafe(terracotta, "TERRACOTTA");
        for (String c : COLORS) {
            addSafe(terracotta, c + "_TERRACOTTA");
            addSafe(terracotta, c + "_GLAZED_TERRACOTTA");
        }

        // 7. LEN & TRANG TRÍ
        addSafe(woolDecor, "CANDLE"); // Nến mộc
        for (String c : COLORS) {
            addSafe(woolDecor, c + "_WOOL");
            addSafe(woolDecor, c + "_CARPET");
            addSafe(woolDecor, c + "_BANNER");
            addSafe(woolDecor, c + "_BED");
            addSafe(woolDecor, c + "_CANDLE");
        }

        // 8. KÍNH MÀU
        addSafe(glass, "GLASS"); addSafe(glass, "GLASS_PANE"); addSafe(glass, "TINTED_GLASS");
        for (String c : COLORS) {
            addSafe(glass, c + "_STAINED_GLASS");
            addSafe(glass, c + "_STAINED_GLASS_PANE");
        }

        // 9. KHỐI ĐỒNG
        String[] copperStates = {"", "EXPOSED_", "WEATHERED_", "OXIDIZED_"};
        for (String state : copperStates) {
            addSafe(copper, state + "COPPER");
            addSafe(copper, "CUT_" + state + "COPPER");
            addSafe(copper, "CHISELED_" + state + "COPPER");
            addSafe(copper, state + "COPPER_GRATE");
            addSafe(copper, state + "COPPER_BULB");
            addSafe(copper, state + "COPPER_DOOR");
            addSafe(copper, state + "COPPER_TRAPDOOR");
        }

        // 10. ÁNH SÁNG & XÍCH
        addSafe(lights, "TORCH"); addSafe(lights, "SOUL_TORCH");
        addSafe(lights, "LANTERN"); addSafe(lights, "SOUL_LANTERN");
        addSafe(lights, "CHAIN"); addSafe(lights, "GLOWSTONE");
        addSafe(lights, "SEA_LANTERN"); addSafe(lights, "SHROOMLIGHT");
        addSafe(lights, "OCHRE_FROGLIGHT"); addSafe(lights, "VERDANT_FROGLIGHT"); addSafe(lights, "PEARLESCENT_FROGLIGHT");
        addSafe(lights, "END_ROD");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) openMainMenu((Player) sender);
        return true;
    }

    // ==========================================
    // MENU CHÍNH: 10 DANH MỤC TRƯNG BÀY
    // ==========================================
    public void openMainMenu(Player player) {
        // Nới rộng rương lên 45 ô để sắp xếp 10 danh mục cho thoáng
        Inventory inv = Bukkit.createInventory(null, 45, MAIN_MENU);

        // Hàng trên
        inv.setItem(11, createBtn(Material.OAK_LOG, "§e§lGỗ Tự Nhiên", "§7Gồm " + categories.get("Gỗ Tự Nhiên").size() + " loại Gỗ chưa chế tác."));
        inv.setItem(12, createBtn(Material.OAK_PLANKS, "§6§lSản Phẩm Từ Gỗ", "§7Gồm " + categories.get("Sản Phẩm Từ Gỗ").size() + " loại Ván, Thang, Cửa, Hàng rào..."));
        inv.setItem(13, createBtn(Material.STONE_BRICKS, "§7§lĐá Khối & Gạch", "§7Gồm " + categories.get("Đá Khối & Gạch").size() + " khối đá tự nhiên và gạch cứng."));
        inv.setItem(14, createBtn(Material.COBBLESTONE_WALL, "§8§lSản Phẩm Từ Đá", "§7Gồm " + categories.get("Sản Phẩm Từ Đá").size() + " loại Tường, Phiến và Cầu thang đá."));
        inv.setItem(15, createBtn(Material.COPPER_BLOCK, "§c§lKhối Đồng", "§7Gồm " + categories.get("Khối Đồng").size() + " loại Đồng với các mức độ rỉ sét."));

        // Hàng dưới
        inv.setItem(20, createBtn(Material.TERRACOTTA, "§d§lĐất Nung", "§7Gồm " + categories.get("Đất Nung").size() + " loại Đất nung và Đất nung men."));
        inv.setItem(21, createBtn(Material.CYAN_CONCRETE, "§3§lBê Tông", "§7Gồm " + categories.get("Bê Tông").size() + " loại Bê tông và Bột bê tông."));
        inv.setItem(22, createBtn(Material.RED_WOOL, "§c§lLen & Trang Trí", "§7Gồm " + categories.get("Len & Trang Trí").size() + " loại Len, Thảm, Cờ, Giường và Nến."));
        inv.setItem(23, createBtn(Material.GLASS, "§b§lKính Màu", "§7Gồm " + categories.get("Kính Màu").size() + " loại Kính khối và Kính mỏng (Pane)."));
        inv.setItem(24, createBtn(Material.LANTERN, "§e§lÁnh Sáng & Xích", "§7Gồm " + categories.get("Ánh Sáng & Xích").size() + " nguồn sáng và dây xích trang trí."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    public void openCategoryMenu(Player player, String catName, int page) {
        List<Material> items = categories.get(catName);
        if (items == null || items.isEmpty()) return;

        int totalPages = (int) Math.ceil(items.size() / 45.0);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        playerPages.put(player.getUniqueId(), page);
        playerCategories.put(player.getUniqueId(), catName);

        Inventory inv = Bukkit.createInventory(null, 54, CAT_MENU_PREFIX + catName + " (Trang " + page + ")");

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, createShopItem(items.get(i)));
        }

        if (page > 1) inv.setItem(45, createBtn(Material.ARROW, "§e§l◀ Trang Trước"));
        inv.setItem(49, createBtn(Material.BARRIER, "§c§l◀ Trở về Danh Mục", "§7Quay lại giao diện chính."));
        if (page < totalPages) inv.setItem(53, createBtn(Material.ARROW, "§e§lTrang Sau ▶"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        if (title.equals(MAIN_MENU)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            String catName = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            if (categories.containsKey(catName)) {
                openCategoryMenu(player, catName, 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }
        else if (title.startsWith(CAT_MENU_PREFIX)) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot >= 54) return;

            String currentCat = playerCategories.get(player.getUniqueId());
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

            if (slot == 45 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                openCategoryMenu(player, currentCat, currentPage - 1);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                return;
            }
            if (slot == 53 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                openCategoryMenu(player, currentCat, currentPage + 1);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                return;
            }
            if (slot == 49) {
                openMainMenu(player);
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir() || slot >= 45) return;

            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            if (rpgPlayer == null) return;

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§c§l[!] §cTúi đồ của bạn đã đầy!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (RPGCore.getInstance().getCurrencyManager().canAffordSmart(player, rpgPlayer, PRICE_PER_STACK)) {
                RPGCore.getInstance().getCurrencyManager().processSmartPayment(player, rpgPlayer, PRICE_PER_STACK);
                player.getInventory().addItem(new ItemStack(clicked.getType(), 64));
                player.sendMessage("§a[Cửa Hàng] Bạn đã mua thành công 64 " + clicked.getType().name() + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } else {
                player.sendMessage("§c§l[!] §cBạn không có đủ 1 Bạc để mua khối này!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private ItemStack createBtn(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createShopItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(
                "",
                "§f▶ Số lượng: §e64 khối",
                "§f▶ Đơn giá: §a1 Bạc",
                "",
                "§eClick để mua ngay!"
        ));
        item.setItemMeta(meta);
        return item;
    }
}