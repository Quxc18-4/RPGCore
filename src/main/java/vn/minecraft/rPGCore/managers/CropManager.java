package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CropManager implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final File cropsFolder;

    private final Map<Location, CropData> activeCrops = new HashMap<>();
    private final long BASE_GROW_TIME_MS = 14400000L; // 4 Tiếng

    public static class CropData {
        public UUID owner;
        public String seedId;
        public long plantTime;
        public int harvestsLeft;
        public int maxHarvests;
        public long requiredTime;

        public CropData(UUID owner, String seedId, long plantTime, int harvestsLeft, int maxHarvests, long requiredTime) {
            this.owner = owner;
            this.seedId = seedId;
            this.plantTime = plantTime;
            this.harvestsLeft = harvestsLeft;
            this.maxHarvests = maxHarvests;
            this.requiredTime = requiredTime;
        }
    }

    public CropManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cropsFolder = new File(plugin.getDataFolder(), "crops");
        if (!cropsFolder.exists()) cropsFolder.mkdirs();
        loadAllCrops();
        startRayTraceScanner();
        startVisualUpdater();
    }

    // ==========================================
    // CƠ SỞ DỮ LIỆU
    // ==========================================
    private void loadAllCrops() {
        activeCrops.clear();
        File[] files = cropsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            UUID owner;
            try { owner = UUID.fromString(file.getName().replace(".yml", "")); } catch (Exception e) { continue; }

            if (config.contains("locations")) {
                for (String locStr : config.getConfigurationSection("locations").getKeys(false)) {
                    String path = "locations." + locStr;
                    String seedId = config.getString(path + ".seedId");
                    long plantTime = config.getLong(path + ".plantTime");
                    int harvestsLeft = config.getInt(path + ".harvestsLeft", 1);
                    int maxHarvests = config.getInt(path + ".maxHarvests", 1);
                    long reqTime = config.getLong(path + ".reqTime", BASE_GROW_TIME_MS);

                    String[] parts = locStr.split(",");
                    if (parts.length == 4) {
                        try {
                            org.bukkit.World world = Bukkit.getWorld(parts[0]);
                            Location loc = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                            if (world != null) activeCrops.put(loc, new CropData(owner, seedId, plantTime, harvestsLeft, maxHarvests, reqTime));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void saveCropForPlayer(UUID uuid) {
        File file = new File(cropsFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<Location, CropData> entry : activeCrops.entrySet()) {
            if (entry.getValue().owner.equals(uuid)) {
                Location loc = entry.getKey();
                String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                config.set("locations." + locStr + ".seedId", entry.getValue().seedId);
                config.set("locations." + locStr + ".plantTime", entry.getValue().plantTime);
                config.set("locations." + locStr + ".harvestsLeft", entry.getValue().harvestsLeft);
                config.set("locations." + locStr + ".maxHarvests", entry.getValue().maxHarvests);
                config.set("locations." + locStr + ".reqTime", entry.getValue().requiredTime);
            }
        }
        try {
            if (config.getConfigurationSection("locations") == null) { if (file.exists()) file.delete(); }
            else config.save(file);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ==========================================
    // TƯƠNG TÁC (GIEO HẠT, THU HOẠCH, PHÂN BÓN)
    // ==========================================
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.PHYSICAL && clickedBlock != null && clickedBlock.getType() == Material.FARMLAND) {
            event.setCancelled(true); return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            String itemId = itemManager.getItemId(handItem);

            Block rootCropBlock = getRootCropBlock(clickedBlock);

            // --- 1. DÙNG PHÂN BÓN & BÌNH TƯỚI ---
            if (rootCropBlock != null && itemId != null && (itemId.equals("misc_fertilizer") || itemId.equals("misc_watering_can"))) {
                event.setCancelled(true);
                handleFertilizer(player, rootCropBlock, itemId, handItem);
                return;
            }

            // --- 2. THU HOẠCH ---
            if (rootCropBlock != null) {
                if (itemId != null && itemId.equals("tool_harvest_hoe")) {
                    event.setCancelled(true);
                    handleHarvest(player, rootCropBlock);
                } else {
                    player.sendMessage("§cBạn phải dùng §eCuốc Thu Hoạch §cđể lấy nông sản mà không làm đứt rễ!");
                }
                return;
            }

            // --- 3. GIEO HẠT ---
            if (itemId != null && itemId.startsWith("seed_")) {
                BlockFace clickedFace = event.getBlockFace();

                if (!isValidPlacement(clickedBlock, clickedFace, itemId)) return;
                Block targetBlock = getTargetBlockForSeed(clickedBlock, clickedFace, itemId);
                if (targetBlock == null || (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.WATER)) return;

                event.setCancelled(true);

                vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
                boolean isFarmer = jobData != null && jobData.getJobId().equals("FARMER");

                if (itemId.contains("magic_") && !isFarmer) {
                    player.sendMessage("§cChỉ có Nghề Nông Dân mới biết cách gieo trồng Hạt giống Ma thuật này!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                Material cropMat = getVanillaCropMaterial(itemId);
                if (cropMat != null) {
                    targetBlock.setType(cropMat);
                    BlockData blockData = targetBlock.getBlockData();
                    if (blockData instanceof Waterlogged) ((Waterlogged) blockData).setWaterlogged(true);
                    if (blockData instanceof Cocoa) ((Cocoa) blockData).setFacing(clickedFace);
                    if (itemId.contains("cucumber") && blockData instanceof SeaPickle) ((SeaPickle) blockData).setPickles(1);
                    targetBlock.setBlockData(blockData);

                    int harvests = getMaxHarvests(itemId);
                    long reqTime = BASE_GROW_TIME_MS;

                    if (isFarmer) {
                        if (hasUltimate(player, "farmer_ult_1") && harvests > 1) harvests *= 2;
                        if (hasUltimate(player, "farmer_ult_2")) reqTime = 10800000L;
                    }

                    activeCrops.put(targetBlock.getLocation(), new CropData(player.getUniqueId(), itemId, System.currentTimeMillis(), harvests, harvests, reqTime));
                    saveCropForPlayer(player.getUniqueId());

                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) handItem.setAmount(handItem.getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
                    player.sendMessage("§aĐã gieo hạt! Thời gian chín: " + (reqTime / 3600000) + " Tiếng.");

                    // [HOOK QUEST]: Gieo Mầm Sự Sống
                    try {
                        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.PLANT_SEED);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // ==========================================
    // LOGIC PHÂN BÓN & TƯỚI NƯỚC (CÓ ĐỘ BỀN)
    // ==========================================
    private void handleFertilizer(Player player, Block rootBlock, String itemId, ItemStack handItem) {
        org.bukkit.inventory.meta.ItemMeta meta = handItem.getItemMeta();
        org.bukkit.NamespacedKey duraKey = new org.bukkit.NamespacedKey(plugin, "farming_tool_durability");

        // Cài đặt số lần dùng tối đa
        int maxDura = itemId.equals("misc_watering_can") ? 20 : 10;
        int currentDura = meta.getPersistentDataContainer().getOrDefault(duraKey, org.bukkit.persistence.PersistentDataType.INTEGER, maxDura);

        if (itemId.equals("misc_watering_can")) {
            CropData data = activeCrops.get(rootBlock.getLocation());
            data.plantTime -= 3600000L; // Giảm 1 tiếng
            player.getWorld().spawnParticle(Particle.SPLASH, rootBlock.getLocation().add(0.5, 1, 0.5), 30, 0.3, 0.3, 0.3);
            player.playSound(rootBlock.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1f, 1f);
            player.sendMessage("§bĐã tưới nước! Thời gian được rút ngắn 1 tiếng.");
        }
        else if (itemId.equals("misc_fertilizer")) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Location loc = rootBlock.getLocation().add(x, y, z);
                        if (activeCrops.containsKey(loc)) {
                            CropData data = activeCrops.get(loc);
                            if (x == 0 && y == 0 && z == 0) data.plantTime -= 7200000L; // Giảm 2 tiếng
                            else data.plantTime -= 3600000L; // Giảm 1 tiếng
                        }
                    }
                }
            }
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, rootBlock.getLocation().add(0.5, 1, 0.5), 50, 1.5, 1.5, 1.5);
            player.playSound(rootBlock.getLocation(), Sound.BLOCK_COMPOSTER_READY, 1f, 1f);
            player.sendMessage("§aĐã bón phân diện rộng! Cây trung tâm -2h, xung quanh -1h.");

            try {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.USE_FERTILIZER);
            } catch (Exception ignored) {}
        }
        saveCropForPlayer(player.getUniqueId());

        // --- TRỪ ĐỘ BỀN VÀ CẬP NHẬT LORE ---
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            currentDura--;
            if (currentDura <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                player.sendMessage("§c§l[!] §fVật phẩm nông nghiệp của bạn đã hết lần sử dụng!");
                handItem.setAmount(handItem.getAmount() - 1);
            } else {
                meta.getPersistentDataContainer().set(duraKey, org.bukkit.persistence.PersistentDataType.INTEGER, currentDura);

                List<String> lore = meta.getLore();
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        if (lore.get(i).contains("Độ bền:")) {
                            lore.set(i, "§7Độ bền: " + currentDura + " lần sử dụng.");
                            break;
                        }
                    }
                    meta.setLore(lore);
                }
                handItem.setItemMeta(meta);
            }
        }
    }

    private void handleHarvest(Player player, Block rootBlock) {
        Location loc = rootBlock.getLocation();
        CropData data = activeCrops.get(loc);

        if (!data.owner.equals(player.getUniqueId())) {
            player.sendMessage("§cĐây không phải là nông sản do bạn trồng!");
            return;
        }

        long ageMs = System.currentTimeMillis() - data.plantTime;
        if (ageMs < data.requiredTime) {
            player.sendMessage("§eCây vẫn chưa chín. Vui lòng quay lại sau §a" + formatTime((data.requiredTime - ageMs) / 1000) + "§e!");
            return;
        }

        vn.minecraft.rPGCore.jobs.core.PlayerJobData jobData = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        int jobLevel = (jobData != null && jobData.getJobId().equals("FARMER")) ? jobData.getLevel() : 0;

        double hatBuff = 0.0;
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack helmet = player.getInventory().getHelmet();

        ItemStack activeHat = null;
        boolean isHelmet = false;

        // Kiểm tra xem nón đang nằm ở đâu
        if (itemManager.getItemId(helmet) != null && itemManager.getItemId(helmet).equals("armor_farmer_hat")) {
            activeHat = helmet;
            isHelmet = true;
        } else if (itemManager.getItemId(offHand) != null && itemManager.getItemId(offHand).equals("armor_farmer_hat")) {
            activeHat = offHand;
        }

        if (activeHat != null) {
            hatBuff = 0.10;
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1, 0.5), 10, 0.2, 0.2, 0.2);

            // --- LOGIC TRỪ ĐỘ BỀN NÓN LÁ (300 LẦN) ---
            org.bukkit.inventory.meta.ItemMeta hatMeta = activeHat.getItemMeta();
            org.bukkit.NamespacedKey duraKey = new org.bukkit.NamespacedKey(plugin, "farmer_hat_durability");

            // Lấy độ bền hiện tại (Nếu chưa có thì mặc định là 300)
            int currentDura = hatMeta.getPersistentDataContainer().getOrDefault(duraKey, org.bukkit.persistence.PersistentDataType.INTEGER, 300);
            currentDura--;

            if (currentDura <= 0) {
                // Nón đã hỏng hoàn toàn
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                player.sendMessage("§c§l[!] §fNón Lá Chân Truyền của bạn đã bị hỏng rách!");
                if (isHelmet) player.getInventory().setHelmet(null);
                else player.getInventory().setItemInOffHand(null);
            } else {
                // Lưu lại độ bền mới vào Nón
                hatMeta.getPersistentDataContainer().set(duraKey, org.bukkit.persistence.PersistentDataType.INTEGER, currentDura);

                // Cập nhật lại dòng Lore hiển thị độ bền
                List<String> lore = hatMeta.getLore();
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        if (lore.get(i).contains("Độ bền:")) {
                            lore.set(i, "§7Độ bền: " + currentDura + " lần thu hoạch.");
                            break;
                        }
                    }
                    hatMeta.setLore(lore);
                }
                activeHat.setItemMeta(hatMeta);
            }
        }

        Random rand = new Random();
        boolean hasUlt3 = hasUltimate(player, "farmer_ult_3");
        boolean isMultiHarvest = data.maxHarvests > 1;

        double chanceRare = (jobLevel > 0) ? (0.15 + (0.15 * (jobLevel/50.0)) + hatBuff) : 0.0;
        double chanceUncommon = (jobLevel > 0) ? (0.40 + (0.30 * (jobLevel/50.0)) + hatBuff) : (0.10 + hatBuff);

        if (hasUlt3) {
            chanceRare += 0.15;
            chanceUncommon = 1.0;
        }

        String qualitySuffix = "";
        if (rand.nextDouble() < chanceRare) qualitySuffix = "_rare";
        else if (rand.nextDouble() < chanceUncommon) qualitySuffix = "_uncommon";

        int yield = 1;
        if (jobLevel > 0) {
            if (!isMultiHarvest) {
                double chanceYield = 0.20 + (0.20 * (jobLevel/50.0)) + hatBuff;
                if (hasUlt3) chanceYield += 0.20;
                // [FIX BUG TOÁN HỌC]: Đảm bảo nổ 2 hoặc 3 quả, không nổ 1 quả.
                if (rand.nextDouble() < chanceYield) yield = 2 + rand.nextInt(2);
            } else {
                double chanceYield = 0.10 + (0.10 * (jobLevel/50.0)) + hatBuff;
                if (hasUlt3) chanceYield += 0.10;
                if (rand.nextDouble() < chanceYield) yield = 2;
            }
        }

        double seedRecovery = (jobLevel > 0) ? (0.70 + (0.30 * (jobLevel/50.0)) + hatBuff) : (0.20 + rand.nextDouble() * 0.10 + hatBuff);
        int seedYield = 0;
        if (rand.nextDouble() < seedRecovery) seedYield = 1;
        if (jobLevel > 0 && rand.nextDouble() < (0.05 + hatBuff)) seedYield = 2;

        // --- TRẢ THƯỞNG ---
        String finalRewardId;
        String fallbackId;

        if (data.seedId.startsWith("seed_magic_")) {
            finalRewardId = data.seedId.replace("seed_", "");
            fallbackId = finalRewardId;
        } else {
            String baseRewardId = data.seedId.replace("seed_", "");
            if (baseRewardId.equals("sweet_berry")) baseRewardId = "berries";
            if (baseRewardId.equals("glow_berry")) baseRewardId = "glow_berries";
            if (baseRewardId.equals("kelp")) baseRewardId = "kelp_fresh";

            finalRewardId = "veg_" + baseRewardId + qualitySuffix;
            fallbackId = "veg_" + baseRewardId;
        }

        ItemStack rewardItem = itemManager.buildItem(finalRewardId, yield);
        if (rewardItem != null) loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), rewardItem);
        else {
            ItemStack backup = itemManager.buildItem(fallbackId, yield);
            if (backup != null) loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), backup);
        }

        if (seedYield > 0) {
            ItemStack seedItem = itemManager.buildItem(data.seedId, seedYield);
            if (seedItem != null) loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), seedItem);

            // [HOOK QUEST]: Bậc Thầy Thu Hồi
            try {
                for(int i=0; i<seedYield; i++) {
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.RECOVER_SEED);
                }
            } catch (Exception ignored) {}
        }

        if (jobLevel > 0 && rand.nextDouble() < 0.3) {
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), itemManager.buildItem("misc_nature_essence", 1));
            // [HOOK QUEST]: Thu Thập Tinh Chất
            try {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.COLLECT_NATURE_ESSENCE);
            } catch (Exception ignored) {}
        }

        player.playSound(loc, Sound.BLOCK_CROP_BREAK, 1f, 1f);

        // =======================================================
        // [ĐÃ BỔ SUNG]: CỘNG EXP NGHỀ KHI THU HOẠCH THÀNH CÔNG
        // =======================================================
        try {
            double gainedExp = data.seedId.contains("magic_") ? 80.0 : 25.0;
            if (qualitySuffix.equals("_uncommon")) gainedExp *= 1.5;
            else if (qualitySuffix.equals("_rare")) gainedExp *= 2.0;

            vn.minecraft.rPGCore.jobs.core.JobManager.addExp(player, gainedExp);
        } catch (Exception ignored) {}
        // =======================================================

        // --- HOOK CÁC SỰ KIỆN QUEST CHÍNH ---
        try {
            vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.HARVEST_CROP);
            if (data.seedId.contains("magic_")) {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.HARVEST_MAGIC_CROP);
            }
            if (qualitySuffix.equals("_uncommon") || qualitySuffix.equals("_rare")) {
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.HARVEST_MUTATION);
            }
        } catch (Exception ignored) {}

        // --- XỬ LÝ KHÔNG GIAN ---
        Block fruitBlock = getFruitBlock(rootBlock, data.seedId);
        if (fruitBlock != null) {
            if (data.seedId.contains("kelp") || data.seedId.contains("cucumber")) fruitBlock.setType(Material.WATER);
            else fruitBlock.setType(Material.AIR);
        }

        if (data.harvestsLeft > 1) {
            data.harvestsLeft--;
            data.plantTime = System.currentTimeMillis();
            saveCropForPlayer(player.getUniqueId());
            if (rootBlock.getBlockData() instanceof org.bukkit.block.data.Ageable) {
                org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) rootBlock.getBlockData();
                ageable.setAge(0); rootBlock.setBlockData(ageable);
            } else if (rootBlock.getBlockData() instanceof CaveVines) {
                CaveVines vines = (CaveVines) rootBlock.getBlockData();
                vines.setBerries(false); rootBlock.setBlockData(vines);
            }
            player.sendMessage("§aĐã thu hoạch! Cây sẽ tiếp tục ra quả lứa tiếp theo. (Còn " + data.harvestsLeft + " vụ)");
        } else {
            activeCrops.remove(loc);
            if (data.seedId.contains("kelp") || data.seedId.contains("cucumber")) rootBlock.setType(Material.WATER);
            else rootBlock.setType(Material.AIR);
            saveCropForPlayer(player.getUniqueId());
            player.sendMessage("§aThu hoạch lứa cuối cùng! Cây đã tàn lụi, hãy trồng cây mới.");
        }
    }

    // ==========================================
    // VISUAL & QUÉT KHÔNG GIAN
    // ==========================================
    private void startVisualUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<Location, CropData> entry : activeCrops.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType() != Material.AIR) {
                        CropData data = entry.getValue();
                        long ageMs = now - data.plantTime;

                        if (ageMs >= data.requiredTime) {
                            Block fruitBlock = getFruitBlock(block, data.seedId);
                            if (fruitBlock == null) spawnFruitBlock(block, data.seedId);
                        }

                        BlockData bData = block.getBlockData();
                        if (bData instanceof org.bukkit.block.data.Ageable) {
                            org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) bData;
                            int maxAge = ageable.getMaximumAge();
                            int currentAge = (ageMs >= data.requiredTime) ? maxAge : (int) (maxAge * ((double) ageMs / data.requiredTime));
                            if (ageable.getAge() != currentAge) {
                                ageable.setAge(currentAge); block.setBlockData(ageable);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void spawnFruitBlock(Block root, String seedId) {
        if (seedId.contains("melon") || seedId.contains("pumpkin")) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block side = root.getRelative(face);
                Block sideDown = side.getRelative(BlockFace.DOWN);
                if (side.getType() == Material.AIR && (sideDown.getType() == Material.DIRT || sideDown.getType() == Material.GRASS_BLOCK || sideDown.getType() == Material.FARMLAND)) {
                    side.setType(seedId.contains("melon") ? Material.MELON : Material.PUMPKIN);
                    break;
                }
            }
        } else if (seedId.contains("kelp")) {
            Block up = root.getRelative(BlockFace.UP);
            if (up.getType() == Material.WATER) up.setType(Material.KELP);
        } else if (seedId.contains("cucumber")) {
            Block up = root.getRelative(BlockFace.UP);
            if (up.getType() == Material.WATER) {
                up.setType(Material.SEA_PICKLE);
                SeaPickle sp = (SeaPickle) up.getBlockData();
                sp.setWaterlogged(true); sp.setPickles(4); up.setBlockData(sp);
            }
        } else if (seedId.contains("glow_berry")) {
            Block down = root.getRelative(BlockFace.DOWN);
            if (down.getType() == Material.AIR) {
                down.setType(Material.CAVE_VINES);
                CaveVines cv = (CaveVines) down.getBlockData();
                cv.setBerries(true); down.setBlockData(cv);
            }
        }
    }

    // ==========================================
    // THUẬT TOÁN "BẮC CẦU" VÀ BẢO VỆ
    // ==========================================
    private Block getRootCropBlock(Block clicked) {
        if (activeCrops.containsKey(clicked.getLocation())) return clicked;
        Material t = clicked.getType();
        if (t == Material.MELON || t == Material.PUMPKIN) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block side = clicked.getRelative(face);
                if (activeCrops.containsKey(side.getLocation())) {
                    String seedId = activeCrops.get(side.getLocation()).seedId;
                    if ((t == Material.MELON && seedId.contains("melon")) || (t == Material.PUMPKIN && seedId.contains("pumpkin"))) return side;
                }
            }
        } else if (t == Material.KELP || t == Material.KELP_PLANT || t == Material.SEA_PICKLE) {
            Block down = clicked.getRelative(BlockFace.DOWN);
            if (activeCrops.containsKey(down.getLocation()) && (activeCrops.get(down.getLocation()).seedId.contains("kelp") || activeCrops.get(down.getLocation()).seedId.contains("cucumber"))) return down;
        } else if (t == Material.CAVE_VINES || t == Material.CAVE_VINES_PLANT) {
            Block up = clicked.getRelative(BlockFace.UP);
            if (activeCrops.containsKey(up.getLocation()) && activeCrops.get(up.getLocation()).seedId.contains("glow_berry")) return up;
        }
        return null;
    }

    private Block getFruitBlock(Block rootBlock, String seedId) {
        if (seedId.contains("melon") || seedId.contains("pumpkin")) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block side = rootBlock.getRelative(face);
                if ((seedId.contains("melon") && side.getType() == Material.MELON) || (seedId.contains("pumpkin") && side.getType() == Material.PUMPKIN)) return side;
            }
        } else if (seedId.contains("kelp") || seedId.contains("cucumber")) {
            Block up = rootBlock.getRelative(BlockFace.UP);
            if (up.getType() == Material.KELP || up.getType() == Material.KELP_PLANT || up.getType() == Material.SEA_PICKLE) return up;
        } else if (seedId.contains("glow_berry")) {
            Block down = rootBlock.getRelative(BlockFace.DOWN);
            if (down.getType() == Material.CAVE_VINES || down.getType() == Material.CAVE_VINES_PLANT) return down;
        }
        return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (getRootCropBlock(block) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cHãy dùng §eCuốc Thu Hoạch §cđể hái. Việc đập bỏ sẽ làm hỏng nông sản!");
            return;
        }
        Block up = block.getRelative(BlockFace.UP);
        if (activeCrops.containsKey(up.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cKhông thể đập khối móng khi cây đang phát triển ở trên!");
            return;
        }
        Block down = block.getRelative(BlockFace.DOWN);
        if (activeCrops.containsKey(down.getLocation()) && activeCrops.get(down.getLocation()).seedId.contains("glow_berry")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cKhông thể đập trần nhà khi dây leo đang bám!");
            return;
        }
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block side = block.getRelative(face);
            if (activeCrops.containsKey(side.getLocation()) && activeCrops.get(side.getLocation()).seedId.contains("cocoa")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cKhông thể đập thân cây này vì đang có quả Cacao bám vào!");
                return;
            }
        }
    }

    @EventHandler public void onCropGrow(BlockGrowEvent event) { if (activeCrops.containsKey(event.getBlock().getLocation())) event.setCancelled(true); }
    @EventHandler public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        if (activeCrops.containsKey(event.getBlock().getLocation())) event.setCancelled(true);
        Block blockAbove = event.getBlock().getRelative(BlockFace.UP);
        if (activeCrops.containsKey(blockAbove.getLocation()) && activeCrops.get(blockAbove.getLocation()).seedId.contains("glow_berry")) event.setCancelled(true);
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH & ĐỊA HÌNH
    // ==========================================
    private boolean isValidPlacement(Block clickedBlock, BlockFace face, String seedId) {
        Material type = clickedBlock.getType();
        if (seedId.contains("glow_berry")) {
            if (face != BlockFace.DOWN || !type.isSolid()) return false;
            Block root = clickedBlock.getRelative(BlockFace.DOWN);
            return root.getType() == Material.AIR && root.getRelative(BlockFace.DOWN).getType() == Material.AIR;
        }
        if (seedId.contains("kelp") || seedId.contains("cucumber")) {
            if (face != BlockFace.UP || !(type == Material.DIRT || type == Material.SAND || type == Material.GRAVEL)) return false;
            Block root = clickedBlock.getRelative(BlockFace.UP);
            return root.getType() == Material.WATER && root.getRelative(BlockFace.UP).getType() == Material.WATER;
        }
        if (seedId.contains("melon") || seedId.contains("pumpkin")) {
            if (face != BlockFace.UP || type != Material.FARMLAND) return false;
            Block root = clickedBlock.getRelative(BlockFace.UP);
            for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block side = root.getRelative(f);
                Block sideDown = side.getRelative(BlockFace.DOWN);
                if ((side.getType() == Material.AIR || side.getType() == Material.WATER) && (sideDown.getType() == Material.DIRT || sideDown.getType() == Material.GRASS_BLOCK || sideDown.getType() == Material.FARMLAND)) return true;
            }
            return false;
        }
        if (seedId.contains("sweet_berry")) return face == BlockFace.UP && (type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.PODZOL);
        if (seedId.contains("mushroom")) return face == BlockFace.UP && (type.name().endsWith("_LOG") || type.name().endsWith("_WOOD"));
        if (seedId.contains("cocoa")) return (face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST) && type == Material.JUNGLE_LOG;
        return face == BlockFace.UP && type == Material.FARMLAND;
    }

    private Block getTargetBlockForSeed(Block clickedBlock, BlockFace face, String seedId) {
        if (seedId.contains("glow_berry")) return clickedBlock.getRelative(BlockFace.DOWN);
        if (seedId.contains("cocoa")) return clickedBlock.getRelative(face);
        return clickedBlock.getRelative(BlockFace.UP);
    }

    private int getMaxHarvests(String seedId) {
        if (seedId.contains("berry")) return 5;
        if (seedId.contains("melon") || seedId.contains("pumpkin") || seedId.contains("cocoa") || seedId.contains("kelp") || seedId.contains("cucumber")) return 3;
        return 1;
    }

    private Material getVanillaCropMaterial(String seedId) {
        if (seedId.contains("carrot")) return Material.CARROTS;
        if (seedId.contains("potato")) return Material.POTATOES;
        if (seedId.contains("wheat")) return Material.WHEAT;
        if (seedId.contains("beetroot")) return Material.BEETROOTS;
        if (seedId.contains("sweet_berry")) return Material.SWEET_BERRY_BUSH;
        if (seedId.contains("glow_berry")) return Material.CAVE_VINES;
        if (seedId.contains("cocoa")) return Material.COCOA;
        if (seedId.contains("red_mushroom")) return Material.RED_MUSHROOM;
        if (seedId.contains("mushroom")) return Material.BROWN_MUSHROOM;
        if (seedId.contains("kelp")) return Material.KELP_PLANT;
        if (seedId.contains("cucumber")) return Material.SEA_PICKLE;
        if (seedId.contains("melon")) return Material.MELON_STEM;
        if (seedId.contains("pumpkin")) return Material.PUMPKIN_STEM;
        return Material.OAK_SAPLING;
    }

    @EventHandler
    public void onOPSneakForwardTime(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && event.isSneaking()) {
            boolean forwarded = false;
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Location loc = player.getLocation().add(x, y, z).getBlock().getLocation();
                        if (activeCrops.containsKey(loc)) {
                            CropData data = activeCrops.get(loc);
                            data.plantTime -= 1800000L;
                            long timeLeft = data.requiredTime - (System.currentTimeMillis() - data.plantTime);
                            player.sendMessage("§d[Dev] §fTua 30p lân cận. Còn: §a" + Math.max(0, timeLeft / 60000) + " phút.");
                            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);
                            forwarded = true;
                        }
                    }
                }
            }
            if (forwarded) {
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);
                saveCropForPlayer(player.getUniqueId());
            }
        }
    }

    private void startRayTraceScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Block target = p.getTargetBlockExact(5);
                    if (target != null) {
                        Block rootBlock = getRootCropBlock(target);
                        if (rootBlock != null && activeCrops.containsKey(rootBlock.getLocation())) {
                            CropData data = activeCrops.get(rootBlock.getLocation());
                            long ageMs = System.currentTimeMillis() - data.plantTime;
                            String vuMua = data.maxHarvests > 1 ? " §7| §dVụ mùa: " + (data.maxHarvests - data.harvestsLeft + 1) + "/" + data.maxHarvests : "";

                            if (ageMs >= data.requiredTime) {
                                p.sendActionBar("§a§l✔ CÂY ĐÃ CHÍN! Dùng Cuốc thu hoạch." + vuMua);
                            } else {
                                p.sendActionBar("§e⏳ Đang phát triển... Còn: §f" + formatTime((data.requiredTime - ageMs) / 1000) + vuMua);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // [ĐÃ SỬA]: Hoàn thiện Logic kiểm tra Cây Kỹ Năng
    private boolean hasUltimate(Player player, String ultId) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(player);
        return data != null && data.hasUnlockedSkill("FARMER", ultId.toUpperCase());
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}