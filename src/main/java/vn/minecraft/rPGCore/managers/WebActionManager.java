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
            String target = doc.getString("targetUsername");
            String reason = doc.getString("reason") != null && !doc.getString("reason").isEmpty()
                    ? doc.getString("reason")
                    : "Web Operator";
            Document payload = doc.get("payload", Document.class);

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

            // Đưa về Main Thread của Paper 1.21 để dispatchCommand an toàn
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