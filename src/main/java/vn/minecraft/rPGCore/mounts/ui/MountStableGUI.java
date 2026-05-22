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
import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;
import vn.minecraft.rPGCore.mounts.type.MountArchetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MountStableGUI implements Listener {

    public static final String MAIN_GUI = "§8Chuồng Ngựa Của Bạn";
    public static final String UPGRADE_GUI = "§8Nâng Cấp Thú Cưỡi";

    // Giả lập lấy ID của Item Chứng Nhận Thương Nhân (Bạn thay ID thực tế của server vào đây)
    public static final String MERCHANT_TOKEN_ID = "item_merchant_certificate";

    private final Random rand = new Random();

    // =========================================================================
    // 1. GIAO DIỆN CHUỒNG NGỰA CHÍNH (MAIN STABLE)
    // =========================================================================
    public void openMainStable(Player player, PlayerMountProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_GUI);
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        // Render Slot 1, 2, 3
        inv.setItem(11, renderSlot(profile, 1, 0));       // Slot 1: Miễn phí
        inv.setItem(13, renderSlot(profile, 2, 100));     // Slot 2: 100 Vàng
        inv.setItem(15, renderSlot(profile, 3, 200));     // Slot 3: 200 Vàng

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1f);
    }

    private ItemStack renderSlot(PlayerMountProfile profile, int slotIndex, int unlockCostGold) {
        // Nếu slot chưa được mở khóa
        if (profile.getUnlockedSlots() < slotIndex) {
            ItemStack locked = createItem(Material.RED_STAINED_GLASS_PANE, "§c§lÔ CHUỒNG SỐ " + slotIndex + " [BỊ KHÓA]");
            ItemMeta meta = locked.getItemMeta();
            meta.setLore(Arrays.asList(
                    "§7Bạn chưa mở khóa ô chuồng này.",
                    "",
                    "§eChi phí mở khóa: §l" + unlockCostGold + " Vàng",
                    "§a► Click để mua mở rộng ngay!"
            ));
            locked.setItemMeta(meta);
            return locked;
        }

        // Nếu slot đã mở nhưng chưa có thú (Trống)
        if (!profile.hasMountAt(slotIndex)) {
            ItemStack empty = createItem(Material.MINECART, "§a§lÔ CHUỒNG SỐ " + slotIndex + " [TRỐNG]");
            ItemMeta meta = empty.getItemMeta();
            meta.setLore(Arrays.asList(
                    "§7Ô chuồng này đã sẵn sàng.",
                    "",
                    "§a► Click để tới Cửa Hàng mua Thú cưỡi mới!"
            ));
            empty.setItemMeta(meta);
            return empty;
        }

        // Nếu đã có thú cưỡi
        MountData mount = profile.getMountAt(slotIndex);
        ItemStack saddle = createItem(Material.SADDLE, "§6§l" + mount.getCustomName());
        ItemMeta meta = saddle.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add("§8[ Chủng loại: " + mount.getType().getDisplayName() + " ]");
        lore.add("");
        lore.add("§f- Tốc độ: §bBậc " + mount.getTierSpeed() + " §8/ " + mount.getType().getMaxSpeed());
        lore.add("§f- Sức nhảy: §aBậc " + mount.getTierJump() + " §8/ " + mount.getType().getMaxJump());
        lore.add("§f- Sức bền (Máu): §cBậc " + mount.getTierDurability() + " §8/ " + mount.getType().getMaxDurability());
        lore.add("§f- Giảm hồi chiêu: §eBậc " + mount.getTierCooldown() + " §8/ " + mount.getType().getMaxCooldown());
        lore.add("§f- Kho chứa đồ: §6Bậc " + mount.getTierStorage() + " §8/ " + mount.getType().getMaxStorage());
        lore.add("");
        if (mount.isDead()) {
            long sec = mount.getRemainingCooldownMs() / 1000;
            lore.add("§c§l[!] Đang Hồi Sức: §f" + (sec / 60) + " phút " + (sec % 60) + " giây");
        } else {
            lore.add("§a§l[!] Sẵn sàng triệu hồi");
        }
        lore.add("");
        lore.add("§e► Chuột Trái: §fGọi Thú Cưỡi (/h " + slotIndex + ")");
        lore.add("§d► Chuột Phải: §fMở Bảng Nâng Cấp");
        lore.add("§c► Shift + Phải: §fĐổi Thú Cưỡi mới (Ghi đè)");

        meta.setLore(lore);
        saddle.setItemMeta(meta);
        return saddle;
    }

    // =========================================================================
    // 2. GIAO DIỆN NÂNG CẤP CHỈ SỐ (UPGRADE GUI)
    // =========================================================================
    public void openUpgradeGUI(Player player, MountData mount, int slotIndex) {
        Inventory inv = Bukkit.createInventory(null, 36, UPGRADE_GUI + " - Ô " + slotIndex);

        MountArchetype arch = mount.getType();

        inv.setItem(11, renderUpgradeIcon(Material.LEATHER_BOOTS, "Tốc Độ", mount.getTierSpeed(), arch.getMaxSpeed()));
        inv.setItem(12, renderUpgradeIcon(Material.RABBIT_FOOT, "Sức Nhảy", mount.getTierJump(), arch.getMaxJump()));
        inv.setItem(13, renderUpgradeIcon(Material.GOLDEN_APPLE, "Sức Bền (Máu)", mount.getTierDurability(), arch.getMaxDurability()));
        inv.setItem(14, renderUpgradeIcon(Material.CLOCK, "Giảm Hồi Chiêu", mount.getTierCooldown(), arch.getMaxCooldown()));
        inv.setItem(15, renderUpgradeIcon(Material.CHEST, "Kho Chứa Đồ", mount.getTierStorage(), arch.getMaxStorage()));

        // Nút quay lại
        inv.setItem(31, createItem(Material.ARROW, "§cTrở về Chuồng Ngựa"));
        player.openInventory(inv);
    }

    private ItemStack renderUpgradeIcon(Material mat, String statName, int currentTier, int maxTier) {
        ItemStack item = createItem(mat, "§e§lNâng Cấp: §f" + statName);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add("§7Cấp bậc hiện tại: §b" + currentTier + " §8/ §b" + maxTier);
        lore.add("");

        if (currentTier >= maxTier) {
            lore.add("§a§lĐÃ ĐẠT CẤP TỐI ĐA TRẦN CỦA LOÀI NÀY!");
        } else {
            long costCopper = getSilverCost(currentTier) * 64L; // 1 Bạc = 64 Đồng
            int tokenCost = getTokenCost(currentTier);

            lore.add("§fYêu cầu Nâng cấp lên Bậc " + (currentTier + 1) + ":");
            lore.add("§8- §eTiền mặt: §f" + costCopper + " Đồng");
            lore.add("§8- §dChứng Nhận Thương Nhân: §f" + tokenCost + " cái");
            lore.add("");
            lore.add("§a► Click để tiến hành Nâng Cấp!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private long getSilverCost(int tier) {
        if (tier <= 4) return 500;
        if (tier <= 9) return 5000;
        return 50000;
    }

    private int getTokenCost(int tier) {
        if (tier <= 4) return 1;
        if (tier <= 9) return 3;
        return 10;
    }

    // =========================================================================
    // 3. XỬ LÝ SỰ KIỆN CLICK (LISTENER)
    // =========================================================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (!title.contains(MAIN_GUI) && !title.contains(UPGRADE_GUI)) return;
        event.setCancelled(true); // Khóa không cho lấy item ra ngoài

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Tạm giả lập lấy Profile. Bạn sẽ đổi thành MountManager.getProfile(player) ở Phase 3
        PlayerMountProfile profile = getDummyProfile(player);

        // --- XỬ LÝ MAIN STABLE ---
        if (title.contains(MAIN_GUI)) {
            int targetSlot = -1;
            int unlockCost = 0;
            if (slot == 11) { targetSlot = 1; unlockCost = 0; }
            if (slot == 13) { targetSlot = 2; unlockCost = 100; }
            if (slot == 15) { targetSlot = 3; unlockCost = 200; }
            if (targetSlot == -1) return;

            // 1. Mua mở khóa Slot
            if (profile.getUnlockedSlots() < targetSlot) {
                // Gọi tới CurrencyManager kiểm tra VÀNG ở đây
                // if (CurrencyManager.takeGold(player, unlockCost)) {
                profile.setUnlockedSlots(targetSlot);
                player.sendMessage("§aBạn đã mở khóa thành công Ô Chuồng Số " + targetSlot + "!");
                openMainStable(player, profile);
                // } else { player.sendMessage("§cBạn không đủ " + unlockCost + " Vàng!"); }
                return;
            }

            // 2. Mua/Ghi đè Thú Cưỡi
            if (!profile.hasMountAt(targetSlot) || event.getClick().isShiftClick()) {

                // [CHỐT CHẶN PHƯƠNG ÁN C]: Chặn ghi đè nếu túi đồ thú cũ còn vật phẩm
                if (profile.hasMountAt(targetSlot)) {
                    MountData oldMount = profile.getMountAt(targetSlot);
                    if (oldMount.getInventoryBase64() != null && !oldMount.getInventoryBase64().isEmpty()) {
                        player.sendMessage("§c§l[!] KHÔNG THỂ GHI ĐÈ!");
                        player.sendMessage("§cHãy gọi thú cưỡi ra và lấy hết đồ trong túi của nó trước khi đổi thú mới!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                }

                // Thực hiện Gacha Phôi Mới (Ví dụ: Mua HORSE)
                // Bạn có thể tạo 1 GUI Shop riêng. Ở đây viết code giả lập hàm Gacha nổ hũ
                gachaNewMount(player, profile, targetSlot, MountArchetype.HORSE);
                openMainStable(player, profile);
                return;
            }

            // 3. Chuột Phải -> Mở Bảng Nâng cấp
            if (event.getClick().isRightClick()) {
                openUpgradeGUI(player, profile.getMountAt(targetSlot), targetSlot);
            }

            // 4. Chuột Trái -> Gọi thú
            if (event.getClick().isLeftClick()) {
                player.closeInventory();
                player.performCommand("h " + targetSlot);
            }
        }

        // --- XỬ LÝ BẢNG NÂNG CẤP ---
        if (title.contains(UPGRADE_GUI)) {
            if (slot == 31) {
                openMainStable(player, profile);
                return;
            }

            // Lấy SlotIndex từ Title
            int mountSlot = Integer.parseInt(title.substring(title.length() - 1));
            MountData mount = profile.getMountAt(mountSlot);

            // Xử lý logic trừ tiền/token và nâng cấp ở Phase 3 (gọi tới MountManager)
            // Ví dụ: handleUpgradeClick(player, mount, slotClicked);
            player.sendMessage("§eTính năng trừ tiền và nâng cấp sẽ được link ở Phase 3!");
        }
    }

    // =========================================================================
    // 4. THUẬT TOÁN GACHA NỔ HŨ (TẠO PHÔI KHỞI ĐIỂM)
    // =========================================================================
    private void gachaNewMount(Player player, PlayerMountProfile profile, int slotIndex, MountArchetype baseArch) {
        // Tỷ lệ biến thể (Ví dụ: Mua ngựa có 5% ra Ngựa Xương, 5% Ngựa Zombie)
        MountArchetype finalArch = baseArch;
        if (baseArch == MountArchetype.HORSE) {
            double roll = rand.nextDouble();
            if (roll < 0.05) finalArch = MountArchetype.SKELETON_HORSE;
            else if (roll < 0.10) finalArch = MountArchetype.ZOMBIE_HORSE;
        }

        MountData mount = new MountData(finalArch);

        // Nổ hũ 0.5% ra Max Tiers ngay lập tức
        if (rand.nextDouble() <= 0.005) {
            mount.setTierSpeed(finalArch.getMaxSpeed());
            mount.setTierJump(finalArch.getMaxJump());
            mount.setTierDurability(finalArch.getMaxDurability());
            mount.setTierCooldown(finalArch.getMaxCooldown());
            mount.setTierStorage(finalArch.getMaxStorage());
            Bukkit.broadcastMessage("§6§l[TIN ĐỒN] §fNgười chơi §a" + player.getName() + " §fvừa nổ hũ nhận được một con §e" + finalArch.getDisplayName() + " §fMax Chỉ số!!!");
        } else {
            // Ngẫu nhiên từ 1 đến (Max - Offset) để người chơi phải cày
            mount.setTierSpeed(1 + rand.nextInt(Math.max(1, finalArch.getMaxSpeed() - 1)));
            mount.setTierJump(1 + rand.nextInt(Math.max(1, finalArch.getMaxJump() - 1)));
            mount.setTierDurability(1 + rand.nextInt(Math.max(1, finalArch.getMaxDurability() - 1)));
            mount.setTierCooldown(1 + rand.nextInt(Math.max(1, finalArch.getMaxCooldown() - 1)));
            mount.setTierStorage(1 + rand.nextInt(Math.max(1, finalArch.getMaxStorage() - 1)));
        }

        profile.setMountAt(slotIndex, mount);
        player.sendMessage("§aBạn đã mua thành công Phôi thú cưỡi: §l" + finalArch.getDisplayName());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private PlayerMountProfile getDummyProfile(Player p) {
        // Method tạm để tránh lỗi biên dịch ở Phase 2
        return new PlayerMountProfile(p.getUniqueId());
    }
}