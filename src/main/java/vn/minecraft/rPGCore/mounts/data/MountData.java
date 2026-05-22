package vn.minecraft.rPGCore.mounts.data;

import vn.minecraft.rPGCore.mounts.type.MountArchetype;

public class MountData {

    private MountArchetype type;
    private String customName;

    // Cấp bậc hiện tại (Khởi đầu có thể được gacha ngẫu nhiên)
    private int tierSpeed = 1;
    private int tierJump = 1;
    private int tierDurability = 1;
    private int tierCooldown = 1;
    private int tierStorage = 1;

    // Chuỗi Base64 lưu Inventory (Để trống "" tức là túi đồ chưa có gì)
    private String inventoryBase64 = "";

    // Timestamp lúc chết (0 = Đang sống)
    private long deathTimestamp = 0;

    public MountData(MountArchetype type) {
        this.type = type;
        this.customName = type.getDisplayName();
    }

    // --- GETTERS / SETTERS ---
    public MountArchetype getType() { return type; }
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public int getTierSpeed() { return tierSpeed; }
    public void setTierSpeed(int tierSpeed) { this.tierSpeed = Math.min(tierSpeed, type.getMaxSpeed()); }

    public int getTierJump() { return tierJump; }
    public void setTierJump(int tierJump) { this.tierJump = Math.min(tierJump, type.getMaxJump()); }

    public int getTierDurability() { return tierDurability; }
    public void setTierDurability(int tierDurability) { this.tierDurability = Math.min(tierDurability, type.getMaxDurability()); }

    public int getTierCooldown() { return tierCooldown; }
    public void setTierCooldown(int tierCooldown) { this.tierCooldown = Math.min(tierCooldown, type.getMaxCooldown()); }

    public int getTierStorage() { return tierStorage; }
    public void setTierStorage(int tierStorage) { this.tierStorage = Math.min(tierStorage, type.getMaxStorage()); }

    public String getInventoryBase64() { return inventoryBase64; }
    public void setInventoryBase64(String inventoryBase64) { this.inventoryBase64 = inventoryBase64; }

    // --- LOGIC HỒI SINH KHI BỊ ĐÁNH CHẾT ---
    public void setDead() {
        this.deathTimestamp = System.currentTimeMillis();
    }

    public boolean isDead() {
        if (deathTimestamp == 0) return false;

        // Thời gian chờ gốc: 30 phút (1.800.000 ms). Mỗi bậc Cooldown giảm 2 phút (120.000 ms)
        long baseCooldownMs = 1800 * 1000L;
        long reductionMs = (tierCooldown - 1) * 120 * 1000L;
        long finalCooldownMs = Math.max(60000L, baseCooldownMs - reductionMs); // Ít nhất phải chờ 1 phút

        long timePassed = System.currentTimeMillis() - deathTimestamp;
        if (timePassed >= finalCooldownMs) {
            this.deathTimestamp = 0; // Đã hồi sinh xong
            return false;
        }
        return true;
    }

    public long getRemainingCooldownMs() {
        if (deathTimestamp == 0) return 0;
        long baseCooldownMs = 1800 * 1000L;
        long reductionMs = (tierCooldown - 1) * 120 * 1000L;
        long finalCooldownMs = Math.max(60000L, baseCooldownMs - reductionMs);

        long timePassed = System.currentTimeMillis() - deathTimestamp;
        return Math.max(0, finalCooldownMs - timePassed);
    }
}