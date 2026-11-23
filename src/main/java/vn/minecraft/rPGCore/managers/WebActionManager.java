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

            // 1. Nhóm action xử lý tiền cược Giấy Phép (không dùng lệnh console)
            if ("LICENSE_BET_DEBIT".equals(action) || "LICENSE_BET_PAYOUT".equals(action)) {
                handleLicenseBetAction(doc, action, payload);
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
}