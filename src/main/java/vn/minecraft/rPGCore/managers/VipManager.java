package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VipManager {

    public static final long VIP_GOLD_COST = 100L * 4096L; // 100 Vàng = 409,600 Đồng
    public static final int VIP_DURATION_DAYS = 30;
    public static final long DAY_MILLIS = 86_400_000L;
    public static final String VIP_PREFIX = "§6§l[VIP] §f";

    private final JavaPlugin plugin;
    private final AuthManager authManager;
    private final Set<UUID> onlineVips = ConcurrentHashMap.newKeySet();

    public VipManager(JavaPlugin plugin, AuthManager authManager) {
        this.plugin = plugin;
        this.authManager = authManager;
    }

    public boolean isOnlineVip(UUID uuid) {
        return onlineVips.contains(uuid);
    }

    public void removeOnlineVip(UUID uuid) {
        onlineVips.remove(uuid);
    }

    /**
     * Kiểm tra trạng thái VIP từ MongoDB và tự động hạ cấp nếu đã hết hạn (giữ nguyên max_slots = 3)
     */
    public boolean checkAndSyncVip(UUID uuid) {
        MongoCollection<Document> accounts = authManager.getAccountCollection();
        Document account = accounts.find(Filters.eq("_id", uuid.toString())).first();
        if (account == null) {
            onlineVips.remove(uuid);
            return false;
        }

        boolean isVip = Boolean.TRUE.equals(account.getBoolean("is_vip"));
        long expiresAt = AuthManager.getSafeLong(account, "vip_expires_at", 0L);

        if (!isVip) {
            onlineVips.remove(uuid);
            return false;
        }

        if (expiresAt > 0L && expiresAt <= System.currentTimeMillis()) {
            // Hết hạn -> hạ is_vip = false nhưng KHÔNG giảm max_slots
            accounts.updateOne(
                    Filters.eq("_id", uuid.toString()),
                    Updates.set("is_vip", false)
            );
            onlineVips.remove(uuid);

            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (onlinePlayer.isOnline()) {
                        onlinePlayer.setPlayerListName(onlinePlayer.getName());
                        onlinePlayer.setDisplayName(onlinePlayer.getName());
                        onlinePlayer.sendMessage("§e§l[VIP] §cGói VIP của bạn đã hết hạn! Slot nhân vật thứ 3 đã mở vẫn được giữ nguyên.");
                    }
                });
            }
            return false;
        }

        onlineVips.add(uuid);
        return true;
    }

    /**
     * Kích hoạt hoặc gia hạn VIP thêm số ngày chỉ định
     */
    public void grantVip(UUID uuid, int days) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MongoCollection<Document> accounts = authManager.getAccountCollection();
            Document account = accounts.find(Filters.eq("_id", uuid.toString())).first();
            if (account == null) return;

            long now = System.currentTimeMillis();
            long currentExpiry = AuthManager.getSafeLong(account, "vip_expires_at", 0L);
            boolean hasUnlockedSlot = Boolean.TRUE.equals(account.getBoolean("has_unlocked_vip_slot"));

            long baseTime = Math.max(currentExpiry, now);
            long newExpiry = baseTime + (days * DAY_MILLIS);

            List<Bson> updates = new ArrayList<>();
            updates.add(Updates.set("is_vip", true));
            updates.add(Updates.set("vip_expires_at", newExpiry));

            boolean firstTimeUnlockSlot = false;
            if (!hasUnlockedSlot) {
                updates.add(Updates.set("max_slots", AuthManager.VIP_MAX_SLOTS));
                updates.add(Updates.set("has_unlocked_vip_slot", true));
                firstTimeUnlockSlot = true;
            }

            accounts.updateOne(Filters.eq("_id", uuid.toString()), Updates.combine(updates));
            onlineVips.add(uuid);

            final boolean unlockedNow = firstTimeUnlockSlot;
            final String formattedDate = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(newExpiry));

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    applyVipVisuals(player);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    player.sendMessage("§6§l========================================");
                    player.sendMessage("§6§l[VIP] §aKích hoạt / Gia hạn đặc quyền VIP thành công!");
                    player.sendMessage("§e• Thời hạn sử dụng đến: §f" + formattedDate);
                    if (unlockedNow) {
                        player.sendMessage("§e• Đặc quyền lần đầu: §aMở khóa vĩnh viễn Slot nhân vật thứ 3 (§f/slot§a)!");
                    }
                    player.sendMessage("§6§l========================================");
                }
            });
        });
    }

    /**
     * Mua gói VIP 30 ngày bằng 100 Vàng (409,600 Đồng) trong game
     */
    public void purchaseVipWithGold(Player player) {
        UUID uuid = player.getUniqueId();
        if (!authManager.isLoggedIn(player)) {
            player.sendMessage("§c§l[!] §cBạn cần đăng nhập trước khi mua VIP!");
            return;
        }

        PlayerManager playerManager = RPGCore.getInstance().getPlayerManager();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
        if (rpgPlayer == null) {
            player.sendMessage("§c§l[!] §cKhông tìm thấy dữ liệu nhân vật!");
            return;
        }

        if (rpgPlayer.getBalance() < VIP_GOLD_COST) {
            long currentGold = rpgPlayer.getBalance() / 4096L;
            player.sendMessage("§c§l[!] §cBạn không đủ §e100 Vàng §cđể mua VIP 30 ngày! (Hiện có: §e" + currentGold + " Vàng§c)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Trừ tiền ngay trên Main Thread và lưu DB ngay để chống dupe
        if (!rpgPlayer.withdraw(VIP_GOLD_COST)) {
            player.sendMessage("§c§l[!] §cGiao dịch thất bại, vui lòng thử lại!");
            return;
        }

        playerManager.savePlayer(uuid, true, false);
        grantVip(uuid, VIP_DURATION_DAYS);

        Bukkit.broadcastMessage("§6§l[VIP] §fNgười chơi §e" + player.getName() + " §fvừa kích hoạt gói §6§l[VIP] 30 Ngày §fbằng §e100 Vàng§f!");
    }

    /**
     * Áp dụng tiền tố [VIP] lên Tablist và DisplayName
     */
    public void applyVipVisuals(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean active = checkAndSyncVip(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (active) {
                    player.setPlayerListName(VIP_PREFIX + player.getName());
                    player.setDisplayName(VIP_PREFIX + player.getName());
                } else {
                    player.setPlayerListName(player.getName());
                    player.setDisplayName(player.getName());
                }
            });
        });
    }

    public String getVipStatusSummary(UUID uuid) {
        Document account = authManager.getAccountDocument(uuid);
        if (account == null) return "§7Chưa có dữ liệu tài khoản.";

        boolean active = checkAndSyncVip(uuid);
        int maxSlots = AuthManager.getSafeInt(account, "max_slots", AuthManager.DEFAULT_MAX_SLOTS);
        long expiresAt = AuthManager.getSafeLong(account, "vip_expires_at", 0L);

        if (active) {
            String dateStr = expiresAt > 0L
                    ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(expiresAt))
                    : "Vĩnh viễn";
            return "§6§l[VIP] Đang kích hoạt §7| §eHết hạn: §f" + dateStr + " §7| §eSố Slot: §a" + maxSlots + "/3";
        }
        return "§7Tài khoản Thường §7| §eSố Slot: §f" + maxSlots + "/3";
    }
}