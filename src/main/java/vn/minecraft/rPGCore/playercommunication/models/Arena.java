package vn.minecraft.rPGCore.playercommunication.models;

import org.bukkit.Location;

public class Arena {
    private String name;
    private Location spawn1; // Vị trí người thách đấu đứng
    private Location spawn2; // Vị trí người bị thách đấu đứng
    private boolean isOccupied; // Trạng thái: Đang có người đánh nhau không?

    public Arena(String name) {
        this.name = name;
        this.isOccupied = false;
    }

    public String getName() { return name; }

    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }

    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }

    // Kiểm tra xem map này đã được set đủ 2 tọa độ chưa
    public boolean isSetupComplete() {
        return spawn1 != null && spawn2 != null;
    }
}