package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CookingManager implements Listener {

    private final JavaPlugin plugin;
    // Đã thêm biến ingredientManager vào đây
    private final IngredientManager ingredientManager;

    private final Map<Location, FinishedPot> activePots = new HashMap<>();
    private final Set<Location> cookingPots = new HashSet<>();

    // Đã cập nhật Constructor để nhận IngredientManager từ RPGCore
    public CookingManager(JavaPlugin plugin, IngredientManager ingredientManager) {
        this.plugin = plugin;
        this.ingredientManager = ingredientManager;
        startItemScanner();
    }

    private static class FinishedPot {
        ItemStack resultItem;
        int scoopsLeft;
        Color particleColor;

        public FinishedPot(ItemStack resultItem, int scoopsLeft, Color particleColor) {
            this.resultItem = resultItem;
            this.scoopsLeft = scoopsLeft;
            this.particleColor = particleColor;
        }
    }

    private void startItemScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<Location, FinishedPot>> activeIt = activePots.entrySet().iterator();
                while (activeIt.hasNext()) {
                    Map.Entry<Location, FinishedPot> entry = activeIt.next();
                    if (entry.getKey().getBlock().getType() != Material.WATER_CAULDRON) {
                        activeIt.remove();
                    }
                }
                cookingPots.removeIf(loc -> loc.getBlock().getType() != Material.WATER_CAULDRON);

                Bukkit.getWorlds().forEach(world -> {
                    for (Item item : world.getEntitiesByClass(Item.class)) {
                        if (item.isDead()) continue;

                        Location loc = item.getLocation();
                        Block currentBlock = loc.getBlock();
                        Block blockBelow = loc.clone().subtract(0, 0.5, 0).getBlock();

                        Block cauldronBlock = null;
                        if (currentBlock.getType() == Material.WATER_CAULDRON) cauldronBlock = currentBlock;
                        else if (blockBelow.getType() == Material.WATER_CAULDRON) cauldronBlock = blockBelow;

                        if (cauldronBlock != null) {
                            Block campfireBlock = cauldronBlock.getRelative(BlockFace.DOWN);
                            if (isHeatSource(campfireBlock.getType())) {
                                if (!cookingPots.contains(cauldronBlock.getLocation()) && !activePots.containsKey(cauldronBlock.getLocation())) {
                                    startCookingProcess(cauldronBlock);
                                }
                            }
                        }
                    }
                });

                for (Location loc : activePots.keySet()) {
                    Color c = activePots.get(loc).particleColor;
                    Particle.DustOptions dust = new Particle.DustOptions(c, 1.2f);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.5, 0.9, 0.5), 3, 0.2, 0.0, 0.2, dust);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private boolean isHeatSource(Material mat) {
        return mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE ||
                mat == Material.FIRE || mat == Material.SOUL_FIRE || mat == Material.MAGMA_BLOCK;
    }

    private void startCookingProcess(Block cauldronBlock) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.add(loc);

        cauldronBlock.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;
            final List<Item> ingredients = new ArrayList<>();

            @Override
            public void run() {
                if (cauldronBlock.getType() != Material.WATER_CAULDRON) {
                    cookingPots.remove(loc);
                    this.cancel();
                    return;
                }

                // Quét nguyên liệu (Chỉ nhận đồ RPG có ID)
                for (Item item : loc.getWorld().getEntitiesByClass(Item.class)) {
                    if (item.isDead()) continue;
                    Block iBlock = item.getLocation().getBlock();
                    Block iBlockBelow = item.getLocation().clone().subtract(0, 0.5, 0).getBlock();

                    if ((iBlock.equals(cauldronBlock) || iBlockBelow.equals(cauldronBlock)) && !ingredients.contains(item)) {

                        // Kiểm tra xem item có ID RPG không
                        String ingredientId = ingredientManager.getIngredientId(item.getItemStack());
                        if (ingredientId != null) {
                            // Giới hạn 9 nguyên liệu
                            if (ingredients.size() < 9) {
                                ingredients.add(item);
                                item.setPickupDelay(9999);
                            } else {
                                item.setVelocity(new org.bukkit.util.Vector(0, 0.5, 0));
                            }
                        }
                    }
                }

                double centerOfCauldronX = loc.getX() + 0.5;
                double centerOfCauldronZ = loc.getZ() + 0.5;
                double waterY = loc.getY() + 0.65;

                for (int i = 0; i < ingredients.size(); i++) {
                    Item item = ingredients.get(i);
                    if (item.isDead()) continue;

                    double bobbing = Math.sin(ticks * 0.3 + i) * 0.15;
                    double offsetX = Math.cos(ticks * 0.1 + i) * 0.2;
                    double offsetZ = Math.sin(ticks * 0.15 + i) * 0.2;

                    item.teleport(new Location(loc.getWorld(), centerOfCauldronX + offsetX, waterY + bobbing, centerOfCauldronZ + offsetZ));
                }

                if (ticks % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.BUBBLE_POP, centerOfCauldronX, waterY + 0.2, centerOfCauldronZ, 3, 0.2, 0.1, 0.2, 0.05);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, centerOfCauldronX, waterY + 0.3, centerOfCauldronZ, 1, 0.1, 0.1, 0.1, 0.01);
                }

                if (ticks >= 100) {
                    finishCooking(cauldronBlock, ingredients);
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishCooking(Block cauldronBlock, List<Item> ingredients) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.remove(loc);

        // Lấy danh sách ID của các nguyên liệu
        List<String> recipeIds = new ArrayList<>();
        for (Item item : ingredients) {
            String id = ingredientManager.getIngredientId(item.getItemStack());
            if (id != null) recipeIds.add(id);
            item.remove();
        }
        Collections.sort(recipeIds);

        // Tạm thời xuất ra món ăn thất bại (Sẽ code hệ thống Điểm ở Phase 2)
        ItemStack result = getSuspiciousStew();
        Color pColor = Color.fromRGB(80, 100, 80);

        // Công thức cứng (Tạm thời)
        if (recipeIds.contains("meat_beef") && recipeIds.contains("veg_carrot") && recipeIds.contains("veg_mushroom")) {
            result = getBeefStew();
            pColor = Color.fromRGB(200, 50, 0);
        }

        cauldronBlock.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0.5, 0.8, 0.5), 15, 0.2, 0.2, 0.2, 0.1);

        activePots.put(loc, new FinishedPot(result, 6, pColor));
    }

    @EventHandler
    public void onScoopSoup(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem.getType() != Material.BOWL) return;

        Location loc = block.getLocation();
        if (activePots.containsKey(loc)) {
            event.setCancelled(true);

            FinishedPot pot = activePots.get(loc);

            handItem.setAmount(handItem.getAmount() - 1);
            player.getInventory().addItem(pot.resultItem.clone());
            player.playSound(loc, Sound.ENTITY_GENERIC_DRINK, 1f, 1f);

            pot.scoopsLeft--;

            if (pot.scoopsLeft <= 0) {
                activePots.remove(loc);
                block.setType(Material.CAULDRON);
                player.sendMessage("§eNồi súp đã cạn sạch!");
            } else {
                Levelled cauldronData = (Levelled) block.getBlockData();
                if (pot.scoopsLeft <= 4 && cauldronData.getLevel() == 3) cauldronData.setLevel(2);
                if (pot.scoopsLeft <= 2 && cauldronData.getLevel() == 2) cauldronData.setLevel(1);
                block.setBlockData(cauldronData);
            }
        }
    }

    private ItemStack getSuspiciousStew() {
        ItemStack item = new ItemStack(Material.MUSHROOM_STEW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§8Món Ăn Kì Lạ");
        meta.setLore(Arrays.asList("§7Nấu sai công thức...", "§cĂn vào có thể bị đau bụng."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getBeefStew() {
        ItemStack item = new ItemStack(Material.RABBIT_STEW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lBò Hầm Nấm");
        meta.setLore(Arrays.asList("§7Độ no: §a15 phút", "§7Hiệu ứng: §c+20% Sát thương vật lý"));
        item.setItemMeta(meta);
        return item;
    }
}