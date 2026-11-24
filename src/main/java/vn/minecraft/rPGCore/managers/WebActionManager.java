package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import vn.minecraft.rPGCore.database.MongoManager;

import java.util.Date;

public class WebActionManager {

    private final JavaPlugin plugin;
    private final MongoCollection<Document> actionCollection;
    private BukkitTask pollingTask;

    public WebActionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.actionCollection = MongoManager.getInstance()
                .getDatabase()
                .getCollection("web_operator_actions");
    }

    public void start() {
        // Quét async mỗi 3 giây (60 ticks) để không ảnh hưởng TPS Main Thread
        this.pollingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::pollPendingActions,
                60L,
                60L
        );
        plugin.getLogger().info("[RPGCore] WebActionManager đã kích hoạt (chu kỳ 3s).");
    }

    public void stop() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel();
        }
    }

    private void pollPendingActions() {
        for (Document doc : actionCollection.find(Filters.eq("status", "PENDING"))
                .sort(Sorts.ascending("createdAt"))) {

            String action = doc.getString("action");
            Document payload = doc.get("payload", Document.class);

            // 1. Nhóm action xử lý tiền cược Giấy Phép
            if ("LICENSE_BET_DEBIT".equals(action) || "LICENSE_BET_PAYOUT".equals(action)) {
                handleLicenseBetAction(doc, action, payload);
                continue;
            }

            // 2. Nhóm action Web Shop (VIP, Mở Slot, Vật phẩm RPG)
            if ("GRANT_VIP".equals(action) || "UNLOCK_SLOT".equals(action) || "GIVE_RPG_ITEM".equals(action)) {
                handleWebShopAction(doc, action, payload);
                continue;
            }

            // 2. Nhóm lệnh điều hành qua Console (KICK, BAN, WARN, MUTE)
            String target = doc.getString("targetUsername");
            String reason = doc.getString("reason") != null && !doc.getString("reason").isEmpty()
                    ? doc.getString("reason")
                    : "Web Operator";

            String consoleCmd = buildMinecraftCommand(action, target, payload, reason);
            if (consoleCmd == null) {
                actionCollection.updateOne(
                        Filters.eq("_id", doc.getObjectId("_id")),
                        Updates.combine(
                                Updates.set("status", "FAILED"),
                                Updates.set("errorMessage", "Unsupported action: " + action)
                        )
                );
                continue;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd);
                    actionCollection.updateOne(
                            Filters.eq("_id", doc.getObjectId("_id")),
                            Updates.combine(
                                    Updates.set("status", success ? "EXECUTED" : "FAILED"),
                                    Updates.set("executedAt", new Date())
                            )
                    );
                } catch (Exception ex) {
                    actionCollection.updateOne(
                            Filters.eq("_id", doc.getObjectId("_id")),
                            Updates.combine(
                                    Updates.set("status", "FAILED"),
                                    Updates.set("errorMessage", ex.getMessage())
                            )
                    );
                }
            });
        }
    }

    private void handleLicenseBetAction(Document doc, String action, Document payload) {
        org.bson.types.ObjectId actionId = doc.getObjectId("_id");
        if (payload == null) {
            markActionFailedSync(actionId, "Missing payload");
            return;
        }

        // Khóa nguyên tử PENDING -> PROCESSING ngay trên Async Thread để chống xử lý trùng lặp
        com.mongodb.client.result.UpdateResult claimResult = actionCollection.updateOne(
                Filters.and(Filters.eq("_id", actionId), Filters.eq("status", "PENDING")),
                Updates.set("status", "PROCESSING")
        );
        if (claimResult.getModifiedCount() == 0) {
            return;
        }

        String targetUuidStr = payload.getString("targetUuid");
        Number amountNum = payload.get("amount", Number.class);
        String betId = payload.getString("betId");
        String message = payload.getString("message");

        if (targetUuidStr == null || amountNum == null || amountNum.longValue() <= 0) {
            markActionFailedSync(actionId, "Invalid targetUuid or amount in payload");
            return;
        }

        final java.util.UUID targetUuid;
        try {
            targetUuid = java.util.UUID.fromString(targetUuidStr);
        } catch (IllegalArgumentException ex) {
            markActionFailedSync(actionId, "Invalid UUID format: " + targetUuidStr);
            return;
        }

        long amount = amountNum.longValue();

        // Chạy trên Main Thread để đọc/ghi RPGPlayer online trên RAM an toàn
        Bukkit.getScheduler().runTask(plugin, () -> {
            CurrencyManager currencyManager = vn.minecraft.rPGCore.RPGCore.getInstance().getCurrencyManager();
            boolean ok;

            if ("LICENSE_BET_DEBIT".equals(action)) {
                ok = currencyManager.processOfflinePayment(targetUuid, amount);
            } else {
                ok = currencyManager.processOfflineDeposit(targetUuid, amount, message);
            }

            // Đẩy thao tác cập nhật MongoDB về Async Thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean hasValidBetId = betId != null && org.bson.types.ObjectId.isValid(betId);

                if (ok) {
                    actionCollection.updateOne(
                            Filters.eq("_id", actionId),
                            Updates.combine(
                                    Updates.set("status", "EXECUTED"),
                                    Updates.set("executedAt", new Date())
                            )
                    );
                    if ("LICENSE_BET_DEBIT".equals(action) && hasValidBetId) {
                        MongoManager.getInstance().getDatabase().getCollection("license_bets").updateOne(
                                Filters.eq("_id", new org.bson.types.ObjectId(betId)),
                                Updates.combine(
                                        Updates.set("status", "CONFIRMED"),
                                        Updates.set("updatedAt", new Date())
                                )
                        );
                    }
                } else {
                    String err = "LICENSE_BET_DEBIT".equals(action) ? "INSUFFICIENT_BALANCE" : "DEPOSIT_FAILED";
                    markActionFailedSync(actionId, err);
                    if ("LICENSE_BET_DEBIT".equals(action) && hasValidBetId) {
                        MongoManager.getInstance().getDatabase().getCollection("license_bets").updateOne(
                                Filters.eq("_id", new org.bson.types.ObjectId(betId)),
                                Updates.combine(
                                        Updates.set("status", "FAILED"),
                                        Updates.set("failReason", err),
                                        Updates.set("updatedAt", new Date())
                                )
                        );
                    }
                }
            });
        });
    }

    private void markActionFailedAsync(org.bson.types.ObjectId actionId, String errorMessage) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> markActionFailedSync(actionId, errorMessage));
    }

    private void markActionFailedSync(org.bson.types.ObjectId actionId, String errorMessage) {
        actionCollection.updateOne(
                Filters.eq("_id", actionId),
                Updates.combine(
                        Updates.set("status", "FAILED"),
                        Updates.set("errorMessage", errorMessage),
                        Updates.set("executedAt", new Date())
                )
        );
    }

    private String buildMinecraftCommand(String action, String target, Document payload, String reason) {
        return switch (action) {
            case "KICK_PLAYER" -> "kick " + target + " " + reason;
            case "BAN_PLAYER" -> "ban " + target + " " + reason;
            case "WARN_PLAYER" -> "warn " + target + " " + reason;
            case "MUTE_PLAYER" -> {
                int minutes = (payload != null && payload.getInteger("minutes") != null)
                        ? payload.getInteger("minutes") : 10;
                yield "mute " + target + " " + minutes + "m " + reason;
            }
            default -> null;
        };
    }
    private void handleWebShopAction(Document doc, String action, Document payload) {
        org.bson.types.ObjectId actionId = doc.getObjectId("_id");
        if (payload == null) {
            markActionFailedSync(actionId, "Missing payload");
            return;
        }

        // Khóa nguyên tử chống xử lý trùng lặp
        com.mongodb.client.result.UpdateResult claimResult = actionCollection.updateOne(
                Filters.and(Filters.eq("_id", actionId), Filters.eq("status", "PENDING")),
                Updates.set("status", "PROCESSING")
        );
        if (claimResult.getModifiedCount() == 0) return;

        String targetUuidStr = payload.getString("targetUuid");
        if (targetUuidStr == null) {
            markActionFailedSync(actionId, "Missing targetUuid");
            return;
        }

        final java.util.UUID targetUuid;
        try {
            targetUuid = java.util.UUID.fromString(targetUuidStr);
        } catch (IllegalArgumentException ex) {
            markActionFailedSync(actionId, "Invalid UUID: " + targetUuidStr);
            return;
        }

        vn.minecraft.rPGCore.RPGCore core = vn.minecraft.rPGCore.RPGCore.getInstance();

        if ("GRANT_VIP".equals(action)) {
            int days = (payload.get("days") instanceof Number n) ? n.intValue() : 30;
            core.getVipManager().grantVip(targetUuid, days);
            actionCollection.updateOne(
                    Filters.eq("_id", actionId),
                    Updates.combine(Updates.set("status", "EXECUTED"), Updates.set("executedAt", new Date()))
            );
            return;
        }

        if ("UNLOCK_SLOT".equals(action)) {
            MongoCollection<Document> accounts = MongoManager.getInstance().getDatabase().getCollection("player_accounts");
            Document acc = accounts.find(Filters.eq("_id", targetUuid.toString())).first();
            if (acc == null) {
                markActionFailedSync(actionId, "Account not found");
                return;
            }
            int currentMax = vn.minecraft.rPGCore.managers.AuthManager.getSafeInt(acc, "max_slots", 2);
            if (currentMax >= vn.minecraft.rPGCore.player.PlayerManager.MAX_ALLOWED_SLOTS) {
                markActionFailedSync(actionId, "MAX_SLOTS_REACHED");
                return;
            }
            int newMax = currentMax + 1;
            accounts.updateOne(Filters.eq("_id", targetUuid.toString()), Updates.set("max_slots", newMax));
            actionCollection.updateOne(
                    Filters.eq("_id", actionId),
                    Updates.combine(Updates.set("status", "EXECUTED"), Updates.set("executedAt", new Date()))
            );

            Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player online = Bukkit.getPlayer(targetUuid);
                if (online != null && online.isOnline()) {
                    online.playSound(online.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    online.sendMessage("§a§l[Web Shop] §fTài khoản của bạn vừa mở khóa vĩnh viễn §eSlot nhân vật thứ " + newMax + " §f(§e/slot§f)!");
                }
            });
            return;
        }

        if ("GIVE_RPG_ITEM".equals(action)) {
            String itemId = payload.getString("itemId");
            int amount = (payload.get("amount") instanceof Number n) ? Math.max(1, n.intValue()) : 1;
            if (itemId == null || itemId.isEmpty()) {
                markActionFailedSync(actionId, "Missing itemId");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player online = Bukkit.getPlayer(targetUuid);
                if (online != null && online.isOnline()) {
                    org.bukkit.inventory.ItemStack item = core.getItemManager().buildItem(itemId, amount);
                    if (item == null) {
                        markActionFailedAsync(actionId, "Unknown itemId: " + itemId);
                        return;
                    }
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = online.getInventory().addItem(item);
                    for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                        online.getWorld().dropItemNaturally(online.getLocation(), drop);
                    }
                    online.playSound(online.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                    online.sendMessage("§a§l[Web Shop] §fBạn vừa nhận được §e" + amount + "x " + item.getItemMeta().getDisplayName() + " §ftừ cửa hàng Web!");

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> actionCollection.updateOne(
                            Filters.eq("_id", actionId),
                            Updates.combine(Updates.set("status", "EXECUTED"), Updates.set("executedAt", new Date()))
                    ));
                } else {
                    // Nếu Offline: đẩy vào hàng đợi pending_web_items trong player_accounts để trả khi login
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        Document pendingEntry = new Document("item_id", itemId)
                                .append("amount", amount)
                                .append("created_at", System.currentTimeMillis());
                        MongoManager.getInstance().getDatabase().getCollection("player_accounts").updateOne(
                                Filters.eq("_id", targetUuid.toString()),
                                Updates.push("pending_web_items", pendingEntry)
                        );
                        actionCollection.updateOne(
                                Filters.eq("_id", actionId),
                                Updates.combine(Updates.set("status", "EXECUTED"), Updates.set("executedAt", new Date()))
                        );
                    });
                }
            });
        }
    }
}