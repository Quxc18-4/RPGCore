package vn.minecraft.rPGCore.managers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.database.MongoManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AuthManager {

    public static final int DEFAULT_MAX_SLOTS = 2;
    public static final int VIP_MAX_SLOTS = 3;

    private final JavaPlugin plugin;
    private final MongoCollection<Document> collection;

    // Lưu trạng thái xem người chơi đã đăng nhập chưa (Chỉ lưu trên RAM)
    private final Map<UUID, Boolean> loggedInPlayers = new HashMap<>();

    public AuthManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.collection = MongoManager.getInstance().getDatabase().getCollection("player_accounts");
    }

    public MongoCollection<Document> getAccountCollection() {
        return collection;
    }

    public boolean isLoggedIn(Player player) {
        return loggedInPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public Document getAccountDocument(UUID uuid) {
        return collection.find(Filters.eq("_id", uuid.toString())).first();
    }

    public int getMaxSlots(UUID uuid) {
        Document account = getAccountDocument(uuid);
        if (account == null) return DEFAULT_MAX_SLOTS;
        return getSafeInt(account, "max_slots", DEFAULT_MAX_SLOTS);
    }

    public boolean isVipActive(UUID uuid) {
        Document account = getAccountDocument(uuid);
        if (account == null) return false;

        boolean isVip = Boolean.TRUE.equals(account.getBoolean("is_vip"));
        long expiresAt = getSafeLong(account, "vip_expires_at", 0L);

        if (!isVip) return false;

        // Nếu có mốc thời gian hết hạn và đã quá hạn -> tự động hạ cấp is_vip về false (vẫn giữ max_slots = 3)
        if (expiresAt > 0L && expiresAt <= System.currentTimeMillis()) {
            collection.updateOne(
                    Filters.eq("_id", uuid.toString()),
                    Updates.set("is_vip", false)
            );
            return false;
        }
        return true;
    }

    // ==========================================
    // TÍNH NĂNG ĐĂNG KÝ
    // ==========================================
    public void register(Player player, String password, String email) {
        UUID uuid = player.getUniqueId();

        // Kiểm tra xem tài khoản đã tồn tại chưa
        if (collection.find(Filters.eq("_id", uuid.toString())).first() != null) {
            player.sendMessage("§c§l[!] §cTài khoản này đã được đăng ký! Vui lòng dùng lệnh /login <mật khẩu>");
            return;
        }

        // Validate định dạng email chuẩn (VD: player@gmail.com, admin@yahoo.com)
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            player.sendMessage("§c§l[!] §cEmail không hợp lệ! Vui lòng nhập đúng định dạng email thật.");
            return;
        }

        if (password.length() < 6) {
            player.sendMessage("§c§l[!] §cMật khẩu quá ngắn! Phải có ít nhất 6 ký tự.");
            return;
        }

        // Mã hóa mật khẩu trước khi đẩy xuống DB
        String hashedPassword = hashPassword(password);

        Document account = new Document("_id", uuid.toString())
                .append("playerName", player.getName())
                .append("password", hashedPassword)
                .append("email", email)
                .append("is_vip", false)
                .append("vip_expires_at", 0L)
                .append("max_slots", DEFAULT_MAX_SLOTS)
                .append("has_unlocked_vip_slot", false);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            collection.insertOne(account);
            loggedInPlayers.put(uuid, true);
            player.sendMessage("§a§l[!] §aĐăng ký tài khoản thành công! Tự động đăng nhập.");

            // [ĐÃ SỬA]: Bạn quên chèn đoạn khôi phục nhân vật vào đây!
            Bukkit.getScheduler().runTask(plugin, () -> {
                vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().loadPlayer(player);
                vn.minecraft.rPGCore.RPGCore.getInstance().getBossBarManager().createBars(player, rpgPlayer);
                vn.minecraft.rPGCore.RPGCore.getInstance().getVipManager().applyVipVisuals(player);
                player.sendMessage("§e[Hệ Thống] §fDữ liệu nhân vật và túi đồ đã tải thành công!");

                // [MỚI]: Chờ 3 giây sau đó gỡ chế độ Bất tử
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.setInvulnerable(false);
                        player.sendMessage("§a§l[!] §aBảo vệ tân thủ đã kết thúc. Sẵn sàng chiến đấu!");
                    }
                }, 60L);
            });
        });
    }

    // ==========================================
    // TÍNH NĂNG ĐĂNG NHẬP
    // ==========================================
    public void login(Player player, String password) {
        UUID uuid = player.getUniqueId();

        if (isLoggedIn(player)) {
            player.sendMessage("§eBạn đã đăng nhập rồi!");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document account = collection.find(Filters.eq("_id", uuid.toString())).first();

            if (account == null) {
                player.sendMessage("§c§l[!] §cBạn chưa đăng ký tài khoản! Dùng lệnh /register <mật khẩu> <email@haha.com>");
                return;
            }

            String storedHash = account.getString("password");
            if (storedHash != null && storedHash.equals(hashPassword(password))) {
                // Tự động bổ sung các field VIP & Slot cho tài khoản cũ nếu chưa có
                migrateLegacyAccountFields(uuid, account);

                loggedInPlayers.put(uuid, true);
                player.sendMessage("§a§l[!] §aĐăng nhập thành công! Chúc bạn chơi game vui vẻ.");

                // [THÊM ĐOẠN NÀY]: Đưa về Main Thread để bơm túi đồ và Load dữ liệu
                Bukkit.getScheduler().runTask(plugin, () -> {
                    vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().loadPlayer(player);
                    vn.minecraft.rPGCore.RPGCore.getInstance().getBossBarManager().createBars(player, rpgPlayer);
                    vn.minecraft.rPGCore.RPGCore.getInstance().getVipManager().applyVipVisuals(player);
                    player.sendMessage("§e[Hệ Thống] §fDữ liệu nhân vật và túi đồ đã tải thành công!");

                    // [MỚI]: Chờ 3 giây sau đó gỡ chế độ Bất tử
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.setInvulnerable(false);
                            player.sendMessage("§a§l[!] §aBảo vệ đăng nhập đã kết thúc. Sẵn sàng chiến đấu!");
                        }
                    }, 60L);
                });

            } else {
                player.sendMessage("§c§l[!] §cSai mật khẩu! Nếu quên, hãy dùng lệnh /forgotpw <email>");
            }
        });
    }

    private void migrateLegacyAccountFields(UUID uuid, Document account) {
        List<Bson> missingUpdates = new ArrayList<>();
        if (!account.containsKey("is_vip")) {
            missingUpdates.add(Updates.set("is_vip", false));
        }
        if (!account.containsKey("vip_expires_at")) {
            missingUpdates.add(Updates.set("vip_expires_at", 0L));
        }
        if (!account.containsKey("max_slots")) {
            missingUpdates.add(Updates.set("max_slots", DEFAULT_MAX_SLOTS));
        }
        if (!account.containsKey("has_unlocked_vip_slot")) {
            missingUpdates.add(Updates.set("has_unlocked_vip_slot", false));
        }
        if (!missingUpdates.isEmpty()) {
            collection.updateOne(Filters.eq("_id", uuid.toString()), Updates.combine(missingUpdates));
        }
    }

    // ==========================================
    // TÍNH NĂNG QUÊN MẬT KHẨU
    // ==========================================
    public void forgotPassword(Player player, String email) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Document account = collection.find(Filters.eq("_id", uuid.toString())).first();

            if (account == null) {
                player.sendMessage("§cTài khoản này chưa được đăng ký!");
                return;
            }

            String registeredEmail = account.getString("email");
            if (!registeredEmail.equalsIgnoreCase(email)) {
                player.sendMessage("§c§l[!] §cEmail không khớp với dữ liệu đăng ký ban đầu!");
                return;
            }

            // Generate mật khẩu mới dài đúng 16 ký tự
            String tempPassword = generateRandomPassword();
            String newHashedPassword = hashPassword(tempPassword);

            // Cập nhật mật khẩu mới xuống Database
            collection.updateOne(Filters.eq("_id", uuid.toString()), new Document("$set", new Document("password", newHashedPassword)));

            // [MỚI]: Kích hoạt hàm gửi email thật
            sendRealEmail(registeredEmail, tempPassword);

            // Thông báo bảo mật cho người chơi
            player.sendMessage("§a§l[Hệ Thống] §fMật khẩu mới đã được gửi an toàn tới hòm thư §e" + registeredEmail + "§f của bạn.");
            player.sendMessage("§7Hãy kiểm tra Hộp thư đến (hoặc mục Spam) và dùng lệnh /login để tiếp tục.");
        });
    }

    // Xóa trạng thái đăng nhập khi thoát game
    public void logout(Player player) {
        loggedInPlayers.remove(player.getUniqueId());
    }

    public static long getSafeLong(Document doc, String key, long defaultValue) {
        if (doc == null) return defaultValue;
        Object val = doc.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return defaultValue;
    }

    public static int getSafeInt(Document doc, String key, int defaultValue) {
        if (doc == null) return defaultValue;
        Object val = doc.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return rawPassword;
        }
    }

    private void sendRealEmail(String recipient, String newPassword) {
        // Cấu hình tài khoản trạm phát của server
        final String senderEmail = "noreply@haha.com";
        final String senderPassword = "mat_khau_app_cua_ban";

        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        javax.mail.Session session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
            message.setFrom(new javax.mail.internet.InternetAddress(senderEmail, "RPG Server Admin"));
            message.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(recipient));
            message.setSubject("[RPG Server] Yêu cầu khôi phục mật khẩu");

            // Trang trí nội dung mail bằng HTML
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
                    + "<h3>Xin chào!</h3>"
                    + "<p>Hệ thống đã nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn.</p>"
                    + "<p>Đây là mật khẩu đăng nhập mới của bạn:</p>"
                    + "<h2 style='color: #2e6c80; background: #f4f4f4; padding: 10px; display: inline-block;'>" + newPassword + "</h2>"
                    + "<p>Vui lòng vào server, sử dụng lệnh <b>/login</b> để đăng nhập.</p>"
                    + "<br><p><i>Lưu ý: Nếu bạn không yêu cầu đổi mật khẩu, vui lòng bỏ qua email này.</i></p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            javax.mail.Transport.send(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Không thể gửi email tới: " + recipient);
            e.printStackTrace();
        }
    }
}