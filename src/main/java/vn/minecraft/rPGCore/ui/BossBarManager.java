package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    private final Map<UUID, BossBar> healthBars = new HashMap<>();
    private final Map<UUID, BossBar> resourceBars = new HashMap<>();

    public void createBars(Player player, RPGPlayer rpgPlayer) {
        UUID uuid = player.getUniqueId();

        BossBar healthBar = Bukkit.createBossBar(
                formatHealthTitle(rpgPlayer),
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        healthBar.addPlayer(player);
        healthBars.put(uuid, healthBar);

        ResourceType resType = rpgPlayer.getCurrentClass().getResourceType();
        BossBar resourceBar = Bukkit.createBossBar(
                formatResourceTitle(rpgPlayer),
                resType.getColor(),
                BarStyle.SEGMENTED_10
        );
        resourceBar.addPlayer(player);
        resourceBars.put(uuid, resourceBar);

        updateBars(player, rpgPlayer);
    }

    public void updateBars(Player player, RPGPlayer rpgPlayer) {
        UUID uuid = player.getUniqueId();
        if (!healthBars.containsKey(uuid) || !resourceBars.containsKey(uuid)) return;

        BossBar healthBar = healthBars.get(uuid);
        BossBar resourceBar = resourceBars.get(uuid);

        healthBar.setTitle(formatHealthTitle(rpgPlayer));
        // Đã đổi từ getBaseMaxHealth() sang getMaxHealth()
        double hpProgress = rpgPlayer.getCurrentHealth() / rpgPlayer.getTotalMaxHealth();
        healthBar.setProgress(Math.max(0.0, Math.min(1.0, hpProgress)));

        resourceBar.setTitle(formatResourceTitle(rpgPlayer));
        resourceBar.setColor(rpgPlayer.getCurrentClass().getResourceType().getColor());
        // Đã đổi từ getBaseMaxResource() sang getMaxResource()
        double resProgress = rpgPlayer.getCurrentResource() / rpgPlayer.getCurrentClass().getMaxResource();
        resourceBar.setProgress(Math.max(0.0, Math.min(1.0, resProgress)));
    }

    public void removeBars(Player player) {
        UUID uuid = player.getUniqueId();
        if (healthBars.containsKey(uuid)) {
            healthBars.get(uuid).removeAll();
            healthBars.remove(uuid);
        }
        if (resourceBars.containsKey(uuid)) {
            resourceBars.get(uuid).removeAll();
            resourceBars.remove(uuid);
        }
    }

    private String formatHealthTitle(RPGPlayer p) {
        // ĐỔI: p.getCurrentClass().getMaxHealth() -> p.getTotalMaxHealth()
        return "§c❤ HP: " + (int)p.getCurrentHealth() + " / " + (int)p.getTotalMaxHealth();
    }

    private String formatResourceTitle(RPGPlayer p) {
        ResourceType type = p.getCurrentClass().getResourceType();
        String colorCode = (type == ResourceType.MANA) ? "§b" : "§e";
        String icon = (type == ResourceType.MANA) ? "✧" : "⚡";

        // Đã đổi từ getBaseMaxResource() sang getMaxResource()
        return colorCode + icon + " " + type.getName() + ": "
                + (int)p.getCurrentResource() + " / " + (int)p.getCurrentClass().getMaxResource();
    }
}