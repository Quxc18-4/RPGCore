package vn.minecraft.rPGCore.player;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.RPGClass;

import java.util.HashMap;
import java.util.Map;

public class RPGPlayer {
    // HỆ THỐNG LEVEL
    private final Player bukkitPlayer;
    private RPGClass currentClass;
    private double currentResource;
    private double currentExp = 0; // Biến này sẽ lưu EXP lẻ
    private long lastCombatTime = 0;

    // HỆ THỐNG ATTRIBUTE
    private int unspentAttrPoints = 0; // Điểm tiềm năng chưa dùng
    private final java.util.Map<String, Integer> attributes = new java.util.HashMap<>();

    // HỆ THỐNG KỸ NĂNG & COMBO
    private int skillPoints = 0;
    // Map lưu cấp độ kỹ năng: Key = Tên kỹ năng (VD: "archer_spread"), Value = Cấp độ (1, 2, 3...)
    private final Map<String, Integer> skills = new HashMap<>();
    // Map lưu Combo: Key = Tổ hợp phím (VD: "LLR"), Value = Tên kỹ năng
    private final Map<String, String> binds = new HashMap<>();

    public RPGPlayer(Player player, RPGClass defaultClass) {
        this.bukkitPlayer = player;
        this.currentClass = defaultClass;
        this.currentResource = defaultClass.getMaxResource();
        initDefaultAttributes();
    }

    // --- CÁC HÀM GETTER ---
    public RPGClass getCurrentClass() { return currentClass; }
    public Player getBukkitPlayer() { return bukkitPlayer; }
    public double getCurrentResource() { return currentResource; }
    public double getCurrentHealth() { return this.bukkitPlayer.getHealth(); }
    public double getCurrentExp() { return currentExp; }

    // --- GIAO TRANH ---
    public void setInCombat() { this.lastCombatTime = System.currentTimeMillis(); }
    public boolean isInCombat() { return (System.currentTimeMillis() - this.lastCombatTime) <= 10000; }

    // --- HỆ THỐNG EXP VÀ LEVEL ---

    /**
     * Công thức bạn yêu cầu: Base 500 + (Lvl * 100) + (Lvl^2)^2
     * Phân tích độ ảnh hưởng:
     * - Lv 0: 500 (Base chiếm 100%)
     * - Lv 10: 500 + 1000 + 10,000 = 11,500 (L^4 bắt đầu chiếm ưu thế)
     * - Lv 20: 500 + 2000 + 160,000 = 162,500 (Bức tường thực sự)
     */
    public double getRequiredExp() {
        int level = this.bukkitPlayer.getLevel();
        return 500 + (level * 100) + Math.pow(level, 3) * level/4;
    }

    public void addExp(double amount) {
        this.currentExp += amount;

        // Xử lý lên cấp
        while (this.currentExp >= getRequiredExp()) {
            this.currentExp -= getRequiredExp();
            int newLevel = this.bukkitPlayer.getLevel() + 1;
            this.bukkitPlayer.setLevel(newLevel);
            this.unspentAttrPoints += 3;
            this.skillPoints += 1;

            // Cập nhật máu tối đa ngay khi lên cấp
            updateMaxStats();

            this.bukkitPlayer.sendMessage("§b§lLEVEL UP! §fBạn đã đạt cấp độ §e" + newLevel);
            this.bukkitPlayer.playSound(this.bukkitPlayer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        updateExpBar();
    }

    public void updateExpBar() {
        float progress = (float) (this.currentExp / getRequiredExp());
        this.bukkitPlayer.setExp(Math.min(0.999f, progress));
    }

    // --- HỒI PHỤC VÀ CHỈ SỐ ---

    public void heal(double amount) {
        if (!this.bukkitPlayer.isDead()) {
            // Lấy giới hạn máu THỰC TẾ mà server đang gán cho người chơi này
            AttributeInstance maxAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr == null) return;

            double actualMax = maxAttr.getValue(); // Đây là con số 1600.0 trong lỗi của bạn
            double currentHp = this.bukkitPlayer.getHealth();

            // Đảm bảo máu mới không bao giờ vượt quá giới hạn thuộc tính
            double newHp = Math.min(currentHp + amount, actualMax);

            this.bukkitPlayer.setHealth(newHp);
        }
    }

    public void setClass(RPGClass newClass) {
        this.currentClass = newClass;
        this.currentResource = getTotalMaxResource();
        updateMaxStats();
        this.bukkitPlayer.setHealth(getTotalMaxHealth());
    }

    public void updateMaxStats() {
        double totalMaxHp = getTotalMaxHealth();
        // Dùng GENERIC_MAX_HEALTH cho bản 1.21.1
        AttributeInstance maxHpAttr = this.bukkitPlayer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.setBaseValue(totalMaxHp);
        }
        this.bukkitPlayer.setHealthScale(20.0);
        this.bukkitPlayer.setHealthScaled(true);
    }

    public void setCurrentResource(double currentResource) {
        double maxResource = getTotalMaxResource();
        this.currentResource = Math.max(0, Math.min(currentResource, maxResource));
    }

    public double getTotalMaxHealth() {
        double baseHp = this.currentClass.getMaxHealth();
        double levelBonus = this.bukkitPlayer.getLevel() * 50.0;

        // Lấy điểm Thể lực (VIT) hiện có, mỗi điểm cộng thêm 20 Máu tối đa
        double vitBonus = getAttribute("VIT") * 15.0;

        return baseHp + levelBonus + vitBonus;
    }

    public double getTotalMaxResource() {
        // Lấy Mana gốc của Class
        double baseRes = this.currentClass.getMaxResource();

        // Tính thêm Mana từ điểm INT (1 điểm INT = 2 Mana)
        double intBonus = getAttribute("INT") * 2.0;

        // Trả về tổng cộng
        return baseRes + intBonus;
    }

    /**
     * Xử lý hình phạt mất EXP khi chết.
     * @param percentage Tỷ lệ mất (ví dụ 0.3 cho 30%)
     */
    public void loseDeathExp(double percentage) {
        if (this.currentExp <= 0) return;

        // Tính toán lượng EXP bị mất dựa trên tiến độ hiện tại (90k * 0.3)
        double lostAmount = this.currentExp * percentage;
        this.currentExp -= lostAmount;

        // Đảm bảo EXP không bị âm (đề phòng lỗi tính toán)
        if (this.currentExp < 0) this.currentExp = 0;

        // Cập nhật lại thanh EXP xanh lá của Minecraft
        updateExpBar();

        // Thông báo cho người chơi
        this.bukkitPlayer.sendMessage("§c§l☠ BẠN ĐÃ TỬ VONG!");
        this.bukkitPlayer.sendMessage("§cHình phạt cấp độ > 20: Mất §e" + (int)lostAmount + " EXP §c(30% tiến độ).");
    }

    // Khởi tạo các chỉ số cơ bản
    public void initDefaultAttributes() {
        if (attributes.isEmpty()) {
            attributes.put("STR", 0); // Sức mạnh
            attributes.put("INT", 0); // Trí tuệ
            attributes.put("VIT", 0); // Thể lực
            attributes.put("AGI", 0); // Nhanh nhẹn
            attributes.put("LUK", 0); // May mắn
        }
    }

    public int getUnspentAttrPoints() { return unspentAttrPoints; }
    public void setUnspentAttrPoints(int points) { this.unspentAttrPoints = points; }
    public void addUnspentAttrPoints(int points) { this.unspentAttrPoints += points; }

    public int getAttribute(String attr) {
        return attributes.getOrDefault(attr.toUpperCase(), 0);
    }

    public void setAttribute(String attr, int value) {
        attributes.put(attr.toUpperCase(), value);
    }

    // Hàm thực hiện việc cộng điểm từ UI/Command
    public boolean upgradeAttribute(String attr) {
        String key = attr.toUpperCase();
        if (unspentAttrPoints > 0 && attributes.containsKey(key)) {
            attributes.put(key, attributes.get(key) + 1);
            unspentAttrPoints--;

            // Nếu cộng VIT hoặc INT, phải cập nhật lại Max HP / Max Mana ngay lập tức
            if (key.equals("VIT") || key.equals("INT")) {
                updateMaxStats();
            }
            return true;
        }
        return false;
    }

    // Thêm hàm Reset vào RPGPlayer.java
    public void resetAttributes() {
        int totalSpent = 0;

        // Tính tổng số điểm đã cộng
        for (int value : attributes.values()) {
            totalSpent += value;
        }

        if (totalSpent > 0) {
            // Hoàn trả 80% (làm tròn xuống)
            int refund = (int) (totalSpent * 0.8);

            // Đưa tất cả về 0
            for (String key : attributes.keySet()) {
                attributes.put(key, 0);
            }

            // Cộng điểm hoàn trả vào điểm tiềm năng
            this.unspentAttrPoints += refund;

            // Bắt buộc phải cập nhật lại Máu và Mana ngay lập tức
            updateMaxStats();

            this.bukkitPlayer.sendMessage("§a§lTHÀNH CÔNG! §fĐã tẩy tủy chỉ số, hoàn trả §e" + refund + " điểm §f(80%).");
            this.bukkitPlayer.playSound(this.bukkitPlayer.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        } else {
            this.bukkitPlayer.sendMessage("§cBạn chưa cộng điểm nào để có thể tẩy tủy!");
        }
    }

    // Thêm hàm này để nạp EXP từ file save
    public void setCurrentExp(double exp) {
        this.currentExp = exp;
    }

    // --- GETTER / SETTER KỸ NĂNG ---
    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int sp) { this.skillPoints = sp; }

    public int getSkillLevel(String skillId) { return skills.getOrDefault(skillId, 0); }
    public void setSkillLevel(String skillId, int level) { skills.put(skillId, level); }

    public String getSkillByCombo(String combo) { return binds.get(combo); }
    public void bindCombo(String combo, String skillId) { binds.put(combo, skillId); }
    public void unbindCombo(String combo) { binds.remove(combo); }
    public Map<String, String> getBinds() { return binds; }
}