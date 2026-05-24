package vn.minecraft.rPGCore.mounts.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMountProfile {

    private UUID ownerUUID;
    // Mặc định người chơi mới luôn có 1 ô thú cưỡi miễn phí
    private int unlockedSlots = 1;

    // Lưu các thú cưỡi theo slot (Key = 1, 2, hoặc 3)
    private Map<Integer, MountData> mounts = new HashMap<>();

    public PlayerMountProfile(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public UUID getOwnerUUID() { return ownerUUID; }

    public int getUnlockedSlots() { return unlockedSlots; }
    public void setUnlockedSlots(int unlockedSlots) { this.unlockedSlots = Math.min(3, unlockedSlots); } // Tối đa 3 slot

    public Map<Integer, MountData> getMounts() { return mounts; }

    public MountData getMountAt(int slot) {
        return mounts.get(slot);
    }

    public void setMountAt(int slot, MountData mountData) {
        // Nếu null -> xóa mount khỏi slot
        if (mountData == null) {
            mounts.remove(slot);
            return;
        }
        mounts.put(slot, mountData);
    }

    public boolean hasMountAt(int slot) {
        return mounts.get(slot) != null;
    }
}