package vn.minecraft.rPGCore.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CustomGUI implements InventoryHolder {
    private final String guiId;
    private final int data; // Dùng để lưu Số trang hoặc Ngày

    public CustomGUI(String guiId, int data) {
        this.guiId = guiId;
        this.data = data;
    }

    public String getGuiId() { return guiId; }
    public int getData() { return data; }

    @Override
    public Inventory getInventory() {
        return null;
    }
}