package vn.minecraft.rPGCore.playercommunication.models;

import org.bson.Document;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TradeRecord {
    public String tradeId;
    public long timestamp;
    public String player1Id;
    public String player2Id;
    public String items1Base64;
    public String items2Base64;

    public TradeRecord(TradeSession session) {
        this.tradeId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.player1Id = session.getPlayer1Id().toString();
        this.player2Id = session.getPlayer2Id().toString();

        // Quét trực tiếp các Item (bao gồm cả Tiền Xu vật lý) đang nằm trên bàn giao dịch
        List<ItemStack> items1 = getItemsFromSlots(session.getInventory(), TradeSession.P1_SLOTS);
        List<ItemStack> items2 = getItemsFromSlots(session.getInventory(), TradeSession.P2_SLOTS);

        this.items1Base64 = itemsToBase64(items1);
        this.items2Base64 = itemsToBase64(items2);
    }

    // Hàm tiện ích bóc tách Item từ các Slot quy định
    private List<ItemStack> getItemsFromSlots(Inventory inv, Set<Integer> slots) {
        List<ItemStack> list = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                list.add(item);
            }
        }
        return list;
    }

    public Document toDocument() {
        return new Document("trade_id", tradeId)
                .append("timestamp", timestamp)
                .append("player1", player1Id)
                .append("player2", player2Id)
                .append("items1_base64", items1Base64)
                .append("items2_base64", items2Base64);
    }

    private String itemsToBase64(List<ItemStack> items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.size());
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
}