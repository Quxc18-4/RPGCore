package vn.minecraft.rPGCore.jobs.events;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import vn.minecraft.rPGCore.jobs.types.MerchantJob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MerchantListener implements Listener {

    private final RPGCore plugin;
    // Danh sách các quái vật hình người
    private final List<EntityType> humanoids = Arrays.asList(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER_SKELETON,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.ILLUSIONER, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.WITCH, EntityType.DROWNED, EntityType.HUSK
    );

    public MerchantListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // NỘI TẠI 4: MÙI TIỀN & ĐẶC QUYỀN BẠCH KIM & CÂN TIỂU LY
    // ==========================================
    @EventHandler
    public void onHumanoidKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();

        // Chỉ áp dụng cho quái vật của MythicMobs (Bỏ qua quái Vanilla)
        if (!MythicBukkit.inst().getAPIHelper().isMythicMob(event.getEntity())) {
            return;
        }

        PlayerJobData data = JobManager.getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        int level = data.getLevel("MERCHANT");
        if (level <= 0) return;

        // 1. Kiểm tra Ultimate 3: Đặc Quyền Bạch Kim
        if (MerchantJob.triggerPlatinumTouch(player)) {
            ItemStack platItem = plugin.getCurrencyManager().getCurrencyItem("gold", 64);
            event.getDrops().add(platItem);
            player.sendMessage("§b§l[!] §fTiếng xèng lanh lảnh vang lên... Bạn vừa tìm thấy kho báu Bạch Kim!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        }

        // 2. Tính toán tỉ lệ rớt Đồng/Bạc
        double copperChance = MerchantJob.getCopperDropChance(level);
        double silverChance = MerchantJob.getSilverDropChance(level);

        // 3. Kiểm tra Buff từ Cân Tiểu Ly Mạ Vàng
        boolean hasScaleBuff = false;
        NamespacedKey scaleKey = new NamespacedKey(plugin, "scale_expire_time");
        long now = System.currentTimeMillis();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && "misc_gold_scale".equals(plugin.getItemManager().getItemId(item))) {
                long expire = item.getItemMeta().getPersistentDataContainer().getOrDefault(scaleKey, PersistentDataType.LONG, 0L);

                if (expire > now) {
                    hasScaleBuff = true;
                    break;
                } else if (expire > 0 && expire <= now) {
                    item.setAmount(0);
                    player.sendMessage("§c§l[!] §cCân Tiểu Ly Mạ Vàng của bạn đã quá hạn 24 giờ và bị hỏng mất rồi!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }

        if (hasScaleBuff) {
            copperChance *= 1.5;
            silverChance *= 1.5;
        }

        // 4. Roll xúc xắc rớt tiền
        double roll = Math.random() * 100.0;
        if (roll <= silverChance) {
            event.getDrops().add(plugin.getCurrencyManager().getCurrencyItem("iron", 1));
        } else if (roll <= copperChance) {
            int amount = 1 + (int)(Math.random() * 3);
            event.getDrops().add(plugin.getCurrencyManager().getCurrencyItem("copper", amount));
        }
    }

    // ==========================================
    // NỘI TẠI 3: LÃI SUẤT TÍN DỤNG (ĐÃ FIX LỖI ĐĂNG NHẬP)
    // ==========================================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Tạo vòng lặp chờ người chơi gõ /login thành công
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // Nếu người chơi out game giữa chừng thì hủy vòng lặp
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Lấy dữ liệu RPGPlayer. Nếu nó khác null tức là đã đăng nhập xong!
                vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = plugin.getPlayerManager().getRPGPlayer(player.getUniqueId());

                if (rpgPlayer != null) {
                    this.cancel(); // Đã có dữ liệu -> Dừng vòng lặp chờ

                    // === BẮT ĐẦU LOGIC CỘNG LÃI SUẤT ===
                    PlayerJobData data = JobManager.getPlayerData(player);
                    if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

                    double interestRate = MerchantJob.getBankInterestRate(player);
                    if (interestRate <= 0) return;

                    NamespacedKey interestKey = new NamespacedKey(plugin, "merchant_last_interest_day");
                    int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                    int lastInterestDay = player.getPersistentDataContainer().getOrDefault(interestKey, org.bukkit.persistence.PersistentDataType.INTEGER, -1);

                    if (currentDay != lastInterestDay) {
                        // Lúc này gọi getBalance() thoải mái vì rpgPlayer chắc chắn không còn null nữa!
                        long currentBalance = rpgPlayer.getBalance();

                        if (currentBalance > 0) {
                            long interestAmount = (long) (currentBalance * interestRate);
                            if (interestAmount > 0) {
                                rpgPlayer.addBalance(interestAmount);
                                player.getPersistentDataContainer().set(interestKey, org.bukkit.persistence.PersistentDataType.INTEGER, currentDay);
                                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_INTEREST, 1);

                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    player.sendMessage("§e§l[Ngân Hàng] §fTiền đẻ ra tiền! Bạn vừa nhận được §a" + plugin.getCurrencyManager().formatCurrency(interestAmount) + " §ftiền lãi dựa trên số dư hiện tại.");
                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                                }, 60L);
                            }
                        }
                    }
                    // === KẾT THÚC LOGIC ===
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Cứ mỗi 1 giây (20 ticks) kiểm tra 1 lần
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        PlayerJobData data = JobManager.getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_MOVE, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractMount(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }
        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId == null) {
            return;
        }
        if (!itemId.equals("misc_mount_retrieval") && !itemId.startsWith("misc_mount_upgrade_")) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isInsideVehicle()) {
                player.leaveVehicle();
            }
        });

        Entity activeMount = plugin.getMountManager().getActiveMount(player);
        if (activeMount == null || !activeMount.getUniqueId().equals(clicked.getUniqueId())) {
            player.sendMessage("§c§l[!] §cĐây không phải thú cưỡi đang được triệu hồi của bạn!");
            return;
        }

        NamespacedKey slotKey = new NamespacedKey(plugin, "mount_slot");
        Integer activeSlot = clicked.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
        if (activeSlot == null) {
            player.sendMessage("§c§l[!] §cKhông thể xác định dữ liệu thú cưỡi!");
            return;
        }

        vn.minecraft.rPGCore.mounts.data.PlayerMountProfile profile = plugin.getMountManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        vn.minecraft.rPGCore.mounts.data.MountData mount = profile.getMountAt(activeSlot);
        if (mount == null) {
            player.sendMessage("§c§l[!] §cDữ liệu thú cưỡi không tồn tại!");
            return;
        }

        // 1. LOGIC ĐÓNG GÓI
        if (itemId.equals("misc_mount_retrieval")) {
            ItemStack card = plugin.getItemManager().buildItem("misc_mount_packaged", 1);
            if (card == null) {
                player.sendMessage("§cKhông thể tạo thẻ đóng gói!");
                return;
            }
            ItemMeta meta = card.getItemMeta();
            if (meta == null) return;
            PersistentDataContainer pda = meta.getPersistentDataContainer();

            pda.set(new NamespacedKey(plugin, "mount_arch"), PersistentDataType.STRING, mount.getType().name());
            pda.set(new NamespacedKey(plugin, "mount_sp"), PersistentDataType.INTEGER, mount.getTierSpeed());
            pda.set(new NamespacedKey(plugin, "mount_jp"), PersistentDataType.INTEGER, mount.getTierJump());
            pda.set(new NamespacedKey(plugin, "mount_hp"), PersistentDataType.INTEGER, mount.getTierDurability());
            pda.set(new NamespacedKey(plugin, "mount_cd"), PersistentDataType.INTEGER, mount.getTierCooldown());
            pda.set(new NamespacedKey(plugin, "mount_st"), PersistentDataType.INTEGER, mount.getTierStorage());
            if (mount.getInventoryBase64() != null && !mount.getInventoryBase64().isEmpty()) {
                pda.set(new NamespacedKey(plugin, "mount_inv"), PersistentDataType.STRING, mount.getInventoryBase64());
            }

            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) lore.addAll(meta.getLore());
            lore.add("§8§m-------------------------");
            lore.add("§fGiống loài: §6§l" + mount.getType().getDisplayName());
            lore.add("");
            lore.add("§fThông số phôi đóng gói:");
            lore.add(" §8- §fTốc độ: §b" + mount.getTierSpeed() + " §8/ " + mount.getType().getMaxSpeed());
            lore.add(" §8- §fSức nhảy: §a" + mount.getTierJump() + " §8/ " + mount.getType().getMaxJump());
            lore.add(" §8- §fSức bền: §c" + mount.getTierDurability() + " §8/ " + mount.getType().getMaxDurability());
            lore.add(" §8- §fHồi chiêu: §e" + mount.getTierCooldown() + " §8/ " + mount.getType().getMaxCooldown());
            lore.add(" §8- §fKho chứa: §6" + mount.getTierStorage() + " §8/ " + mount.getType().getMaxStorage());
            lore.add("");
            if (mount.getInventoryBase64() != null && !mount.getInventoryBase64().isEmpty()) {
                lore.add("§d§l[!] Thú cưỡi này đang mang theo hành lý!");
            } else {
                lore.add("§7§o(Túi đồ thú cưỡi trống không)");
            }
            lore.add("§8§m-------------------------");
            lore.add("§a► Click phải để giải nén vào Chuồng Ngựa!");
            meta.setLore(lore);
            card.setItemMeta(meta);

            player.getInventory().addItem(card);
            profile.setMountAt(activeSlot, null);

            // [SỬA LỖI Ở ĐÂY]: GỌI LƯU NGẦM (ASYNC) VÀ KHÔNG XÓA RAM
            plugin.getMountDataManager().saveProfile(player.getUniqueId(), true, false);

            plugin.getMountManager().despawnMount(player, false);
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a§l[!] §fĐã thu hồi và đóng gói thành công!");
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
        }
        // 2. LOGIC NÂNG CẤP
        else if (itemId.startsWith("misc_mount_upgrade_")) {
            boolean success = false;
            String type = itemId.replace("misc_mount_upgrade_", "");
            if (type.equals("speed") && mount.getTierSpeed() < mount.getType().getMaxSpeed()) {
                mount.setTierSpeed(mount.getTierSpeed() + 1);
                success = true;
            }
            else if (type.equals("jump") && mount.getTierJump() < mount.getType().getMaxJump()) {
                mount.setTierJump(mount.getTierJump() + 1);
                success = true;
            }
            else if (type.equals("durability") && mount.getTierDurability() < mount.getType().getMaxDurability()) {
                mount.setTierDurability(mount.getTierDurability() + 1);
                success = true;
            }
            else if (type.equals("cooldown") && mount.getTierCooldown() < mount.getType().getMaxCooldown()) {
                mount.setTierCooldown(mount.getTierCooldown() + 1);
                success = true;
            }
            else if (type.equals("storage") && mount.getTierStorage() < mount.getType().getMaxStorage()) {
                mount.setTierStorage(mount.getTierStorage() + 1);
                success = true;
            }
            if (success) {
                // [SỬA LỖI Ở ĐÂY]: GỌI LƯU NGẦM (ASYNC) VÀ KHÔNG XÓA RAM
                plugin.getMountDataManager().saveProfile(player.getUniqueId(), true, false);

                item.setAmount(item.getAmount() - 1);
                player.sendMessage("§e§l[!] §fĐã dùng Bùa Ép Xung để tăng 1 Bậc chỉ số cho Thú Cưỡi!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            } else {
                player.sendMessage("§c§l[!] §cChỉ số này đã đạt cấp tối đa!");
            }
        }
    }
}