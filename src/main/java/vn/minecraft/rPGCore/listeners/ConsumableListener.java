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
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.BoredomManager;
import vn.minecraft.rPGCore.managers.CookingManager;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.*;

public class ConsumableListener implements Listener {

    private final ItemManager itemManager;
    private final BoredomManager boredomManager;
    private final CookingManager cookingManager;
    private final Map<UUID, Long> consumeCooldowns = new HashMap<>();

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
        List<String> instantFoods = Arrays.asList("fat_milk", "spice_honey", "veg_apple", "veg_melon", "veg_melon_uncommon", "veg_melon_rare", "veg_kelp_dried", "potion_digestion", "potion_anti_anorexia", "misc_lunch_bag", "starch_bread", "magic_flame_beetroot", "magic_golden_melon", "magic_crystal_kelp", "magic_mana_potato", "magic_chaos_glow_berry", "bag_seeds_common", "bag_seeds_magic", "misc_seed_pouch");

        if (instantFoods.contains(itemId)) {
            event.setCancelled(true);

            long now = System.currentTimeMillis();
            if (consumeCooldowns.getOrDefault(player.getUniqueId(), 0L) > now) return;

            if (itemId.equals("fat_milk")) {
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

            else if (itemId.equals("misc_seed_pouch")) {
                Inventory pouchInv = Bukkit.createInventory(null, 27, "§2Túi Trữ Hạt");
                ItemMeta meta = item.getItemMeta();
                org.bukkit.NamespacedKey contentKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "pouch_content");
                String rawData = meta.getPersistentDataContainer().getOrDefault(contentKey, PersistentDataType.STRING, "");

                if (!rawData.isEmpty()) {
                    String[] parts = rawData.split("\\|");
                    for (String part : parts) {
                        if (part.isEmpty()) continue;
                        String[] tokens = part.split(":");
                        int slotIdx = Integer.parseInt(tokens[0]);
                        String id = tokens[1];
                        int amount = Integer.parseInt(tokens[2]);
                        ItemStack seedItem = itemManager.buildItem(id, amount);
                        if (seedItem != null) pouchInv.setItem(slotIdx, seedItem);
                    }
                }
                player.openInventory(pouchInv);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
                return;
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

    @EventHandler
    public void onPouchInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§2Túi Trữ Hạt")) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (current != null && "misc_seed_pouch".equals(itemManager.getItemId(current))) { event.setCancelled(true); return; }
        if (cursor != null && "misc_seed_pouch".equals(itemManager.getItemId(cursor))) { event.setCancelled(true); return; }

        if (event.getRawSlot() < 27) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                String cId = itemManager.getItemId(cursor);
                if (cId == null || !cId.startsWith("seed_")) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§cTúi Trữ Hạt chỉ chấp nhận lưu trữ các loại hạt giống nông nghiệp!");
                    return;
                }
            }
        } else {
            if (event.getClick().isShiftClick() && current != null && current.getType() != Material.AIR) {
                String cId = itemManager.getItemId(current);
                if (cId == null || !cId.startsWith("seed_")) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§cTúi Trữ Hạt chỉ chấp nhận lưu trữ các loại hạt giống nông nghiệp!");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPouchInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("§2Túi Trữ Hạt")) return;

        Player player = (Player) event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String mId = itemManager.getItemId(mainHand);

        ItemStack pouchItem = null;
        if ("misc_seed_pouch".equals(mId)) pouchItem = mainHand;
        else {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if ("misc_seed_pouch".equals(itemManager.getItemId(offHand))) pouchItem = offHand;
        }

        if (pouchItem == null) return;

        StringBuilder sb = new StringBuilder();
        Inventory inv = event.getInventory();
        for (int i = 0; i < 27; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            String id = itemManager.getItemId(item);
            if (id != null) {
                sb.append(i).append(":").append(id).append(":").append(item.getAmount()).append("|");
            }
        }

        ItemMeta meta = pouchItem.getItemMeta();
        org.bukkit.NamespacedKey contentKey = new org.bukkit.NamespacedKey(RPGCore.getInstance(), "pouch_content");
        meta.getPersistentDataContainer().set(contentKey, PersistentDataType.STRING, sb.toString());
        pouchItem.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
    }
}