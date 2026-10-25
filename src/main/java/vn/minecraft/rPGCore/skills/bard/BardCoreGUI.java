package vn.minecraft.rPGCore.skills.bard;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BardCoreGUI implements Listener {

    public static final String GUI_TITLE = "§8§lĐỘT PHÁ: NÂNG CẤP GIAI ĐIỆU";
    public static final String ADMIN_GUI_TITLE = "§c§lADMIN: ÉP LÕI (FORCE ADD)";
    public static final String PREVIEW_GUI_TITLE = "§8§lXEM TRƯỚC: NÂNG CẤP";

    private static final CoreData[] CORES = {
            new CoreData(1, "Dư Âm Năng Lượng", "Khi Giai Điệu nổ, lập tức hồi cố định 50 Mana cho mọi đồng minh."),
            new CoreData(2, "Hơi Thở Của Gió", "Nếu HP > 90% khi kích nổ Giai điệu: Kỹ năng đó tốn 0 Mana và giảm thêm 20% Hồi chiêu cuối."),
            new CoreData(3, "Vang Vọng Tột Cùng", "Tăng vĩnh viễn mức mở rộng Bán kính của Âm Vang từ 50% lên 80% mỗi Âm."),
            new CoreData(4, "Hợp Âm Dội Lại", "Khi Giai điệu nổ, tự động hoàn trả 1 Âm ngẫu nhiên vừa bị tiêu thụ vào bộ nhớ."),
            new CoreData(5, "Bội Âm", "Mỗi Âm đang giữ +20% Tốc chạy & +15% Kháng Hiệu Ứng. (Mất đi khi tiêu thụ)."),
            new CoreData(6, "Nghịch Âm", "Kẻ địch dính Giai điệu bị giảm 15% Kháng Phép và Giáp Vật lý."),
            new CoreData(7, "Dấu Lặng Ngân", "Giai điệu Sự Sống thanh tẩy 1 Hiệu ứng xấu ngẫu nhiên cho đồng minh."),
            new CoreData(8, "Thiên Tài Âm Nhạc", "Tăng giới hạn lưu trữ từ 3 Âm lên tối đa 4 Âm."),
            new CoreData(9, "Giai Điệu Vĩnh Cửu", "Giai điệu trúng từ 4 đồng minh trở lên lập tức giảm 50% Hồi chiêu của kỹ năng đó.")
    };

    private static ItemStack getFillerGlass() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack getStatusGlass(boolean isLearned) {
        Material mat = isLearned ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(isLearned ? "§a§l[ĐÃ KÍCH HOẠT]" : "§c§l[CHƯA KÍCH HOẠT]");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    public static void sendPreviewPrompt(Player player) {
        player.sendMessage("§c§l[!] §cBạn chưa có Điểm Nâng Cấp nào. Hãy di chuyển thêm!");

        TextComponent base = new TextComponent("§fBạn có muốn xem trước các Nâng Cấp của chức nghiệp không? ");
        TextComponent yesBtn = new TextComponent("§a§l[XEM TRƯỚC]");
        yesBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bardpreview"));
        yesBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Click để mở Bảng Nâng Cấp").create()));

        // Đã xóa nút [KHÔNG]
        base.addExtra(yesBtn);

        player.spigot().sendMessage(base);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    @EventHandler
    public void onHiddenCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (msg.equals("/bardpreview")) {
            event.setCancelled(true);
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(event.getPlayer().getUniqueId());
            if (rpgPlayer != null) openPreviewMenu(event.getPlayer(), rpgPlayer);
        }
    }

    // ==========================================
    // 1. GUI XEM TRƯỚC (27 Ô - Status Glass)
    // ==========================================
    public static void openPreviewMenu(Player player, RPGPlayer rpgPlayer) {
        Inventory inv = Bukkit.createInventory(null, 27, PREVIEW_GUI_TITLE);

        for (int i = 0; i < CORES.length; i++) {
            CoreData core = CORES[i];
            boolean isLearned = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_" + core.id) > 0;

            // Kính trạng thái ở Hàng 1 (0-8) và Hàng 3 (18-26)
            ItemStack statusGlass = getStatusGlass(isLearned);
            inv.setItem(i, statusGlass);
            inv.setItem(i + 18, statusGlass);

            // Item Lõi ở Hàng 2 (9-17)
            ItemStack item = new ItemStack(isLearned ? Material.ECHO_SHARD : Material.AMETHYST_SHARD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d§l[Lõi Cấp " + core.id + "] §f" + core.name);
                meta.setLore(Arrays.asList("§7" + core.description, "", isLearned ? "§a§l[ĐÃ HỌC]" : "§c§l[CHƯA HỌC]", "§8(Bảng này chỉ dùng để xem trước)"));
                item.setItemMeta(meta);
            }
            inv.setItem(i + 9, item);
        }

        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        player.openInventory(inv);
    }

    public static void openSelectionMenu(Player player, RPGPlayer rpgPlayer) {
        boolean isAdmin = player.hasPermission("rpgcore.admin");
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");

        if (unspent <= 0 && !isAdmin) {
            sendPreviewPrompt(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        ItemStack glass = getFillerGlass();
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        List<CoreData> selectedCores = new ArrayList<>();
        if (player.hasMetadata("BARD_ROLLED_CORES")) {
            String savedRolls = player.getMetadata("BARD_ROLLED_CORES").get(0).asString();
            String[] ids = savedRolls.split(",");
            for (String idStr : ids) {
                try {
                    int id = Integer.parseInt(idStr);
                    for (CoreData core : CORES) {
                        if (core.id == id) { selectedCores.add(core); break; }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (selectedCores.isEmpty()) {
            List<CoreData> availableCores = new ArrayList<>();
            for (CoreData core : CORES) {
                if (rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_" + core.id) == 0) {
                    availableCores.add(core);
                }
            }

            if (availableCores.isEmpty()) {
                if (!isAdmin) {
                    player.sendMessage("§aBạn đã mở khóa toàn bộ Nâng Cấp của Nhạc Sư!");
                    return;
                }
            } else {
                Collections.shuffle(availableCores);
                selectedCores = availableCores.subList(0, Math.min(3, availableCores.size()));
                StringBuilder savedMeta = new StringBuilder();
                for (CoreData core : selectedCores) {
                    savedMeta.append(core.id).append(",");
                }
                player.setMetadata("BARD_ROLLED_CORES", new FixedMetadataValue(RPGCore.getInstance(), savedMeta.toString()));
            }
        }

        int[] slots = {11, 13, 15};
        for (int i = 0; i < selectedCores.size(); i++) {
            CoreData core = selectedCores.get(i);
            ItemStack item = new ItemStack(Material.AMETHYST_CLUSTER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d§l[Nâng Cấp] §e" + core.name);
                meta.setLore(Arrays.asList("§7" + core.description, "", "§a► Nhấp chuột để Thức Tỉnh Nâng Cấp này!"));
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        if (isAdmin) {
            ItemStack adminTool = new ItemStack(Material.NETHER_STAR);
            ItemMeta adminMeta = adminTool.getItemMeta();
            if (adminMeta != null) {
                adminMeta.setDisplayName("§c§l[QUẢN TRỊ VIÊN] §fCông Cụ Lõi");
                adminMeta.setLore(Arrays.asList(
                        "§7Công cụ gỡ lỗi nhanh cho Admin.",
                        "",
                        "§eChuột Trái: §f+1 Điểm Đột Phá",
                        "§eShift + Trái: §fMở Bảng Ép Lõi",
                        "§eChuột Phải: §fReset TOÀN BỘ (Lõi, Quãng Đường)",
                        "§eShift + Phải: §fCộng Bước Chân (Nhập Số)"
                ));
                adminTool.setItemMeta(adminMeta);
            }
            inv.setItem(26, adminTool);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
        player.openInventory(inv);
    }

    public static void openAdminForceAddMenu(Player player, RPGPlayer rpgPlayer) {
        Inventory inv = Bukkit.createInventory(null, 27, ADMIN_GUI_TITLE);
        ItemStack glass = getFillerGlass();
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        for (int i = 0; i < CORES.length; i++) {
            CoreData core = CORES[i];
            boolean isLearned = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_" + core.id) > 0;
            ItemStack item = new ItemStack(isLearned ? Material.ECHO_SHARD : Material.AMETHYST_SHARD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d§l[Lõi Cấp " + core.id + "] §f" + core.name);
                meta.setLore(Arrays.asList("§7" + core.description, "", isLearned ? "§a§l[ĐÃ HỌC]" : "§c§l[CHƯA HỌC]", "§eClick để Bật/Tắt Lõi này ngay lập tức!"));
                item.setItemMeta(meta);
            }
            inv.setItem(9 + i, item);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.5f);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(PREVIEW_GUI_TITLE)) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(GUI_TITLE)) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            if (rpgPlayer == null) return;

            if (clickedItem.getType() == Material.NETHER_STAR && player.hasPermission("rpgcore.admin")) {
                ClickType clickType = event.getClick();

                if (clickType == ClickType.LEFT) {
                    int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
                    rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", unspent + 1);
                    player.sendMessage("§c§l[ADMIN] §fĐã cộng 1 điểm Đột Phá! (Tổng: " + (unspent + 1) + ")");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
                else if (clickType == ClickType.SHIFT_LEFT) {
                    openAdminForceAddMenu(player, rpgPlayer);
                }
                else if (clickType == ClickType.RIGHT) {
                    for (int i = 1; i <= 9; i++) {
                        rpgPlayer.getSkillData().setSkillLevel("BARD_CORE_" + i, 0);
                        rpgPlayer.getSkillData().setSkillLevel("BARD_MILESTONE_" + i, 0);
                    }
                    rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", 0);
                    rpgPlayer.getSkillData().setSkillLevel("BARD_DISTANCE", 0);

                    player.removeMetadata("BARD_ROLLED_CORES", RPGCore.getInstance());
                    player.sendMessage("§c§l[ADMIN] §fĐã Reset sạch sẽ Lõi, Cache và Quãng đường (Đưa về 0)!");
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                    player.closeInventory();
                }
                else if (clickType == ClickType.SHIFT_RIGHT) {
                    player.closeInventory();
                    player.setMetadata("BARD_AWAITING_DISTANCE_INPUT", new FixedMetadataValue(RPGCore.getInstance(), true));
                    player.sendMessage("§c§l[ADMIN] §fVui lòng nhập số Block (Quãng đường) muốn cộng thêm vào kênh Chat.");
                    player.sendMessage("§7(Gõ §c'cancel' §7để hủy thao tác)");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
                return;
            }

            if (clickedItem.getType() == Material.AMETHYST_CLUSTER && clickedItem.hasItemMeta()) {
                String name = clickedItem.getItemMeta().getDisplayName();
                int coreId = -1;
                for (CoreData core : CORES) {
                    if (name.contains(core.name)) { coreId = core.id; break; }
                }

                if (coreId != -1) {
                    int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
                    if (unspent > 0) {
                        rpgPlayer.getSkillData().setSkillLevel("BARD_UNSPENT_CORES", unspent - 1);
                        rpgPlayer.getSkillData().setSkillLevel("BARD_CORE_" + coreId, 1);
                        player.removeMetadata("BARD_ROLLED_CORES", RPGCore.getInstance());

                        player.sendMessage("§a§lĐỘT PHÁ THÀNH CÔNG! §fBạn đã thức tỉnh Nâng Cấp: §d" + name);
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
                        player.closeInventory();

                        if (unspent - 1 > 0) {
                            player.sendMessage("§eBạn vẫn còn §d" + (unspent - 1) + " Điểm Nâng Cấp§e! (Bấm [F] để tiếp tục)");
                        }
                    }
                }
            }
        }
        else if (title.equals(ADMIN_GUI_TITLE)) {
            event.setCancelled(true);
            if (!player.hasPermission("rpgcore.admin")) return;
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String name = clickedItem.getItemMeta().getDisplayName();
            int coreId = -1;
            for (CoreData core : CORES) {
                if (name.contains(core.name)) { coreId = core.id; break; }
            }

            if (coreId != -1) {
                RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
                boolean isLearned = rpgPlayer.getSkillData().getSkillLevel("BARD_CORE_" + coreId) > 0;
                rpgPlayer.getSkillData().setSkillLevel("BARD_CORE_" + coreId, isLearned ? 0 : 1);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_CLERIC, 1.0f, 1.5f);
                openAdminForceAddMenu(player, rpgPlayer);
            }
        }
    }

    @EventHandler
    public void onAdminChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("BARD_AWAITING_DISTANCE_INPUT")) {
            event.setCancelled(true);
            String msg = event.getMessage().trim();

            if (msg.equalsIgnoreCase("cancel")) {
                player.removeMetadata("BARD_AWAITING_DISTANCE_INPUT", RPGCore.getInstance());
                player.sendMessage("§c§l[ADMIN] §fĐã hủy thao tác cộng bước chân.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                return;
            }

            try {
                int amount = Integer.parseInt(msg);
                if (amount <= 0) {
                    player.sendMessage("§cSố lượng phải lớn hơn 0! Vui lòng nhập lại hoặc gõ 'cancel'.");
                    return;
                }

                Bukkit.getScheduler().runTask(RPGCore.getInstance(), () -> {
                    RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
                    if (rpgPlayer != null) {
                        int currentDist = rpgPlayer.getSkillData().getSkillLevel("BARD_DISTANCE");
                        rpgPlayer.getSkillData().setSkillLevel("BARD_DISTANCE", currentDist + amount);

                        player.removeMetadata("BARD_AWAITING_DISTANCE_INPUT", RPGCore.getInstance());
                        player.sendMessage("§c§l[ADMIN] §fĐã cộng §e" + String.format("%,d", amount) + " Bước§f! (Tổng: " + String.format("%,d", currentDist + amount) + ")");
                        player.sendMessage("§7§oHãy di chuyển 1 bước để hệ thống kiểm tra và trao mốc (nếu có).");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }
                });

            } catch (NumberFormatException e) {
                player.sendMessage("§cSai định dạng! Vui lòng chỉ nhập SỐ (VD: 50000) hoặc gõ 'cancel'.");
            }
        }
    }

    private static class CoreData {
        int id; String name; String description;
        CoreData(int id, String name, String description) { this.id = id; this.name = name; this.description = description; }
    }
}