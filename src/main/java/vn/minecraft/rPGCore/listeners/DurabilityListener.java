package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DurabilityListener implements Listener {

    private final PlayerManager playerManager;
    private final Random rand = new Random();

    public DurabilityListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    // Đổi thành EntityDamageByEntityEvent để ngã/cháy không bị mòn giáp
    @EventHandler
    public void onPlayerTakeDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        ItemStack[] armor = player.getInventory().getArmorContents();
        List<ItemStack> equippedArmor = new ArrayList<>();
        for (ItemStack item : armor) {
            if (item != null && item.getType() != Material.AIR) {
                equippedArmor.add(item);
            }
        }

        int count = equippedArmor.size();
        if (count == 0) return;

        int piecesToDamage = 0;
        int damageAmount = 0;
        double roll = rand.nextDouble() * 100;

        if (count == 1) {
            piecesToDamage = 1;
            damageAmount = rand.nextInt(3) + 2; // 2 -> 4
        } else if (count == 2) {
            piecesToDamage = (roll <= 75) ? 2 : 1;
            damageAmount = rand.nextInt(4) + 1; // 1 -> 4
        } else if (count == 3) {
            if (roll <= 50) piecesToDamage = 3;
            else if (roll <= 75) piecesToDamage = 2;
            else piecesToDamage = 1;
            damageAmount = rand.nextInt(3) + 1; // 1 -> 3
        } else if (count == 4) {
            if (roll <= 25) piecesToDamage = 4;
            else if (roll <= 50) piecesToDamage = 3;
            else if (roll <= 75) piecesToDamage = 2;
            else piecesToDamage = 1;
            damageAmount = rand.nextInt(2) + 1; // 1 -> 2
        }

        Collections.shuffle(equippedArmor);

        for (int i = 0; i < piecesToDamage; i++) {
            DurabilityManager.damageItem(player, rpgPlayer, equippedArmor.get(i), damageAmount);
        }
    }
}