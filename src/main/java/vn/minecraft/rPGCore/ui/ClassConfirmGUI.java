package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassConfirmGUI implements InventoryHolder, Listener {

    public static final String GUI_TITLE = "§4§lXÁC NHẬN ĐỔI HỆ PHÁI";
    private static final int CONFIRM_SLOT = 11;
    private static final int INFO_SLOT = 13;
    private static final int CANCEL_SLOT = 15;

    private final RPGCore plugin;
    private final Inventory inventory;
    private final RPGClass targetClass;

    // Constructor dùng để đăng ký Listener trong RPGCore.onEnable()
    public ClassConfirmGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.inventory = null;
        this.targetClass = null;
    }

    private ClassConfirmGUI(RPGCore plugin, RPGPlayer rpgPlayer, RPGClass targetClass) {
        this.plugin = plugin;
        this.targetClass = targetClass;
        this.inventory = Bukkit.createInventory(this, 27, GUI_TITLE);
        render(rpgPlayer);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public RPGClass getTargetClass() {
        return targetClass;
    }

    public static void open(Player player, RPGCore plugin, RPGPlayer rpgPlayer, RPGClass targetClass) {
        if (plugin == null || rpgPlayer == null || targetClass == null) return;
        ClassConfirmGUI gui = new ClassConfirmGUI(plugin, rpgPlayer, targetClass);
        player.openInventory(gui.getInventory());
    }

    private void render(RPGPlayer rpgPlayer) {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        String oldClassName = (rpgPlayer.getCurrentClass() != null)
                ? ChatColor.translateAlternateColorCodes('&', rpgPlayer.getCurrentClass().getDisplayName())
                : "§fCommoner";
        String newClassName = ChatColor.translateAlternateColorCodes('&', targetClass.getDisplayName());
        int level = rpgPlayer.getLevel();

        // Ô 13: Thông tin cảnh báo
        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lTHÔNG TIN CHUYỂN ĐỔI HỆ PHÁI");
            List<String> lore = new ArrayList<>();
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("§7Thay đổi: " + oldClassName + " §f➔ " + newClassName);
            lore.add("");
            lore.add("§aGiữ nguyên Cấp độ: §bLv." + level + " §7(Không mất EXP)");
            lore.add("§aGiữ nguyên Điểm Tiềm Năng: §eToàn bộ chỉ số được bảo toàn");
            lore.add("§eLàm mới Kỹ năng: §fXóa toàn bộ Skill & Combo cũ,");
            lore.add("§f   hoàn trả §b" + level + " điểm SP §fđể học kỹ năng mới.");
            lore.add("§d★ Chi phí: §fTiêu hao §51x Cuộn Giấy Chuyển Hệ Phái");
            lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            infoMeta.setLore(lore);
            infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(INFO_SLOT, infoItem);

        // Ô 11: Xác nhận
        inventory.setItem(CONFIRM_SLOT, createItem(Material.LIME_WOOL,
                "§a§lXÁC NHẬN ĐỔI PHÁI",
                "§7Nhấp để hoàn tất chuyển đổi sang " + newClassName,
                "§c(Sẽ tiêu hao 1x Cuộn Giấy Chuyển Hệ Phái)"));

        // Ô 15: Hủy bỏ
        inventory.setItem(CANCEL_SLOT, createItem(Material.RED_WOOL,
                "§c§lHỦY BỎ",
                "§7Quay lại bảng chọn Hệ Phái",
                "§7(Không tiêu hao vật phẩm)"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ClassConfirmGUI confirmGUI)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof ClassConfirmGUI)) return;

        int rawSlot = event.getRawSlot();

        if (rawSlot == CANCEL_SLOT) {
            player.closeInventory();
            if (plugin.getClassManager() != null) {
                new ClassGUI().openGUI(player, new ArrayList<>(plugin.getClassManager().getClasses().values()));
            }
            return;
        }

        if (rawSlot != CONFIRM_SLOT) return;

        RPGClass target = confirmGUI.getTargetClass();
        if (target == null) {
            player.closeInventory();
            return;
        }

        RPGPlayer rpgPlayer = plugin.getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) {
            player.closeInventory();
            return;
        }

        // 1. Chặn đổi nếu đang trong giao tranh
        if (rpgPlayer.isInCombat()) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cBạn không thể đổi hệ phái khi đang trong trạng thái giao tranh!");
            return;
        }

        // 2. Chặn trùng hệ phái
        if (rpgPlayer.getCurrentClass() != null && rpgPlayer.getCurrentClass().getId().equalsIgnoreCase(target.getId())) {
            player.closeInventory();
            player.sendMessage("§eBạn đang thuộc hệ phái này rồi!");
            return;
        }

        // 3. Kiểm tra và tiêu hao 1x class_change_scroll ngay tại thời điểm bấm xác nhận (chống dupe)
        ItemManager itemManager = plugin.getItemManager();
        ItemStack scrollTemplate = (itemManager != null) ? itemManager.buildItem("class_change_scroll", 1) : null;
        if (scrollTemplate == null || !itemManager.takeItem(player, scrollTemplate, 1)) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cBạn không còn §5[Cuộn Giấy Chuyển Hệ Phái] §ctrong túi đồ!");
            return;
        }

        // 4. Thực hiện đổi Class (giữ nguyên Level & Attribute, làm mới Skill/SP, roll nguyên tố nếu là Mage)
        rpgPlayer.changeClass(target);
        rpgPlayer.updateMaxStats();
        rpgPlayer.updateEquipmentStats();

        if (plugin.getBossBarManager() != null) {
            plugin.getBossBarManager().updateBars(player, rpgPlayer);
        }

        // 5. Lưu ngay xuống MongoDB
        plugin.getPlayerManager().savePlayer(player.getUniqueId(), true, false);

        String newClassName = ChatColor.translateAlternateColorCodes('&', target.getDisplayName());
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage("§a§l[THÀNH CÔNG] §fBạn đã chuyển đổi sang hệ phái " + newClassName
                + " §7(Giữ nguyên §bLv." + rpgPlayer.getLevel() + " §7& Điểm Tiềm Năng)!");
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}