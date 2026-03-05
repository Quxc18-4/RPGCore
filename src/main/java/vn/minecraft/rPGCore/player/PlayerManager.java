package vn.minecraft.rPGCore.player;

import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerManager {
    // Lưu trữ danh sách người chơi đang online: <UUID, Dữ liệu RPG của người đó>
    private final Map<UUID, RPGPlayer> activePlayers = new HashMap<>();

    public void addPlayer(UUID uuid, RPGPlayer player) {
        activePlayers.put(uuid, player);
    }

    public void removePlayer(UUID uuid) {
        activePlayers.remove(uuid);
    }

    public RPGPlayer getRPGPlayer(UUID uuid) {
        return activePlayers.get(uuid);
    }

    // Phương thức lấy toàn bộ UUID của người chơi đang online
    public Set<UUID> getAllPlayerUUIDs() {
        return activePlayers.keySet();
    }
}