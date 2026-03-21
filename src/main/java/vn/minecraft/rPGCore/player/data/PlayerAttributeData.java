package vn.minecraft.rPGCore.player.data;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;

public class PlayerAttributeData {
    private final RPGPlayer rpgPlayer; // Giữ reference đến RPGPlayer để gọi hàm update máu

    private int unspentAttrPoints = 0;
    private final Map<String, Integer> attributes = new HashMap<>();

    public PlayerAttributeData(RPGPlayer rpgPlayer) {
        this.rpgPlayer = rpgPlayer;
        initDefaultAttributes();
    }

    public void initDefaultAttributes() {
        if (attributes.isEmpty()) {
            attributes.put("STR", 0); attributes.put("INT", 0);
            attributes.put("VIT", 0); attributes.put("AGI", 0);
            attributes.put("LUK", 0);
        }
    }

    public int getUnspentAttrPoints() { return unspentAttrPoints; }
    public void setUnspentAttrPoints(int points) { this.unspentAttrPoints = points; }
    public void addUnspentAttrPoints(int points) { this.unspentAttrPoints += points; }

    public int getAttribute(String attr) { return attributes.getOrDefault(attr.toUpperCase(), 0); }
    public void setAttribute(String attr, int value) { attributes.put(attr.toUpperCase(), value); }

    public boolean upgradeAttribute(String attr) {
        String key = attr.toUpperCase();
        if (unspentAttrPoints > 0 && attributes.containsKey(key)) {
            attributes.put(key, attributes.get(key) + 1);
            unspentAttrPoints--;

            if (key.equals("VIT") || key.equals("INT")) {
                rpgPlayer.updateMaxStats(); // Gọi ngược lại RPGPlayer
            }
            return true;
        }
        return false;
    }

    public void resetAttributes() {
        int totalSpent = attributes.values().stream().mapToInt(Integer::intValue).sum();

        if (totalSpent > 0) {
            int refund = (int) (totalSpent * 0.8);
            for (String key : attributes.keySet()) attributes.put(key, 0);

            this.unspentAttrPoints += refund;
            rpgPlayer.updateMaxStats();

            Player p = rpgPlayer.getBukkitPlayer();
            p.sendMessage("§a§lTHÀNH CÔNG! §fĐã tẩy tủy chỉ số, hoàn trả §e" + refund + " điểm §f(80%).");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        } else {
            rpgPlayer.getBukkitPlayer().sendMessage("§cBạn chưa cộng điểm nào để có thể tẩy tủy!");
        }
    }
}