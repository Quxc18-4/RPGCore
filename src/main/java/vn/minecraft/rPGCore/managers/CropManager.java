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
import org.bukkit.block.data.type.Cocoa;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CropManager implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final File cropsFolder;

    // Bộ nhớ RAM lưu trữ toàn bộ cây
    private final Map<Location, CropData> activeCrops = new HashMap<>();

    // Thời gian sinh trưởng mặc định: 4 tiếng (14,400,000 milliseconds)
    private final long GROW_TIME_MS = 14400000L;

    // Lớp chứa dữ liệu của một cái cây
    public static class CropData {
        public UUID owner;
        public String seedId;
        public long plantTime;
        public int harvestsLeft;
        public int maxHarvests;

        public CropData(UUID owner, String seedId, long plantTime, int harvestsLeft, int maxHarvests) {
            this.owner = owner;
            this.seedId = seedId;
            this.plantTime = plantTime;
            this.harvestsLeft = harvestsLeft;
            this.maxHarvests = maxHarvests;
        }
    }

    public CropManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        this.cropsFolder = new File(plugin.getDataFolder(), "crops");
        if (!cropsFolder.exists()) {
            cropsFolder.mkdirs();
        }

        loadAllCrops();
        startRayTraceScanner();
    }

    // ==========================================
    // 1. HỆ THỐNG CƠ SỞ DỮ LIỆU (FILE I/O)
    // ==========================================
    private void loadAllCrops() {
        activeCrops.clear();
        File[] files = cropsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int count = 0;
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

                    String[] parts = locStr.split(",");
                    if (parts.length == 4) {
                        try {
                            org.bukkit.World world = Bukkit.getWorld(parts[0]);
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            double z = Double.parseDouble(parts[3]);

                            if (world != null) {
                                Location loc = new Location(world, x, y, z);
                                activeCrops.put(loc, new CropData(owner, seedId, plantTime, harvestsLeft, maxHarvests));
                                count++;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        plugin.getLogger().info("Da tai thanh cong " + count + " cay trong tu thu muc crops/.");
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
            }
        }

        try {
            if (config.getConfigurationSection("locations") == null) {
                if (file.exists()) file.delete();
            } else {
                config.save(file);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ==========================================
    // 2. CẢM BIẾN TƯƠNG TÁC (GIEO HẠT & THU HOẠCH)
    // ==========================================
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // CHỐNG DẪM ĐẠP NÁT ĐẤT
        if (event.getAction() == Action.PHYSICAL && clickedBlock != null && clickedBlock.getType() == Material.FARMLAND) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            String itemId = itemManager.getItemId(handItem);

            // XỬ LÝ THU HOẠCH TRƯỚC (Ưu tiên check cây đã trồng)
            if (activeCrops.containsKey(clickedBlock.getLocation())) {
                if (itemId != null && itemId.equals("tool_harvest_hoe")) {
                    event.setCancelled(true);
                    handleHarvest(player, clickedBlock);
                } else {
                    player.sendMessage("§cBạn phải dùng §eCuốc Thu Hoạch §cđể lấy nông sản mà không làm đứt rễ!");
                }
                return; // Nếu đã click trúng cây thì dừng sự kiện gieo hạt
            }

            // XỬ LÝ GIEO HẠT THEO TỪNG LOẠI ĐỊA HÌNH
            if (itemId != null && itemId.startsWith("seed_")) {
                BlockFace clickedFace = event.getBlockFace();

                // 1. Kiểm tra điều kiện đất trồng
                if (!isValidPlacement(clickedBlock, clickedFace, itemId)) {
                    // Cảnh báo ẩn để tránh spam nếu lỡ click sai
                    return;
                }

                // 2. Lấy Block thực tế sẽ chứa cái cây
                Block targetBlock = getTargetBlockForSeed(clickedBlock, clickedFace, itemId);
                if (targetBlock == null || (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.WATER)) return;

                event.setCancelled(true); // Ngăn Vanilla

                // 3. Phân quyền Hạt Ma Thuật
                if (itemId.contains("magic_") && !isFarmer(player)) {
                    player.sendMessage("§cChỉ có Nghề Nông Dân cấp cao mới biết cách gieo trồng Hạt giống Ma thuật này!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // 4. Trồng cây
                Material cropMat = getVanillaCropMaterial(itemId);
                if (cropMat != null) {
                    targetBlock.setType(cropMat);

                    // Xử lý block đặc biệt (Nước/Cacao)
                    BlockData blockData = targetBlock.getBlockData();
                    if (blockData instanceof Waterlogged) {
                        ((Waterlogged) blockData).setWaterlogged(true);
                    }
                    if (blockData instanceof Cocoa) {
                        ((Cocoa) blockData).setFacing(clickedFace); // Gắn mặt cacao vào thân cây
                    }
                    targetBlock.setBlockData(blockData);

                    // 5. Lưu Database với số vụ mùa (Multi-harvest)
                    int harvests = getMaxHarvests(itemId);
                    activeCrops.put(targetBlock.getLocation(), new CropData(player.getUniqueId(), itemId, System.currentTimeMillis(), harvests, harvests));
                    saveCropForPlayer(player.getUniqueId());

                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                        handItem.setAmount(handItem.getAmount() - 1);
                    }

                    player.playSound(player.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
                    player.sendMessage("§aĐã gieo hạt thành công! Cần 4 tiếng để thu hoạch lứa đầu tiên.");
                }
            }
        }
    }

    // ==========================================
    // 3. XỬ LÝ THU HOẠCH & BẢO VỆ CÂY
    // ==========================================
    private void handleHarvest(Player player, Block cropBlock) {
        Location loc = cropBlock.getLocation();
        CropData data = activeCrops.get(loc);

        // Check sở hữu
        if (!data.owner.equals(player.getUniqueId())) {
            player.sendMessage("§cĐây không phải là nông sản do bạn trồng!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Check thời gian chín
        long ageMs = System.currentTimeMillis() - data.plantTime;
        if (ageMs < GROW_TIME_MS) {
            long timeLeft = (GROW_TIME_MS - ageMs) / 1000;
            player.sendMessage("§eCây vẫn chưa chín. Vui lòng quay lại sau §a" + formatTime(timeLeft) + "§e!");
            return;
        }

        // --- CÂY ĐÃ CHÍN: TIẾN HÀNH RỚT ĐỒ ---
        String rawName = data.seedId.replace("seed_", "");
        // TODO: Ở Phase tiếp theo, móc Nông dân vào đây để Roll đồ Đột Biến!
        ItemStack reward = itemManager.buildItem("veg_" + rawName, 1);
        if (reward != null) loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), reward);

        player.playSound(loc, Sound.BLOCK_CROP_BREAK, 1f, 1f);

        // XỬ LÝ ĐA VỤ MÙA (MULTI-HARVEST)
        if (data.harvestsLeft > 1) {
            data.harvestsLeft--;
            data.plantTime = System.currentTimeMillis(); // Reset thời gian
            saveCropForPlayer(player.getUniqueId());
            player.sendMessage("§aĐã thu hoạch! Cây sẽ tiếp tục ra quả lứa tiếp theo. (Còn " + data.harvestsLeft + " vụ)");
        } else {
            // Nhổ tận gốc
            activeCrops.remove(loc);
            if (data.seedId.contains("kelp") || data.seedId.contains("cucumber")) {
                cropBlock.setType(Material.WATER); // Trả lại nước
            } else {
                cropBlock.setType(Material.AIR);
            }
            saveCropForPlayer(player.getUniqueId());
            player.sendMessage("§aThu hoạch lứa cuối cùng! Cây đã tàn lụi, hãy trồng cây mới.");
        }
    }

    // Chặn người chơi đào phá cây
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Block blockAbove = block.getRelative(BlockFace.UP);
        Block blockBelow = block.getRelative(BlockFace.DOWN);

        // 1. Đào trực tiếp vào cây
        if (activeCrops.containsKey(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cHãy dùng §eCuốc Thu Hoạch §cđể hái. Việc đập bỏ sẽ làm hỏng nông sản!");
        }
        // 2. Đào khối đất/gỗ bên dưới cây
        else if (activeCrops.containsKey(blockAbove.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cKhông thể đập khối móng khi cây đang phát triển ở trên!");
        }
        // 3. Chống đào trần hang động (đối với Glow Berry)
        else if (activeCrops.containsKey(blockBelow.getLocation())) {
            CropData data = activeCrops.get(blockBelow.getLocation());
            if (data.seedId.contains("glow_berry")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cKhông thể đập khối trần khi có dây leo đang bám!");
            }
        }
        // 4. Chống đập thân cây gỗ khi có Cacao bám vào các mặt
        else {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block side = block.getRelative(face);
                if (activeCrops.containsKey(side.getLocation())) {
                    CropData data = activeCrops.get(side.getLocation());
                    if (data.seedId.contains("cocoa")) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage("§cKhông thể đập thân cây gỗ này vì đang có quả Cacao bám vào!");
                        break;
                    }
                }
            }
        }
    }

    // Chặn Vanilla tự làm cây lớn lên
    @EventHandler
    public void onCropGrow(BlockGrowEvent event) {
        if (activeCrops.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // 4. LỆNH OP: HACK THỜI GIAN (TUA 30 PHÚT)
    // ==========================================
    @EventHandler
    public void onOPSneakForwardTime(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && event.isSneaking()) {
            boolean forwarded = false;

            // Quét các ô xung quanh (Bán kính 3 block)
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Location loc = player.getLocation().add(x, y, z).getBlock().getLocation();
                        if (activeCrops.containsKey(loc)) {
                            CropData data = activeCrops.get(loc);
                            // Tua đi 30 phút (1,800,000 ms)
                            data.plantTime -= 1800000L;

                            // Tạo hạt sáng báo hiệu
                            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);
                            forwarded = true;
                        }
                    }
                }
            }

            if (forwarded) {
                player.sendMessage("§d§l[OP] §aĐã tua nhanh 30 phút cho các cây trồng gần đó!");
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);
                saveCropForPlayer(player.getUniqueId()); // Chú ý: Nên lưu chung toàn cục nếu test cho nhiều người, nhưng ở đây tạm lưu file của OP
            }
        }
    }

    // ==========================================
    // 5. TIỆN ÍCH HOLOGRAM & ĐỊA HÌNH
    // ==========================================
    private void startRayTraceScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Block target = p.getTargetBlockExact(5);
                    if (target != null && activeCrops.containsKey(target.getLocation())) {
                        CropData data = activeCrops.get(target.getLocation());
                        long ageMs = System.currentTimeMillis() - data.plantTime;

                        String vụMùa = data.maxHarvests > 1 ? " §7| §dVụ mùa: " + (data.maxHarvests - data.harvestsLeft + 1) + "/" + data.maxHarvests : "";

                        if (ageMs >= GROW_TIME_MS) {
                            p.sendActionBar("§a§l✔ CÂY ĐÃ CHÍN! Dùng Cuốc thu hoạch." + vụMùa);
                        } else {
                            long secondsLeft = (GROW_TIME_MS - ageMs) / 1000;
                            p.sendActionBar("§e⏳ Đang phát triển... Còn: §f" + formatTime(secondsLeft) + vụMùa);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // QUY TẮC ĐỊA HÌNH TRỒNG CÂY
    private boolean isValidPlacement(Block clickedBlock, BlockFace face, String seedId) {
        Material type = clickedBlock.getType();

        if (seedId.contains("glow_berry")) return face == BlockFace.DOWN && type.isSolid();
        if (seedId.contains("sweet_berry")) return face == BlockFace.UP && (type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.PODZOL);
        if (seedId.contains("mushroom")) return face == BlockFace.UP && (type.name().endsWith("_LOG") || type.name().endsWith("_WOOD"));
        if (seedId.contains("cocoa")) return (face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST) && type == Material.JUNGLE_LOG;
        if (seedId.contains("kelp") || seedId.contains("cucumber")) {
            Block above = clickedBlock.getRelative(BlockFace.UP);
            return face == BlockFace.UP && (type == Material.DIRT || type == Material.SAND || type == Material.GRAVEL) && above.getType() == Material.WATER;
        }

        // Mặc định Hoa màu (Cà rốt, khoai, lúa, bí...)
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
        if (seedId.contains("kelp")) return Material.KELP;
        if (seedId.contains("cucumber")) return Material.SEA_PICKLE;
        if (seedId.contains("melon")) return Material.MELON_STEM;
        if (seedId.contains("pumpkin")) return Material.PUMPKIN_STEM;
        return Material.OAK_SAPLING;
    }

    private boolean isFarmer(Player p) {
        vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(p);
        return data != null && data.getJobId().equals("FARMER");
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}