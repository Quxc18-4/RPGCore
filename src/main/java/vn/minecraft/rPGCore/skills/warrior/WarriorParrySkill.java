package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorParrySkill implements ISkill {

    @Override public String getId() { return "warrior_parry"; }
    @Override public String getName() { return "§6§lChặn Đòn"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.SHIELD; }

    @Override public int getMaxLevel() { return 1; }
    @Override public int getUpgradeCost(int currentLevel) { return 0; } // 0 SP để nâng cấp
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 0; }
    @Override public long getCooldown(int level) { return 7000L; } // Cooldown 7 giây

    @Override public boolean isPassiveSkill() { return true; }


    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Kỹ năng phòng ngự đỉnh cao của Chiến Binh.",
                "§7Tôn vinh sự phản xạ và kỹ năng cá nhân.",
                "",
                "§e[Kích hoạt: Chặn đòn]",
                "§f- Đưa bản thân vào trạng thái Đỡ Đòn trong §a0.75s§f.",
                "§f- Nếu bị tấn công trong thời gian này:",
                "§f  + Giảm §c50% Sát thương§f nhận vào.",
                "§f  + Lập tức hồi §c15 Nộ Khí§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Đếm Cooldown bằng Dấu thời gian (Timestamp) để dễ dàng bẻ cong thời gian
        if (player.hasMetadata("Warrior_Parry_CD_Time")) {
            long cdEnd = player.getMetadata("Warrior_Parry_CD_Time").get(0).asLong();
            if (System.currentTimeMillis() < cdEnd) {
                return false; // Vẫn đang hồi chiêu
            }
        }

        // Kích hoạt Khung giờ Parry 0.75s
        player.setMetadata("Warrior_Parry_Window", new FixedMetadataValue(RPGCore.getInstance(), true));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("Warrior_Parry_Window")) {
                    player.removeMetadata("Warrior_Parry_Window", RPGCore.getInstance());
                }
            }
        }.runTaskLater(RPGCore.getInstance(), 15L);

        // Đặt Cooldown
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata("Warrior_Parry_CD_Time", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));

        // Task theo dõi linh hoạt: Nếu CD bị giảm 50%, nó sẽ báo xong sớm hơn!
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { this.cancel(); return; }

                if (player.hasMetadata("Warrior_Parry_CD_Time")) {
                    long end = player.getMetadata("Warrior_Parry_CD_Time").get(0).asLong();
                    if (System.currentTimeMillis() >= end) {
                        player.sendMessage("§a§l[!] §fKỹ năng §e" + getName() + " §fđã sẵn sàng!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                        player.removeMetadata("Warrior_Parry_CD_Time", RPGCore.getInstance());
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 10L, 5L); // Quét 0.25s một lần

        return true;
    }
}