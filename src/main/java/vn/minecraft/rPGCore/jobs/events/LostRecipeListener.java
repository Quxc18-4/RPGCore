package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.brewing.BrewingEngine;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.managers.ItemManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LostRecipeListener implements Listener {

    private final ItemManager itemManager;

    public LostRecipeListener() {
        this.itemManager = RPGCore.getInstance().getItemManager();
    }

    @EventHandler
    public void onReadBook(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        if ("alch_lost_recipe".equals(itemManager.getItemId(item))) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            PlayerJobData data = JobManager.getPlayerData(player);

            if (!data.getJobId().equalsIgnoreCase("ALCHEMIST")) {
                player.sendMessage("§c§l[!] §cChỉ Dược Sư mới có thể đọc hiểu văn tự cổ đại này!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Tìm các công thức người chơi CHƯA HỌC
            List<BrewingEngine.SecretRecipe> unlearned = new ArrayList<>();
            for (BrewingEngine.SecretRecipe recipe : BrewingEngine.SECRET_RECIPES) {
                if (!data.hasUnlockedSkill("ALCH_RECIPE", recipe.recipeId)) {
                    unlearned.add(recipe);
                }
            }

            if (unlearned.isEmpty()) {
                player.sendMessage("§c§l[!] §cBạn đã học được mọi bí thuật thất truyền trên thế gian rồi!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Quay ngẫu nhiên 1 công thức
            BrewingEngine.SecretRecipe learnedRecipe = unlearned.get(new Random().nextInt(unlearned.size()));

            // Mở khóa công thức (Sử dụng hệ thống "unlockedSkills" có sẵn của bạn)
            data.unlockSkill("ALCH_RECIPE", learnedRecipe.recipeId);

            // Trừ sách
            item.setAmount(item.getAmount() - 1);

            // Bắn pháo bông & Hiệu ứng
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendMessage("");
            player.sendMessage("§d§lKIẾN THỨC BÙNG NỔ! §fBạn vừa đọc hiểu cuốn sách cổ.");
            player.sendMessage("§fMở khóa thành công công thức luyện: " + learnedRecipe.potionName);
            player.sendMessage("§8▪ Nguyên liệu yêu cầu: §7" + String.join(", ", learnedRecipe.requiredItems));
            player.sendMessage("");
        }
    }
}