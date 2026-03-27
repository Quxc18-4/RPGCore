package vn.minecraft.rPGCore.skills;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

public abstract class AbstractHitCounterSkill implements ISkill {

    protected final RPGCore plugin = RPGCore.getInstance();

    // Các lớp con tự định nghĩa số đòn yêu cầu và thời gian đứt chuỗi
    protected abstract int getRequiredHits();
    protected abstract long getComboTimeout();

    // Logic nổ ra khi đủ điểm Combo
    protected abstract void onMaxStacksReached(Player player, RPGPlayer rpgPlayer, LivingEntity lastTarget, int skillLevel);

    public void addStack(Player player, RPGPlayer rpgPlayer, LivingEntity target, int skillLevel) {
        long now = System.currentTimeMillis();
        String timeKey = getId() + "_Combo_Time";
        String countKey = getId() + "_Combo_Count";

        long lastHit = player.hasMetadata(timeKey) ? player.getMetadata(timeKey).get(0).asLong() : 0;
        int combo = player.hasMetadata(countKey) ? player.getMetadata(countKey).get(0).asInt() : 0;

        // Quá thời gian -> Reset
        if (now - lastHit > getComboTimeout()) {
            combo = 0;
        }

        combo++;
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f + (combo * 0.2f));

        // Đạt mốc -> Nổ chiêu và Reset
        if (combo >= getRequiredHits()) {
            combo = 0;
            onMaxStacksReached(player, rpgPlayer, target, skillLevel);
        }

        player.setMetadata(countKey, new FixedMetadataValue(plugin, combo));
        player.setMetadata(timeKey, new FixedMetadataValue(plugin, now));
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        player.sendMessage("§c§l[!] §fKỹ năng này là Nội Tại, tự động kích hoạt khi đánh thường!");
        return false;
    }
}