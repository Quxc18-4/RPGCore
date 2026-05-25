package vn.minecraft.rPGCore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.BoredomManager;
import vn.minecraft.rPGCore.managers.CookingManager;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConsumableListener implements Listener {

    private final ItemManager itemManager;
    private final BoredomManager boredomManager;
    private final CookingManager cookingManager;
    private final Map<UUID, Long> consumeCooldowns = new HashMap<>();
    private final Map<UUID, String> activeBags = new HashMap<>();

    public ConsumableListener(ItemManager itemManager, BoredomManager boredomManager, CookingManager cookingManager) {
        this.itemManager = itemManager;
        this.boredomManager = boredomManager;
        this.cookingManager = cookingManager;
    }

    @EventHandler
    public void onConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String itemId = itemManager.getItemId(item);
        if (itemId == null) return;

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        boolean consumed = false;

        // [ĐÃ SỬA] - Xóa Lúa Mì Tinh Tú khỏi danh sách ăn ngay
        List<String> instantFoods = Arrays.asList("fat_milk", "spice_honey", "veg_apple", "veg_melon",
                "veg_melon_uncommon", "veg_melon_rare", "veg_kelp_dried", "potion_digestion",
                "potion_anti_anorexia", "misc_lunch_bag", "starch_bread", "magic_flame_beetroot",
                "magic_golden_melon", "magic_crystal_kelp", "magic_mana_potato", "magic_chaos_glow_berry",
                "bag_seeds_common", "bag_seeds_magic", "misc_seed_pouch", "bag_fishing_rod_common",
                "bag_fishing_rod_uncommon", "bag_fishing_rod_rare",
                "bag_fishing_rod_epic", "bag_fishing_rod_mythic", "bag_fishing_rod_legendary",
                "bag_cloth", "bag_leather", "bag_dragon_scale", "misc_gold_scale", "misc_stable_expansion", "misc_mount_packaged");

        if (instantFoods.contains(itemId)) {
            event.setCancelled(true);
            player.updateInventory();

            long now = System.currentTimeMillis();
            if (consumeCooldowns.getOrDefault(player.getUniqueId(), 0L) > now) return;

            // ==========================================
            // LOGIC KÍCH HOẠT: CÂN TIỂU LY MẠ VÀNG
            // ==========================================
            if (itemId.equals("misc_gold_scale")) {
                ItemMeta meta = item.getItemMeta();
                org.bukkit.NamespacedKey expireKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "scale_expire_time");
                long expireTime = meta.getPersistentDataContainer().getOrDefault(expireKey, PersistentDataType.LONG, 0L);

                if (expireTime > now) {
                    player.sendMessage("§c§l[!] §cChiếc cân này đang trong thời gian hiệu lực!");
                    return; // Không làm gì cả
                }

                if (item.getAmount() > 1) {
                    player.sendMessage("§c§l[!] §cVui lòng tách Cân Tiểu Ly ra từng cái một để kích hoạt!");
                    return;
                }

                // Cài đặt thời gian (Hiện tại + 24 Giờ)
                long newExpire = now + (24 * 60 * 60 * 1000L);
                meta.getPersistentDataContainer().set(expireKey, PersistentDataType.LONG, newExpire);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                List<String> lore = meta.getLore();
                if (lore != null) {
                    lore.removeIf(line -> line.contains("Chưa kích hoạt")); // Xóa dòng cũ
                    lore.add("");
                    lore.add("§a§l✔ ĐANG HOẠT ĐỘNG");
                    lore.add("§fHết hạn lúc: §e" + sdf.format(new Date(newExpire)));
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);

                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
                player.sendMessage("§6§l[!] §aĐã kích hoạt Cân Tiểu Ly Mạ Vàng trong 24 giờ!");
                return; // Trả về ngay để không bị xóa item
            }

            // ==========================================
            // LOGIC MỞ TÚI THƯƠNG NHÂN (FILE YML + UUID)
            // ==========================================
            else if (itemId.equals("bag_cloth") || itemId.equals("bag_leather") || itemId.equals("bag_dragon_scale")) {
                int size = 18;
                String title = "§eTúi Vải Thô";
                if (itemId.equals("bag_leather")) { size = 36; title = "§6Túi Da Cao Cấp"; }
                else if (itemId.equals("bag_dragon_scale")) { size = 54; title = "§5Túi Vảy Rồng"; }

                ItemMeta meta = item.getItemMeta();
                org.bukkit.NamespacedKey uidKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "bag_uuid");
                String bagUuid = meta.getPersistentDataContainer().getOrDefault(uidKey, PersistentDataType.STRING, "");

                // NẾU TÚI CHƯA CÓ ID (Túi vừa mới tạo/mua) -> Random 1 cái ID và gắn vào túi
                if (bagUuid.isEmpty()) {
                    bagUuid = UUID.randomUUID().toString();
                    meta.getPersistentDataContainer().set(uidKey, PersistentDataType.STRING, bagUuid);
                    item.setItemMeta(meta);
                }

                // Ghi nhớ lại Player này đang mở túi có ID này
                activeBags.put(player.getUniqueId(), bagUuid);

                // Mở GUI và Load đồ từ File lên
                Inventory bagInv = Bukkit.createInventory(null, size, title);
                RPGCore.getInstance().getBackpackManager().loadBackpack(bagUuid, bagInv);

                player.openInventory(bagInv);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
                return;
            }

            // ==========================================
            // LOGIC KÍCH HOẠT: GIẤY PHÉP MỞ RỘNG CHUỒNG
            // ==========================================
            else if (itemId.equals("misc_stable_expansion")) {
                vn.minecraft.rPGCore.mounts.data.PlayerMountProfile profile = RPGCore.getInstance().getMountManager().getLoadedProfilesMap().get(player.getUniqueId());
                if (profile != null) {
                    int currentSlots = profile.getUnlockedSlots();
                    if (currentSlots >= 9) { // Giới hạn tối đa (Ví dụ 9 ô chuồng)
                        player.sendMessage("§c§l[!] §cBạn đã mở khóa tối đa số lượng Chuồng Thú Cưỡi!");
                        return; // Hủy, không tiêu hao item
                    }

                    profile.setUnlockedSlots(currentSlots + 1);
                    RPGCore.getInstance().getMountDataManager().saveProfile(player.getUniqueId());

                    player.sendMessage("§a§l[Chuồng Ngựa] §fBạn đã mở rộng thành công, hiện có thể chứa tối đa §e" + (currentSlots + 1) + " §fThú cưỡi!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    consumed = true;
                } else {
                    player.sendMessage("§cKhông tìm thấy dữ liệu Chuồng ngựa của bạn!");
                }
            }

            else if (itemId.equals("fat_milk")) {
                PotionEffectType[] debuffs = { PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.WEAKNESS, PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.DARKNESS, PotionEffectType.UNLUCK, PotionEffectType.BAD_OMEN };
                List<PotionEffectType> activeDebuffs = new ArrayList<>();
                for (PotionEffectType type : debuffs) if (player.hasPotionEffect(type)) activeDebuffs.add(type);

                if (!activeDebuffs.isEmpty()) {
                    player.removePotionEffect(activeDebuffs.get(new Random().nextInt(activeDebuffs.size())));
                    player.sendMessage("§f§l[!] §aBạn đã uống Sữa. 1 Hiệu ứng xấu đã bị thanh tẩy!");
                } else player.sendMessage("§f§l[!] §aBạn đã uống Sữa. Cơ thể hoàn toàn khỏe mạnh!");

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WANDERING_TRADER_DRINK_MILK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.BUCKET));
                consumed = true;
            }
            else if (itemId.equals("potion_digestion")) {
                double currentPts = boredomManager.getPoints(player.getUniqueId());
                boredomManager.reduceAnorexia(player.getUniqueId(), currentPts * 0.5);
                player.sendMessage("§e§l[!] §aBụng bạn nhẹ đi hẳn! Đã giảm 50% độ ngấy.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.GLASS_BOTTLE));
                consumed = true;
            }
            else if (itemId.equals("potion_anti_anorexia")) {
                boredomManager.unban(player.getUniqueId());
                boredomManager.reduceAnorexia(player.getUniqueId(), 100.0);
                player.sendMessage("§e§l[!] §aChứng biếng ăn đã bị đánh bay! Bạn lại thèm ăn rồi!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.BUCKET));
                consumed = true;
            }
            else if (itemId.equals("misc_lunch_bag")) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§cTúi đồ của bạn đã đầy!"); return;
                }
                for(int i=0; i<3; i++) player.getInventory().addItem(cookingManager.generateRandomSoup(player));
                player.sendMessage("§e§l[!] §aBạn đã mở Túi Cơm Hộp và nhận được 3 Bát Súp ngẫu nhiên!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                consumed = true;
            }
            else if (itemId.equals("starch_bread")) {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 8));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EAT, 1f, 1f);
                consumed = true;
            }
            else if (itemId.equals("spice_honey")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.05);
                player.sendMessage("§e§l[!] §aMật ong rừng giúp bạn phục hồi sinh lực (+5% HP)!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_HONEY_BOTTLE_DRINK, 1f, 1f);
                consumed = true;
            }
            else if (itemId.startsWith("veg_melon") || itemId.equals("veg_apple")) {
                double healPercent = 0.02;
                int foodAmount = 4;

                if (itemId.equals("veg_melon_uncommon")) {
                    healPercent = 0.03;
                    foodAmount = 6;
                } else if (itemId.equals("veg_melon_rare")) {
                    healPercent = 0.05;
                    foodAmount = 8;
                }

                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * healPercent);
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + foodAmount));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                consumed = true;
            }
            else if (itemId.equals("veg_kelp_dried")) {
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.SPEED, 100, 0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.5f);
                consumed = true;
            }
            else if (itemId.equals("magic_flame_beetroot")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.10);
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.FIRE_RESISTANCE, 1200, 0);
                player.setFireTicks(40);
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
                player.sendMessage("§c§l[!] §fCơ thể bạn nóng rực! Hồi 10% HP và nhận Kháng lửa.");
                consumed = true;
            }
            else if (itemId.equals("magic_golden_melon")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.15);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                player.sendMessage("§e§l[!] §fVị ngọt ma thuật tuyệt hảo! Hồi 15% HP.");
                consumed = true;
            }
            else if (itemId.equals("magic_crystal_kelp")) {
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.WATER_BREATHING, 3600, 0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GUARDIAN_AMBIENT, 1f, 1f);
                player.sendMessage("§b§l[!] §fMa lực nước bao bọc bạn! Có thể thở dưới nước 3 phút.");
                consumed = true;
            }
            else if (itemId.equals("magic_mana_potato")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.20);
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.BLINDNESS, 100, 0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 1f);
                player.sendMessage("§d§l[!] §fMa năng quá lớn làm bạn mù tạm thời, nhưng hồi 20% HP!");
                consumed = true;
            }
            else if (itemId.equals("magic_chaos_glow_berry")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.10);
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.INVISIBILITY, 600, 0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
                player.sendMessage("§5§l[!] §fCơ thể bạn hòa vào hư không! Nhận Tàng Hình trong 30 giây.");
                consumed = true;
            }

            else if (itemId.equals("bag_seeds_common")) {
                List<String> commons = Arrays.asList("seed_carrot", "seed_potato", "seed_beetroot", "seed_pumpkin", "seed_melon", "seed_wheat", "seed_sweet_berry", "seed_glow_berry", "seed_mushroom", "seed_red_mushroom", "seed_kelp", "seed_cocoa", "seed_cucumber");
                Random r = new Random();
                for (int i = 0; i < 16; i++) {
                    player.getInventory().addItem(itemManager.buildItem(commons.get(r.nextInt(commons.size())), 1));
                }
                player.sendMessage("§a§l[!] §fBạn đã khui Túi Hạt Giống Hỗn Hợp và nhận được 16 hạt ngẫu nhiên!");
                player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1f, 1f);
                consumed = true;
            }
            else if (itemId.equals("bag_seeds_magic")) {
                List<String> magics = Arrays.asList("seed_magic_golden_carrot", "seed_magic_flame_beetroot", "seed_magic_golden_melon", "seed_magic_glowing_mushroom", "seed_magic_star_wheat", "seed_magic_soul_sweet_berry", "seed_magic_crystal_kelp", "seed_magic_dragon_cocoa", "seed_magic_mana_potato", "seed_magic_chaos_glow_berry");
                Random r = new Random();
                int amt = 5 + r.nextInt(4);
                for (int i = 0; i < amt; i++) {
                    player.getInventory().addItem(itemManager.buildItem(magics.get(r.nextInt(magics.size())), 1));
                }
                player.sendMessage("§5§l[!] §fMa thuật bùng nổ! Nhận được " + amt + " hạt ma thuật ngẫu nhiên.");
                player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1f, 1.3f);
                consumed = true;
            }

            // ==========================================
            // [MỚI BỔ SUNG]: LOGIC MỞ TÚI GACHA CẦN CÂU
            // ==========================================
            else if (itemId.startsWith("bag_fishing_rod_")) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§cTúi đồ của bạn đã đầy, không thể mở thêm!");
                    return;
                }

                // Mẹo nhỏ: Đổi chữ "bag_" thành "tool_" để ra đúng ID của Cần câu tương ứng
                String targetRodId = itemId.replace("bag_", "tool_");

                // Khi gọi buildItem, ItemManager sẽ TỰ ĐỘNG đổ xúc xắc dòng Buff và Độ bền
                ItemStack dynamicRod = itemManager.buildItem(targetRodId, 1);

                if (dynamicRod != null) {
                    player.getInventory().addItem(dynamicRod);
                    player.sendMessage("§b§l[!] §fBạn đã mở túi và nhận được: " + dynamicRod.getItemMeta().getDisplayName());
                    player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1f, 1.2f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    consumed = true;
                } else {
                    player.sendMessage("§cLỗi hệ thống: Không thể khởi tạo Cần câu!");
                }
            }

            // ==========================================
            // LOGIC MỞ TÚI TRỮ HẠT (FILE YML + UUID + MIGRATION)
            // ==========================================
            else if (itemId.equals("misc_seed_pouch")) {
                Inventory pouchInv = Bukkit.createInventory(null, 27, "§2Túi Trữ Hạt");
                ItemMeta meta = item.getItemMeta();
                org.bukkit.NamespacedKey uidKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "bag_uuid");
                String bagUuid = meta.getPersistentDataContainer().getOrDefault(uidKey, PersistentDataType.STRING, "");

                // NẾU TÚI CHƯA CÓ ID -> Cấp ID mới và kiểm tra xem có dữ liệu cũ cần chuyển đổi không
                if (bagUuid.isEmpty()) {
                    bagUuid = UUID.randomUUID().toString();
                    meta.getPersistentDataContainer().set(uidKey, PersistentDataType.STRING, bagUuid);

                    // --- MIGRATION: Cứu dữ liệu hạt giống cũ ---
                    org.bukkit.NamespacedKey oldContentKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "pouch_content");
                    String oldRawData = meta.getPersistentDataContainer().getOrDefault(oldContentKey, PersistentDataType.STRING, "");

                    if (!oldRawData.isEmpty()) {
                        String[] parts = oldRawData.split("\\|");
                        for (String part : parts) {
                            if (part.isEmpty()) continue;
                            String[] tokens = part.split(":");
                            int slotIdx = Integer.parseInt(tokens[0]);
                            String id = tokens[1];
                            int amount = Integer.parseInt(tokens[2]);
                            ItemStack seedItem = itemManager.buildItem(id, amount);
                            if (seedItem != null) pouchInv.setItem(slotIdx, seedItem);
                        }
                        // Xóa data NBT cũ đi cho nhẹ túi
                        meta.getPersistentDataContainer().remove(oldContentKey);
                    }
                    item.setItemMeta(meta);
                } else {
                    // Nếu túi đã có ID chuẩn thì load từ File YML
                    RPGCore.getInstance().getBackpackManager().loadBackpack(bagUuid, pouchInv);
                }

                // Ghi nhớ Player đang mở túi này
                activeBags.put(player.getUniqueId(), bagUuid);

                player.openInventory(pouchInv);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
                return;
            }

            // ==========================================
            // LOGIC MỞ GÓI THÚ CƯỠI VÀO CHUỒNG (GIẢI NÉN)
            // ==========================================
            else if (itemId.equals("misc_mount_packaged")) {
                vn.minecraft.rPGCore.mounts.data.PlayerMountProfile profile = RPGCore.getInstance().getMountManager().getProfile(player.getUniqueId());

                // Tìm slot trống ưu tiên từ 1 -> 2 -> 3
                int emptySlot = -1;
                for (int i = 1; i <= 3; i++) {
                    if (profile.getUnlockedSlots() >= i && !profile.hasMountAt(i)) {
                        emptySlot = i;
                        break;
                    }
                }

                if (emptySlot == -1) {
                    player.sendMessage("§c§l[!] §cTất cả các ô chuồng ngựa của bạn đã đầy hoặc chưa được mở khóa!");
                    return; // Hủy, không tiêu hao item
                }

                // Đọc dữ liệu từ NBT của thẻ bài
                ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();

                String archStr = data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_arch"), PersistentDataType.STRING, "HORSE");
                vn.minecraft.rPGCore.mounts.type.MountArchetype arch = vn.minecraft.rPGCore.mounts.type.MountArchetype.valueOf(archStr);
                vn.minecraft.rPGCore.mounts.data.MountData mountData = new vn.minecraft.rPGCore.mounts.data.MountData(arch);

                mountData.setTierSpeed(data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_sp"), PersistentDataType.INTEGER, 1));
                mountData.setTierJump(data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_jp"), PersistentDataType.INTEGER, 1));
                mountData.setTierDurability(data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_hp"), PersistentDataType.INTEGER, 1));
                mountData.setTierCooldown(data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_cd"), PersistentDataType.INTEGER, 1));
                mountData.setTierStorage(data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_st"), PersistentDataType.INTEGER, 1));
                mountData.setInventoryBase64(data.getOrDefault(new org.bukkit.NamespacedKey(RPGCore.getInstance(), "mount_inv"), PersistentDataType.STRING, ""));

                // Lưu vào chuồng
                profile.setMountAt(emptySlot, mountData);
                RPGCore.getInstance().getMountDataManager().saveProfile(player.getUniqueId());

                player.sendMessage("§a§l[Chuồng Ngựa] §fĐã thêm §e" + arch.getDisplayName() + " §fvào ô chuồng số §b" + emptySlot + "!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_HORSE_ARMOR, 1f, 1f);
                consumed = true;
            }

            if (consumed) consumeCooldowns.put(player.getUniqueId(), now + 1500L);
        }

        else if (itemId.equals("potion_hp_1")) {
            double healAmount = rpgPlayer.getTotalMaxHealth() * 0.10;
            rpgPlayer.heal(healAmount);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            consumed = true;
        }
        else if (itemId.startsWith("crystal_")) {
            if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("mage")) {
                player.sendMessage("§cChỉ Pháp Sư mới có thể hấp thụ Tinh Thể Nguyên Tố!"); return;
            }
            String elementToUnlock = itemId.replace("crystal_", "").toUpperCase();
            if (rpgPlayer.hasElement(elementToUnlock)) {
                player.sendMessage("§eBạn đã sở hữu Nguyên Tố này rồi!"); return;
            }
            rpgPlayer.addElement(elementToUnlock);
            player.sendMessage("§a§l[!] §fBạn đã hấp thụ thành công §dNguyên Tố " + elementToUnlock + "§f!");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            consumed = true;
        }
        else if (itemId.startsWith("bag_weapon_") || itemId.startsWith("bag_armor_")) {
            String type = itemId.startsWith("bag_weapon_") ? "weapon" : "armor";
            String typeName = type.equals("weapon") ? "Vũ Khí" : "Phòng Cụ";
            int tierIndex = 0;
            if (itemId.endsWith("uncommon")) tierIndex = 1;
            else if (itemId.endsWith("rare")) tierIndex = 2;
            else if (itemId.endsWith("epic")) tierIndex = 3;

            int playerLevel = player.getLevel();
            if (playerLevel <= 0) playerLevel = 1;

            player.sendMessage("§a§l[!] §fĐang mở Túi " + typeName + "...");
            ItemStack gachaItem = vn.minecraft.rPGCore.commands.EquipmentCommand.createGachaItem(type, playerLevel, null, tierIndex, null, -1, -1);
            player.getInventory().addItem(gachaItem);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            consumed = true;
        }

        if (consumed) {
            event.setCancelled(true);
            player.updateInventory();
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                EquipmentSlot hand = event.getHand();
                if (hand == EquipmentSlot.HAND) item.setAmount(item.getAmount() - 1);
                else if (hand == EquipmentSlot.OFF_HAND) {
                    ItemStack offHandItem = player.getInventory().getItemInOffHand();
                    offHandItem.setAmount(offHandItem.getAmount() - 1);
                }
            }
        }
    }

    // ==========================================
    // CHẶN BỎ TÚI VÀO TRONG TÚI
    // ==========================================
    @EventHandler
    public void onPouchInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isSeedPouch = title.equals("§2Túi Trữ Hạt");
        boolean isMerchantBag = title.equals("§eTúi Vải Thô") || title.equals("§6Túi Da Cao Cấp") || title.equals("§5Túi Vảy Rồng");

        if (!isSeedPouch && !isMerchantBag) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Danh sách các túi bị cấm nhét vào nhau (Chống Inception bug)
        List<String> blockedBags = Arrays.asList("misc_seed_pouch", "bag_cloth", "bag_leather", "bag_dragon_scale");

        if (current != null && blockedBags.contains(itemManager.getItemId(current))) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).updateInventory();
            return;
        }
        if (cursor != null && blockedBags.contains(itemManager.getItemId(cursor))) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).updateInventory();
            return;
        }

        // Logic cũ cho Túi Hạt Giống
        if (isSeedPouch) {
            if (event.getRawSlot() < 27) {
                if (cursor != null && cursor.getType() != Material.AIR) {
                    String cId = itemManager.getItemId(cursor);
                    if (cId == null || !cId.startsWith("seed_")) {
                        event.setCancelled(true);
                        ((Player) event.getWhoClicked()).updateInventory();
                        event.getWhoClicked().sendMessage("§cTúi Trữ Hạt chỉ chấp nhận lưu trữ các loại hạt giống nông nghiệp!");
                    }
                }
            } else {
                if (event.getClick().isShiftClick() && current != null && current.getType() != Material.AIR) {
                    String cId = itemManager.getItemId(current);
                    if (cId == null || !cId.startsWith("seed_")) {
                        event.setCancelled(true);
                        ((Player) event.getWhoClicked()).updateInventory();
                        event.getWhoClicked().sendMessage("§cTúi Trữ Hạt chỉ chấp nhận lưu trữ các loại hạt giống nông nghiệp!");
                    }
                }
            }
        }
    }

    // ==========================================
    // LƯU DỮ LIỆU KHI ĐÓNG TÚI (Dùng chung cho TẤT CẢ các túi)
    // ==========================================
    @EventHandler
    public void onPouchInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        boolean isSeedPouch = title.equals("§2Túi Trữ Hạt");
        boolean isMerchantBag = title.equals("§eTúi Vải Thô") || title.equals("§6Túi Da Cao Cấp") || title.equals("§5Túi Vảy Rồng");

        // Nếu không phải là túi của RPGCore thì bỏ qua
        if (!isSeedPouch && !isMerchantBag) return;

        Player player = (Player) event.getPlayer();

        // Lấy UUID của túi mà người chơi này đang mở ra
        String bagUuid = activeBags.remove(player.getUniqueId());

        // Nếu hợp lệ, lưu thẳng đồ trong GUI xuống file YML
        if (bagUuid != null) {
            RPGCore.getInstance().getBackpackManager().saveBackpack(bagUuid, event.getInventory());
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
        }
    }
}