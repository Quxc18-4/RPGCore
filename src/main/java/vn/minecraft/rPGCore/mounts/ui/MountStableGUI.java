package vn.minecraft.rPGCore.mounts.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;
import vn.minecraft.rPGCore.mounts.type.MountArchetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MountStableGUI implements Listener {

    private final RPGCore plugin;
    private final Random rand = new Random();

    public static final String MAIN_GUI = "§8Chuồng Ngựa Của Bạn";
    public static final String SHOP_GUI = "§8Cửa Hàng Thú Cưỡi";
    public static final String DETAIL_GUI = "§8Chi Tiết & Nâng Cấp";
    public static final String CONFIRM_GUI = "§8Xác Nhận";
    public static final int[] SHOP_SLOTS = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34};

    // Item ID Giấy Nâng Cấp Thú Cưỡi
    public static final String MOUNT_UPGRADE_ITEM_ID = "misc_mount_upgrade";

    private final java.util.Map<java.util.UUID, Integer> interactingSlot = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, String> confirmAction = new java.util.HashMap<>();

    public MountStableGUI(RPGCore plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // 1. GUI CHUỒNG CHÍNH
    // =========================================================================
    public void openMainStable(Player player) {
        PlayerMountProfile profile = plugin.getMountManager().getProfile(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_GUI);
        fillBackground(inv);

        inv.setItem(11, renderSlot(profile, 1, 0));
        inv.setItem(13, renderSlot(profile, 2, 100)); // 100 Vàng
        inv.setItem(15, renderSlot(profile, 3, 200)); // 200 Vàng

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1f);
    }

    private ItemStack renderSlot(PlayerMountProfile profile, int slotIndex, int costGold) {
        if (profile.getUnlockedSlots() < slotIndex) {
            return createItem(Material.RED_STAINED_GLASS_PANE, "§c§lÔ CHUỒNG SỐ " + slotIndex + " [BỊ KHÓA]",
                    "§7Bạn chưa mở khóa ô chuồng này.", "", "§eChi phí: §l" + costGold + " Vàng", "§a► Click để mua mở rộng!");
        }

        if (!profile.hasMountAt(slotIndex)) {
            return createItem(Material.MINECART, "§a§lÔ CHUỒNG SỐ " + slotIndex + " [TRỐNG]",
                    "§7Ô chuồng này đã sẵn sàng.", "", "§a► Click để tới Cửa Hàng mua Thú cưỡi mới!");
        }

        MountData mount = profile.getMountAt(slotIndex);
        return createItem(Material.SADDLE, "§6§l" + mount.getCustomName(),
                "§8[ Chủng loại: " + mount.getType().getDisplayName() + " ]", "",
                mount.isDead() ? "§c§l[!] Đang Hồi Sức" : "§a§l[!] Sẵn sàng", "",
                "§e► Chuột Trái: §fXem Chi tiết & Nâng cấp",
                "§d► Chuột Phải: §fĐổi Thú Cưỡi (Ghi đè)",
                "§a► Shift + Trái: §fTriệu hồi ngay (/h " + slotIndex + ")");
    }

    // =========================================================================
    // 2. GUI XÁC NHẬN
    // =========================================================================
    public void openConfirmGUI(Player player, String question, String actionCode, int slotIndex) {
        interactingSlot.put(player.getUniqueId(), slotIndex);
        confirmAction.put(player.getUniqueId(), actionCode);

        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_GUI);
        fillBackground(inv);

        inv.setItem(4, createItem(Material.PAPER, "§e§lCÂU HỎI XÁC NHẬN", "§f" + question));
        inv.setItem(11, createItem(Material.LIME_STAINED_GLASS_PANE, "§a§l[ VÂNG, ĐỒNG Ý ]"));
        inv.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l[ KHÔNG, QUAY LẠI ]"));

        player.openInventory(inv);
    }

    // =========================================================================
    // 3. GUI CỬA HÀNG PHÔI THÚ CƯỠI
    // =========================================================================
    public void openShopGUI(Player player, int slotIndex) {
        interactingSlot.put(player.getUniqueId(), slotIndex);
        Inventory inv = Bukkit.createInventory(null, 54, SHOP_GUI + " - Ô " + slotIndex);
        fillBackground(inv);

        List<MountArchetype> buyables = new ArrayList<>();
        for (MountArchetype a : MountArchetype.values()) {
            if (a.isBuyable()) buyables.add(a);
        }

        for (int i = 0; i < buyables.size() && i < SHOP_SLOTS.length; i++) {
            MountArchetype arch = buyables.get(i);
            ItemStack item = createItem(Material.HORSE_SPAWN_EGG, "§6§l" + arch.getDisplayName(),
                    "§7Chỉ số sẽ được quay ngẫu nhiên.", "",
                    "§eGiá bán: §f" + plugin.getCurrencyManager().formatCurrency(arch.getPriceCopper()), "",
                    "§a► Click để mua!");
            inv.setItem(SHOP_SLOTS[i], item);
        }

        inv.setItem(49, createItem(Material.ARROW, "§cTrở về Chuồng Ngựa"));
        player.openInventory(inv);
    }

    // =========================================================================
    // 4. GUI CHI TIẾT & NÂNG CẤP
    // =========================================================================
    public void openDetailGUI(Player player, int slotIndex) {
        interactingSlot.put(player.getUniqueId(), slotIndex);
        PlayerMountProfile profile = plugin.getMountManager().getProfile(player.getUniqueId());
        MountData mount = profile.getMountAt(slotIndex);

        Inventory inv = Bukkit.createInventory(null, 27, DETAIL_GUI + " - Ô " + slotIndex);
        fillBackground(inv);

        ItemStack info = createItem(Material.NAME_TAG, "§e§lThông Tin: §f" + mount.getCustomName(),
                "§f- Tốc độ: §b" + mount.getTierSpeed() + " §8/ " + mount.getType().getMaxSpeed(),
                "§f- Sức nhảy: §a" + mount.getTierJump() + " §8/ " + mount.getType().getMaxJump(),
                "§f- Sức bền (Máu): §c" + mount.getTierDurability() + " §8/ " + mount.getType().getMaxDurability(),
                "§f- Giảm hồi chiêu: §e" + mount.getTierCooldown() + " §8/ " + mount.getType().getMaxCooldown(),
                "§f- Kho chứa đồ: §6" + mount.getTierStorage() + " §8/ " + mount.getType().getMaxStorage());
        inv.setItem(10, info);

        inv.setItem(12, renderUpgradeBtn(player, "Tốc Độ", Material.LIGHT_BLUE_STAINED_GLASS_PANE, mount.getTierSpeed(), mount.getType().getMaxSpeed()));
        inv.setItem(13, renderUpgradeBtn(player, "Sức Nhảy", Material.LIME_STAINED_GLASS_PANE, mount.getTierJump(), mount.getType().getMaxJump()));
        inv.setItem(14, renderUpgradeBtn(player, "Sức Bền", Material.RED_STAINED_GLASS_PANE, mount.getTierDurability(), mount.getType().getMaxDurability()));
        inv.setItem(15, renderUpgradeBtn(player, "Hồi Chiêu", Material.YELLOW_STAINED_GLASS_PANE, mount.getTierCooldown(), mount.getType().getMaxCooldown()));
        inv.setItem(16, renderUpgradeBtn(player, "Kho Chứa", Material.ORANGE_STAINED_GLASS_PANE, mount.getTierStorage(), mount.getType().getMaxStorage()));

        inv.setItem(17, createItem(Material.SADDLE, "§a§l► TRIỆU HỒI NGAY ◄", "§7Gọi thú cưỡi này ra ngoài."));
        inv.setItem(22, createItem(Material.BARRIER, "§cTrở về Chuồng Ngựa"));

        player.openInventory(inv);
    }

    private ItemStack renderUpgradeBtn(Player player, String name, Material mat, int curTier, int maxTier) {
        if (curTier >= maxTier) {
            return createItem(mat, "§eNâng Cấp: §f" + name, "§7Bậc hiện tại: §b" + curTier, "", "§a§lĐÃ MAX CẤP CỦA LOÀI NÀY!");
        }

        long copperCost = getUpgradeCost(player, curTier);
        int tokenCost = getTokenCost(curTier);

        return createItem(mat, "§eNâng Cấp: §f" + name,
                "§7Bậc hiện tại: §b" + curTier + " ➔ " + (curTier + 1), "",
                "§fChi phí:",
                "§8- §eTiền mặt: §f" + plugin.getCurrencyManager().formatCurrency(copperCost),
                "§8- §dGiấy Nâng Cấp Thú Cưỡi: §f" + tokenCost + " tờ", "",
                "§a► Click để nâng cấp!");
    }

    // Tính tiền Nâng cấp bằng Đồng
    // Truyền thêm Player vào để tính chiết khấu
    private long getUpgradeCost(Player player, int tier) {
        long baseCost;
        if (tier <= 4) baseCost = 15000L; // ~3.5 Vàng
        else if (tier <= 9) baseCost = 30000L; // ~7.3 Vàng
        else baseCost = 60000L; // ~14.6 Vàng

        // Trừ đi chiết khấu Thương Nhân
        double discount = vn.minecraft.rPGCore.jobs.types.MerchantJob.getBargainDiscount(player);
        return baseCost - (long)(baseCost * discount);
    }

    private int getTokenCost(int tier) {
        if (tier <= 4) return 1;
        if (tier <= 9) return 3;
        return 10;
    }

    // =========================================================================
    // 5. XỬ LÝ SỰ KIỆN CLICK TOÀN TẬP
    // =========================================================================
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains(MAIN_GUI) && !title.contains(CONFIRM_GUI) && !title.contains(SHOP_GUI) && !title.contains(DETAIL_GUI)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        PlayerMountProfile profile = plugin.getMountManager().getProfile(player.getUniqueId());

        // --- XỬ LÝ CHUỒNG CHÍNH ---
        if (title.contains(MAIN_GUI)) {
            int targetSlot = (slot == 11) ? 1 : (slot == 13) ? 2 : (slot == 15) ? 3 : -1;
            if (targetSlot == -1) return;

            if (profile.getUnlockedSlots() < targetSlot) {
                int goldCost = (targetSlot == 2) ? 100 : 200;
                openConfirmGUI(player, "Mở khóa Ô số " + targetSlot + " với giá " + goldCost + " Vàng?", "UNLOCK", targetSlot);
                return;
            }

            if (!profile.hasMountAt(targetSlot)) {
                openShopGUI(player, targetSlot);
                return;
            }

            if (event.getClick().isShiftClick()) {
                player.closeInventory();
                player.performCommand("h " + targetSlot);
            } else if (event.getClick().isRightClick()) {
                MountData oldMount = profile.getMountAt(targetSlot);
                if (oldMount.getInventoryBase64() != null && !oldMount.getInventoryBase64().isEmpty()) {
                    player.sendMessage("§c§l[!] LỖI: §cTúi đồ của thú cưỡi này vẫn còn vật phẩm!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                openConfirmGUI(player, "Bạn có chắc muốn BỎ thú cưỡi cũ để mua con mới?", "OVERWRITE", targetSlot);
            } else {
                openDetailGUI(player, targetSlot);
            }
        }

        // --- XỬ LÝ BẢNG XÁC NHẬN ---
        else if (title.contains(CONFIRM_GUI)) {
            if (slot == 15) {
                openMainStable(player);
                return;
            }
            if (slot == 11) {
                String action = confirmAction.get(player.getUniqueId());
                int targetSlot = interactingSlot.get(player.getUniqueId());

                if (action.equals("UNLOCK")) {
                    long baseVal = ((targetSlot == 2) ? 100L : 200L) * 4096L;

                    // THÊM: Tính toán chiết khấu Thương Nhân
                    double discount = vn.minecraft.rPGCore.jobs.types.MerchantJob.getBargainDiscount(player);
                    long finalVal = baseVal - (long)(baseVal * discount);

                    if (plugin.getCurrencyManager().takePhysicalCurrency(player, finalVal)) {
                        profile.setUnlockedSlots(targetSlot);

                        // Ghi Quest và EXP theo giá trị finalVal (Giá thực trả)
                        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SPEND, finalVal);
                        vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, finalVal);

                        plugin.getMountDataManager().saveProfile(player.getUniqueId());
                        player.sendMessage("§a[Hệ thống] Đã mở khóa thành công Ô số " + targetSlot + "!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        openMainStable(player);
                    } else {
                        player.sendMessage("§cBạn không đủ tiền để mở khóa!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        player.closeInventory();
                    }
                }
                // THÊM ĐOẠN NÀY VÀO ĐÂY:
                else if (action.equals("OVERWRITE")) {
                    // Lưu lại ô đang thao tác, chuyển sang shop để chọn thú mới
                    // Thú cũ sẽ được xóa tại thời điểm mua thú mới thành công (đảm bảo an toàn)
                    player.sendMessage("§e[Hệ thống] Bạn đã chọn ghi đè. Hãy chọn thú cưỡi mới!");
                    openShopGUI(player, targetSlot);
                }
            }
        }

        // --- XỬ LÝ CỬA HÀNG ---
        else if (title.contains(SHOP_GUI)) {
            // Nút quay lại ở slot 49
            if (slot == 49) { openMainStable(player); return; }

            // Tìm xem slot vừa bấm có nằm trong SHOP_SLOTS không
            int buyableIndex = -1;
            for (int i = 0; i < SHOP_SLOTS.length; i++) {
                if (SHOP_SLOTS[i] == slot) {
                    buyableIndex = i;
                    break;
                }
            }

            // Nếu không tìm thấy hoặc bấm vào ô trống
            if (buyableIndex == -1) return;

            List<MountArchetype> buyables = new ArrayList<>();
            for (MountArchetype a : MountArchetype.values()) {
                if (a.isBuyable()) buyables.add(a);
            }

            // Kiểm tra xem index đó có con thú nào không
            if (buyableIndex >= buyables.size()) return;

            MountArchetype arch = buyables.get(buyableIndex);
            int targetSlot = interactingSlot.get(player.getUniqueId());

            // THỰC HIỆN GIAO DỊCH
            if (plugin.getCurrencyManager().takePhysicalCurrency(player, arch.getPriceCopper())) {

                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SPEND, arch.getPriceCopper());
                vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, arch.getPriceCopper());

                // Thuật toán Gacha Đột Biến (5%)
                MountArchetype finalArch = arch;
                if (arch == MountArchetype.HORSE) {
                    double r = rand.nextDouble();
                    if (r < 0.05) finalArch = MountArchetype.SKELETON_HORSE;
                    else if (r >= 0.05 && r < 0.10) finalArch = MountArchetype.ZOMBIE_HORSE;
                }
                else if (arch == MountArchetype.CAMEL && rand.nextDouble() < 0.05) finalArch = MountArchetype.CAMEL_HUSK;
                else if (arch == MountArchetype.NAUTILUS && rand.nextDouble() < 0.05) finalArch = MountArchetype.ZOMBIE_NAUTILUS;

                MountData newMount = new MountData(finalArch);
                if (rand.nextDouble() <= 0.005) {
                    newMount.setTierSpeed(finalArch.getMaxSpeed()); newMount.setTierJump(finalArch.getMaxJump());
                    newMount.setTierDurability(finalArch.getMaxDurability()); newMount.setTierCooldown(finalArch.getMaxCooldown());
                    newMount.setTierStorage(finalArch.getMaxStorage());
                    Bukkit.broadcastMessage("§6§l[TIN ĐỒN] §fNgười chơi §a" + player.getName() + " §fvừa mua được một con §e" + finalArch.getDisplayName() + " §fMax Chỉ Số ở Chuồng ngựa!");
                } else {
                    newMount.setTierSpeed(1 + rand.nextInt(Math.max(1, finalArch.getMaxSpeed() - 1)));
                    newMount.setTierJump(1 + rand.nextInt(Math.max(1, finalArch.getMaxJump() - 1)));
                    newMount.setTierDurability(1 + rand.nextInt(Math.max(1, finalArch.getMaxDurability() - 1)));
                    newMount.setTierCooldown(1 + rand.nextInt(Math.max(1, finalArch.getMaxCooldown() - 1)));
                    newMount.setTierStorage(1 + rand.nextInt(Math.max(1, finalArch.getMaxStorage() - 1)));
                }

                profile.setMountAt(targetSlot, newMount);
                plugin.getMountDataManager().saveProfile(player.getUniqueId());

                player.sendMessage("§aBạn đã mua thành công: §l" + finalArch.getDisplayName());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                openMainStable(player);
            } else {
                player.sendMessage("§cBạn không đủ tiền mặt để mua phôi này!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }

        // --- XỬ LÝ CHI TIẾT & NÂNG CẤP ---
        else if (title.contains(DETAIL_GUI)) {
            if (slot == 22) { openMainStable(player); return; }
            int targetSlot = interactingSlot.get(player.getUniqueId());
            MountData mount = profile.getMountAt(targetSlot);

            if (slot == 17) {
                player.closeInventory();
                player.performCommand("h " + targetSlot);
                return;
            }

            if (slot >= 12 && slot <= 16) {
                int curTier = 0; int maxTier = 0;
                if (slot == 12) { curTier = mount.getTierSpeed(); maxTier = mount.getType().getMaxSpeed(); }
                if (slot == 13) { curTier = mount.getTierJump(); maxTier = mount.getType().getMaxJump(); }
                if (slot == 14) { curTier = mount.getTierDurability(); maxTier = mount.getType().getMaxDurability(); }
                if (slot == 15) { curTier = mount.getTierCooldown(); maxTier = mount.getType().getMaxCooldown(); }
                if (slot == 16) { curTier = mount.getTierStorage(); maxTier = mount.getType().getMaxStorage(); }

                if (curTier >= maxTier) return;

                long copperCost = getUpgradeCost(player, curTier);
                int tokenCost = getTokenCost(curTier);

                ItemStack tokenTemplate = plugin.getItemManager().buildItem(MOUNT_UPGRADE_ITEM_ID, 1);

                if (plugin.getItemManager().countItem(player, tokenTemplate) < tokenCost) {
                    player.sendMessage("§cBạn không có đủ " + tokenCost + " Giấy Nâng Cấp Thú Cưỡi!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (!plugin.getCurrencyManager().takePhysicalCurrency(player, copperCost)) {
                    player.sendMessage("§cBạn không đủ tiền để nâng cấp!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                plugin.getItemManager().takeItem(player, tokenTemplate, tokenCost);

                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_SPEND, copperCost);
                vn.minecraft.rPGCore.jobs.core.JobManager.processMerchantExp(player, copperCost);

                if (slot == 12) mount.setTierSpeed(curTier + 1);
                if (slot == 13) mount.setTierJump(curTier + 1);
                if (slot == 14) mount.setTierDurability(curTier + 1);
                if (slot == 15) mount.setTierCooldown(curTier + 1);
                if (slot == 16) mount.setTierStorage(curTier + 1);

                plugin.getMountDataManager().saveProfile(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                player.sendMessage("§a[Chuồng Ngựa] Nâng cấp thành công lên Bậc " + (curTier + 1) + "!");
                openDetailGUI(player, targetSlot);
            }
        }
    }

    private void fillBackground(Inventory inv) {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, bg);
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            if (!line.isEmpty()) lore.add(line);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}