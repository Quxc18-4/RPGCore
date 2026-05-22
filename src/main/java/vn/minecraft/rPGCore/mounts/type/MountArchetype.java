package vn.minecraft.rPGCore.mounts.type;

public enum MountArchetype {

    // Nhóm Phổ thông & Đa dụng
    HORSE("Ngựa Chiến", "HORSE", 8, 8, 8, 8, 8),
    MULE("Lừa Lai", "MULE", 6, 4, 10, 8, 12),
    DONKEY("Lừa Thồ Hàng", "DONKEY", 2, 1, 10, 12, 15),

    // Nhóm Đột Biến (Ngựa)
    SKELETON_HORSE("Ngựa Xương Địa Ngục", "SKELETON_HORSE", 16, 10, 4, 6, 4),
    ZOMBIE_HORSE("Ngựa Xác Sống", "ZOMBIE_HORSE", 4, 4, 16, 12, 4),

    // Nhóm Sa Mạc
    CAMEL("Lạc Đà Sa Mạc", "CAMEL", 10, 3, 9, 8, 10),
    CAMEL_HUSK("Lạc Đà Xác Sống", "CAMEL_HUSK", 6, 2, 14, 10, 8),

    // Nhóm Thủy Quái (Dưới nước)
    NAUTILUS("Ốc Biển Đại Dương", "NAUTILUS", 14, 4, 12, 2, 8),
    ZOMBIE_NAUTILUS("Ốc Biển Đột Biến", "ZOMBIE_NAUTILUS", 18, 2, 8, 4, 8),

    // Nhóm Custom API Lái tự do
    HAPPY_GHAST("Bóng Bay Vui Vẻ", "HAPPY_GHAST", 12, 10, 6, 10, 2),
    STRIDER("Quái Sải Bước", "STRIDER", 16, 1, 12, 9, 2),
    PIG("Heo Phong Thần", "PIG", 12, 6, 8, 10, 4);

    private final String displayName;
    private final String entityId;

    // Các mốc Max Tier Caps (Tổng = 40)
    private final int maxSpeed;
    private final int maxJump;
    private final int maxDurability;
    private final int maxCooldown;
    private final int maxStorage;

    MountArchetype(String displayName, String entityId, int maxSpeed, int maxJump, int maxDurability, int maxCooldown, int maxStorage) {
        this.displayName = displayName;
        this.entityId = entityId;
        this.maxSpeed = maxSpeed;
        this.maxJump = maxJump;
        this.maxDurability = maxDurability;
        this.maxCooldown = maxCooldown;
        this.maxStorage = maxStorage;
    }

    public String getDisplayName() { return displayName; }
    public String getEntityId() { return entityId; }
    public int getMaxSpeed() { return maxSpeed; }
    public int getMaxJump() { return maxJump; }
    public int getMaxDurability() { return maxDurability; }
    public int getMaxCooldown() { return maxCooldown; }
    public int getMaxStorage() { return maxStorage; }
}