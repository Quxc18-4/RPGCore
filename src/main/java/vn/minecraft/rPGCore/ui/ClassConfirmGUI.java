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

    public static final String GUI_TITLE = "§4§lXÁC NHẬN ĐỔI CHỨC NGHIỆP";
    private static final int CONFIRM_SLOT = 11;
    private static final int INFO_SLOT = 13;
    private static final int CANCEL_SLOT = 15;

    private final RPGCore plugin;
    private final Inventory inventory;
    private final RPGClass targetClass;
    private final boolean usingScroll;

    public ClassConfirmGUI(RPGCore plugin) {
        this.plugin = plugin;
        this.inventory = null;
        this.targetClass = null;
        this.usingScroll = false;
    }

    private ClassConfirmGUI(RPGCore plugin, RPGPlayer rpgPlayer, RPGClass targetClass, boolean usingScroll) {
        this.plugin = plugin;
        this.targetClass = targetClass;
        this.usingScroll = usingScroll;
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

    public boolean isUsingScroll() {
        return usingScroll;
    }

    public static void open(Player player, RPGCore plugin, RPGPlayer rpgPlayer, RPGClass targetClass, boolean hasScroll) {
        if (plugin == null || rpgPlayer == null || targetClass == null) return;
        ClassConfirmGUI gui = new ClassConfirmGUI(plugin, rpgPlayer, targetClass, hasScroll);
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

        if (usingScroll) {
            // TRẠNG THÁI 1: CÓ CUỘN GIẤY CHUYỂN HỆ PHÁI (BẢO TOÀN LEVEL & ATTR)
            ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
            ItemMeta infoMeta = infoItem.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setDisplayName("§d§lCHUYỂN ĐỔI CHỨC NGHIỆP");
                List<String> lore = new ArrayList<>();
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§7Thay đổi: " + oldClassName + " §f➔ " + newClassName);
                lore.add("");
                lore.add("§aGiữ nguyên Cấp độ: §bLv." + level + " §7(Không mất EXP)");
                lore.add("§aGiữ nguyên Điểm Tiềm Năng: §eBảo toàn toàn bộ chỉ số");
                lore.add("§eLàm mới Kỹ năng: §fXóa Skill cũ, hoàn trả §b" + level + " SP");
                lore.add("§dChi phí: §fTiêu hao §51x Cuộn Giấy Chuyển Chức Nghiệp");
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                infoMeta.setLore(lore);
                infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                infoItem.setItemMeta(infoMeta);
            }
            inventory.setItem(INFO_SLOT, infoItem);

            inventory.setItem(CONFIRM_SLOT, createItem(Material.LIME_WOOL,
                    "§a§lXÁC NHẬN ĐỔI CHỨC NGHIỆP",
                    "§7Chuyển sang " + newClassName + " §7và giữ nguyên tiến độ.",
                    "§5(Tiêu hao 1x Cuộn Giấy Chuyển Chức Nghiệp)"));
        } else {
            // TRẠNG THÁI 2: KHÔNG CÓ CUỘN GIẤY (RESET LEVEL VỀ 1 & XÓA SẠCH ATTR)
            ItemStack infoItem = new ItemStack(Material.WITHER_SKELETON_SKULL);
            ItemMeta infoMeta = infoItem.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setDisplayName("§c§lCẢNH BÁO: TÁI LẬP NHÂN VẬT");
                List<String> lore = new ArrayList<>();
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§7Thay đổi: " + oldClassName + " §f➔ " + newClassName);
                lore.add("§cKhông tìm thấy §5[Cuộn Giấy Chuyển Chức Nghiệp]§c!");
                lore.add("");
                lore.add("§cĐặt lại Cấp độ: §bLv." + level + " §c➔ §eLv.1 §7(Mất toàn bộ EXP)");
                lore.add("§cXóa Điểm Tiềm Năng: §f5 chỉ số & điểm dư đặt về §e0");
                lore.add("§cXóa Kỹ năng: §fToàn bộ Skill cũ bị xóa, SP về §e1");
                lore.add("");
                lore.add("§7Mẹo: Dùng §5Cuộn Giấy Chuyển Chức Nghiệp §7để giữ lại Lv & Chỉ số.");
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                infoMeta.setLore(lore);
                infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                infoItem.setItemMeta(infoMeta);
            }
            inventory.setItem(INFO_SLOT, infoItem);

            inventory.setItem(CONFIRM_SLOT, createItem(Material.ORANGE_WOOL,
                    "§6§lCHẤP NHẬN RESET & ĐỔI CHỨC NGHIỆP",
                    "§cHành động này không thể hoàn tác!",
                    "§7Nhân vật sẽ trở về §eLv.1 §7và mất toàn bộ điểm chỉ số."));
        }

        inventory.setItem(CANCEL_SLOT, createItem(Material.RED_WOOL,
                "§c§lHỦY BỎ",
                "§7Quay lại bảng chọn Chức Nghiệp",
                "§7(Giữ nguyên trạng thái hiện tại)"));
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
            player.sendMessage("§cBạn không thể đổi Chức Nghiệp khi đang trong trạng thái giao tranh!");
            return;
        }

        // 2. Chặn trùng hệ phái
        if (rpgPlayer.getCurrentClass() != null && rpgPlayer.getCurrentClass().getId().equalsIgnoreCase(target.getId())) {
            player.closeInventory();
            player.sendMessage("§eBạn đang thuộc Chức Nghiệp này rồi!");
            return;
        }

        String newClassName = ChatColor.translateAlternateColorCodes('&', target.getDisplayName());

        if (confirmGUI.isUsingScroll()) {
            // NHÁNH 1: ĐỔI BẢO TOÀN BẰNG CUỘN GIẤY
            ItemManager itemManager = plugin.getItemManager();
            ItemStack scrollTemplate = (itemManager != null) ? itemManager.buildItem("class_change_scroll", 1) : null;
            if (scrollTemplate == null || !itemManager.takeItem(player, scrollTemplate, 1)) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage("§cKhông tìm thấy §5[Cuộn Giấy Chuyển Chức Nghiệp] §ctrong túi đồ! Thao tác đã bị hủy để bảo vệ cấp độ của bạn.");
                return;
            }

            rpgPlayer.changeClass(target);
            rpgPlayer.updateMaxStats();
            rpgPlayer.updateEquipmentStats();

            if (plugin.getBossBarManager() != null) {
                plugin.getBossBarManager().updateBars(player, rpgPlayer);
            }
            plugin.getPlayerManager().savePlayer(player.getUniqueId(), true, false);

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage("§a§l[THÀNH CÔNG] §fBạn đã chuyển đổi sang Chức Nghiệp " + newClassName
                    + " §7(Giữ nguyên §bLv." + rpgPlayer.getLevel() + " §7& Điểm Tiềm Năng)!");
        } else {
            // NHÁNH 2: ĐỔI CHẤP NHẬN RESET TOÀN BỘ LEVEL & CHỈ SỐ
            rpgPlayer.setLevel(1);
            rpgPlayer.setCurrentExp(0);
            player.setLevel(1);
            player.setExp(0f);

            String[] stats = {"STR", "INT", "VIT", "AGI", "LUK"};
            for (String stat : stats) {
                rpgPlayer.getAttributeData().setAttribute(stat, 0);
            }
            rpgPlayer.getAttributeData().setUnspentAttrPoints(0);

            // Gọi changeClass sau khi đã đưa level về 1 để SP cũng tự đặt lại về 1
            rpgPlayer.changeClass(target);
            rpgPlayer.updateMaxStats();
            rpgPlayer.updateEquipmentStats();

            if (plugin.getBossBarManager() != null) {
                plugin.getBossBarManager().updateBars(player, rpgPlayer);
            }
            plugin.getPlayerManager().savePlayer(player.getUniqueId(), true, false);

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.2f);
            player.sendMessage("§e§l[TÁI SINH] §fBạn đã chuyển sang Chức Nghiệp " + newClassName
                    + " §7(Cấp độ đặt lại về §eLv.1 §7và toàn bộ chỉ số về §e0§7).");
        }
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