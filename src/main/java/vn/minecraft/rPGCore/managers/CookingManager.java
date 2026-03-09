package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CookingManager implements Listener {

    private final JavaPlugin plugin;
    private final IngredientManager ingredientManager;
    private final Map<Location, FinishedPot> activePots = new HashMap<>();
    private final Set<Location> cookingPots = new HashSet<>();
    private final NamespacedKey COOKED_TIME_KEY;
    private final NamespacedKey MAX_FRESHNESS_KEY; // Thêm dòng này

    public CookingManager(JavaPlugin plugin, IngredientManager ingredientManager) {
        this.plugin = plugin;
        this.ingredientManager = ingredientManager;
        this.COOKED_TIME_KEY = new NamespacedKey(plugin, "cooked_time");
        this.MAX_FRESHNESS_KEY = new NamespacedKey(plugin, "max_freshness"); // Thêm dòng này
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
                        if (ingredientManager.getIngredientId(item.getItemStack()) == null) continue;

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
        return mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE || mat == Material.FIRE || mat == Material.SOUL_FIRE || mat == Material.MAGMA_BLOCK;
    }

    private void startCookingProcess(Block cauldronBlock) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.add(loc);

        cauldronBlock.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f);

        // [MỚI]: TẠO HOLOGRAM ĐẾM NGƯỢC
        org.bukkit.entity.TextDisplay hologram = loc.getWorld().spawn(loc.clone().add(0.5, 1.2, 0.5), org.bukkit.entity.TextDisplay.class);
        hologram.setBillboard(org.bukkit.entity.Display.Billboard.CENTER); // Luôn xoay mặt về phía người chơi
        hologram.setDefaultBackground(false);
        hologram.setBackgroundColor(Color.fromARGB(100, 0, 0, 0)); // Nền đen mờ

        new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 400; // 20 giây (20 ticks * 20s)
            final List<Item> ingredients = new ArrayList<>();

            @Override
            public void run() {
                // Hủy nếu Vạc bị đập
                if (cauldronBlock.getType() != Material.WATER_CAULDRON) {
                    cookingPots.remove(loc);
                    hologram.remove(); // Xóa Hologram
                    this.cancel();
                    return;
                }

                // Quét nguyên liệu
                boolean addedNewItem = false;
                for (Item item : loc.getWorld().getEntitiesByClass(Item.class)) {
                    if (item.isDead()) continue;
                    Block iBlock = item.getLocation().getBlock();
                    Block iBlockBelow = item.getLocation().clone().subtract(0, 0.5, 0).getBlock();

                    if ((iBlock.equals(cauldronBlock) || iBlockBelow.equals(cauldronBlock)) && !ingredients.contains(item)) {
                        String ingredientId = ingredientManager.getIngredientId(item.getItemStack());
                        if (ingredientId != null) {
                            if (ingredients.size() < 9) {
                                ingredients.add(item);
                                item.setPickupDelay(9999);
                                addedNewItem = true; // Đánh dấu là có đồ mới
                            } else {
                                item.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));
                            }
                        } else {
                            item.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));
                        }
                    }
                }

                // [MỚI]: RESET ĐỒNG HỒ NẾU CÓ ĐỒ MỚI VÀO NỒI
                if (addedNewItem) {
                    ticks = 0;
                    loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
                }

                // CẬP NHẬT CHỮ HOLOGRAM (Mỗi giây update 1 lần để đỡ lag)
                if (ticks % 20 == 0) {
                    int secondsLeft = (MAX_TICKS - ticks) / 20;
                    hologram.setText("§e♨ Đang nấu...\n§f" + secondsLeft + "s §8| §a" + ingredients.size() + "§8/§a9 món");
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

                if (ticks >= MAX_TICKS) {
                    finishCooking(cauldronBlock, ingredients);
                    hologram.remove(); // Nấu xong thì xóa Hologram
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishCooking(Block cauldronBlock, List<Item> ingredients) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.remove(loc);

        // 1. TẬP HỢP DỮ LIỆU NGUYÊN LIỆU ĐỂ CHẤM ĐIỂM
        List<IngredientManager.IngredientData> recipeData = new ArrayList<>();
        for (Item item : ingredients) {
            String id = ingredientManager.getIngredientId(item.getItemStack());
            if (id != null) {
                IngredientManager.IngredientData data = ingredientManager.getIngredientData(id);
                if (data != null) recipeData.add(data);
            }
            item.remove();
        }

        // 2. GỌI HỆ THỐNG TRÍ TUỆ NHÂN TẠO ĐỂ PHÂN TÍCH MÓN ĂN
        ItemStack result = evaluateRecipe(recipeData);

        // 3. XÁC ĐỊNH MÀU SẮC NƯỚC SÔI THEO TÊN MÓN ĂN
        Color pColor = Color.fromRGB(200, 150, 50); // Mặc định Vàng nâu
        String name = result.getItemMeta().getDisplayName();

        if (name.contains("Kì Lạ")) pColor = Color.fromRGB(80, 100, 80); // Thất bại -> Xám xanh
        else if (name.contains("Thịt")) pColor = Color.fromRGB(200, 50, 0); // Thịt -> Đỏ cam
        else if (name.contains("Rau") || name.contains("Salad")) pColor = Color.fromRGB(50, 200, 50); // Rau -> Xanh lá

        cauldronBlock.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0.5, 0.8, 0.5), 15, 0.2, 0.2, 0.2, 0.1);

        activePots.put(loc, new FinishedPot(result, 6, pColor));
    }

    // =======================================================
    // HỆ THỐNG CHẤM ĐIỂM VÀ TẠO MÓN ĂN (DYNAMIC RECIPE ENGINE)
    // =======================================================
    private ItemStack evaluateRecipe(List<IngredientManager.IngredientData> ingredients) {
        double meat = 0, veg = 0, fish = 0, fat = 0, starch = 0, monster = 0, magic = 0, spice = 0;
        double totalPoints = 0;

        for (IngredientManager.IngredientData data : ingredients) {
            totalPoints += data.points;
            switch (data.category) {
                case MEAT: meat += data.points; break;
                case VEG: veg += data.points; break;
                case FISH: fish += data.points; break;
                case FAT: fat += data.points; break;
                case STARCH: starch += data.points; break;
                case MONSTER: monster += data.points; break;
                case MAGIC: magic += data.points; break;
                case SPICE: spice += data.points; break;
                default: break;
            }
        }

        ItemStack item = new ItemStack(Material.MUSHROOM_STEW);
        ItemMeta meta = item.getItemMeta();
        String name = "§8Món Ăn Kì Lạ";
        List<String> lore = new ArrayList<>();

        // BIẾN LƯU TRỮ THỜI GIAN THIU (Tính bằng phút)
        int maxFreshnessMinutes = 30; // Mặc định 30 phút

        if (totalPoints == 0 || (monster + magic) / totalPoints > 0.25) {
            name = "§8Món Ăn Kì Lạ";
            lore.add("§7Nấu sai công thức hoặc tạp chất quá nhiều...");
            maxFreshnessMinutes = 10; // Đồ độc thiu cực nhanh (10 phút)
        } else {
            if (meat >= 1.0 && veg >= 0.5) {
                name = "§6§lThịt Hầm Thượng Hạng";
                item.setType(Material.RABBIT_STEW);
                lore.add("§7Món ăn cân bằng hoàn hảo giữa thịt và rau củ.");
                maxFreshnessMinutes = 60; // Nấu kĩ, để được 1 tiếng
            } else if (meat >= 1.0) {
                name = "§c§lSúp Thịt Nguyên Tảng";
                item.setType(Material.RABBIT_STEW);
                lore.add("§7Nhiều năng lượng, nhưng ăn nhiều sẽ nặng bụng.");
                maxFreshnessMinutes = 45;
            } else if (veg >= 1.0) {
                name = "§a§lSalad Súp Thanh Lọc";
                item.setType(Material.BEETROOT_SOUP);
                lore.add("§7Tươi mát và thanh khiết. Giúp giải độc cơ thể.");
                maxFreshnessMinutes = 15; // Rau sống dễ hỏng (15 phút)
            } else {
                name = "§e§lSúp Thập Cẩm";
                item.setType(Material.MUSHROOM_STEW);
            }

            if (spice > 0) {
                lore.add("§6[Đậm Vị] §7- Giảm đáng kể độ ngán khi ăn liên tục.");
                maxFreshnessMinutes += 15; // Gia vị giúp bảo quản đồ ăn lâu hơn
            }
            if (fat > 0) lore.add("§e[Béo Ngậy] §7- Tăng cường sức mạnh của Buff.");
        }

        // LƯU ÂM THẦM VÀO ITEM (Không in ra Lore nữa)
        long currentTime = System.currentTimeMillis();
        meta.getPersistentDataContainer().set(COOKED_TIME_KEY, PersistentDataType.LONG, currentTime);
        meta.getPersistentDataContainer().set(MAX_FRESHNESS_KEY, PersistentDataType.INTEGER, maxFreshnessMinutes);

        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
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

            // Mỗi lần múc là tạo ra 1 clone mới (Để lỡ múc sau 5 phút thì Timestamp vẫn là lúc Nồi chín)
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
}