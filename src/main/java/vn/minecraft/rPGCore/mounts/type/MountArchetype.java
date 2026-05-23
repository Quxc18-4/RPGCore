package vn.minecraft.rPGCore.mounts.type;

public enum MountArchetype {

    // Nhóm Phổ thông & Đa dụng
    HORSE("Ngựa Chiến", "HORSE", 20480, true, 8, 8, 8, 8, 8), // 5 Vàng
    MULE("Lừa La", "MULE", 16384, true, 6, 4, 10, 8, 12), // 4 Vàng
    DONKEY("Lừa Thồ Hàng", "DONKEY", 24576, true, 2, 1, 10, 12, 15), // 6 Vàng

    // Nhóm Đột Biến (Ngựa) - isBuyable = false
    SKELETON_HORSE("Ngựa Xương Địa Ngục", "SKELETON_HORSE", 0, false, 16, 10, 4, 6, 4),
    ZOMBIE_HORSE("Ngựa Xác Sống", "ZOMBIE_HORSE", 0, false, 4, 4, 16, 12, 4),

    // Nhóm Sa Mạc
    CAMEL("Lạc Đà Sa Mạc", "CAMEL", 20480, true, 10, 3, 9, 8, 10), // 5 Vàng
    CAMEL_HUSK("Lạc Đà Xác Sống", "CAMEL_HUSK", 0, false, 6, 2, 14, 10, 8),

    // Nhóm Thủy Quái (Dưới nước)
    NAUTILUS("Ốc Biển Đại Dương", "NAUTILUS", 28672, true, 14, 4, 12, 2, 8), // 7 Vàng
    ZOMBIE_NAUTILUS("Ốc Biển Đột Biến", "ZOMBIE_NAUTILUS", 0, false, 18, 2, 8, 4, 8),

    // Nhóm Custom API Lái tự do
    HAPPY_GHAST("Bóng Bay Vui Vẻ", "HAPPY_GHAST", 32768, true, 12, 10, 6, 10, 2); // 8 Vàng
    //STRIDER("Quái Sải Bước", "STRIDER", 24576, true, 16, 1, 12, 9, 2), // 6 Vàng
    //PIG("Heo Phong Thần", "PIG", 16384, true, 12, 6, 8, 10, 4); // 2 Vàng (Thú vui)

    private final String displayName;
    private final String entityId;
    private final long priceCopper; // Giá mua bằng Đồng
    private final boolean isBuyable; // Có hiện trên Shop không?

    private final int maxSpeed;
    private final int maxJump;
    private final int maxDurability;
    private final int maxCooldown;
    private final int maxStorage;

    MountArchetype(String displayName, String entityId, long priceCopper, boolean isBuyable, int maxSpeed, int maxJump, int maxDurability, int maxCooldown, int maxStorage) {
        this.displayName = displayName;
        this.entityId = entityId;
        this.priceCopper = priceCopper;
        this.isBuyable = isBuyable;
        this.maxSpeed = maxSpeed;
        this.maxJump = maxJump;
        this.maxDurability = maxDurability;
        this.maxCooldown = maxCooldown;
        this.maxStorage = maxStorage;
    }

    public String getDisplayName() { return displayName; }
    public String getEntityId() { return entityId; }
    public long getPriceCopper() { return priceCopper; }
    public boolean isBuyable() { return isBuyable; }
    public int getMaxSpeed() { return maxSpeed; }
    public int getMaxJump() { return maxJump; }
    public int getMaxDurability() { return maxDurability; }
    public int getMaxCooldown() { return maxCooldown; }
    public int getMaxStorage() { return maxStorage; }
}