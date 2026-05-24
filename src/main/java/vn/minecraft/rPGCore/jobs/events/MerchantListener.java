package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.types.MerchantJob;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;

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

        // [SỬA ĐỔI]: Chỉ áp dụng cho quái vật của MythicMobs (Bỏ qua quái Vanilla)
        if (!io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().isMythicMob(event.getEntity())) {
            return;
        }

        PlayerJobData data = JobManager.getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        int level = data.getLevel("MERCHANT");
        if (level <= 0) return;

        // 1. Kiểm tra Ultimate 3: Đặc Quyền Bạch Kim
        if (MerchantJob.triggerPlatinumTouch(player)) {
            // Rớt thẳng 64 Vàng ra đất (Tương đương 1 Bạch Kim)
            ItemStack platItem = plugin.getCurrencyManager().getCurrencyItem("gold", 64);
            event.getDrops().add(platItem);
            player.sendMessage("§b§l[!] §fTiếng xèng lanh lảnh vang lên... Bạn vừa tìm thấy kho báu Bạch Kim!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        }

        // 2. Tính toán tỉ lệ rớt Đồng/Bạc (Nội tại Mùi Tiền)
        double copperChance = MerchantJob.getCopperDropChance(level);
        double silverChance = MerchantJob.getSilverDropChance(level);

        // 3. Kiểm tra Buff từ Cân Tiểu Ly Mạ Vàng trong túi đồ (Có Lazy Cleanup)
        boolean hasScaleBuff = false;
        NamespacedKey scaleKey = new NamespacedKey(plugin, "scale_expire_time");
        long now = System.currentTimeMillis();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && "misc_gold_scale".equals(plugin.getItemManager().getItemId(item))) {
                long expire = item.getItemMeta().getPersistentDataContainer().getOrDefault(scaleKey, PersistentDataType.LONG, 0L);

                if (expire > now) {
                    hasScaleBuff = true; // Còn hạn -> Kích hoạt buff
                    break;
                } else if (expire > 0 && expire <= now) {
                    // Cân đã hết hạn -> Bẻ gãy Cân
                    item.setAmount(0);
                    player.sendMessage("§c§l[!] §cCân Tiểu Ly Mạ Vàng của bạn đã quá hạn 24 giờ và bị hỏng mất rồi!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }

        if (hasScaleBuff) {
            copperChance *= 1.5; // Tăng 50% tỉ lệ rớt Đồng
            silverChance *= 1.5; // Tăng 50% tỉ lệ rớt Bạc
        }

        // 4. Roll xúc xắc rớt tiền
        double roll = Math.random() * 100.0;
        if (roll <= silverChance) {
            event.getDrops().add(plugin.getCurrencyManager().getCurrencyItem("iron", 1));
        } else if (roll <= copperChance) {
            int amount = 1 + (int)(Math.random() * 3); // Rớt từ 1-3 đồng
            event.getDrops().add(plugin.getCurrencyManager().getCurrencyItem("copper", amount));
        }
    }

    // ==========================================
    // NỘI TẠI 3: LÃI SUẤT TÍN DỤNG (Kiểm tra khi Login)
    // ==========================================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerJobData data = JobManager.getPlayerData(player);
        if (!data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        double interestRate = MerchantJob.getBankInterestRate(player);
        if (interestRate <= 0) return;

        // Kiểm tra biến lưu ngày nhận lãi gần nhất (Sử dụng PersistentDataContainer của Bukkit cho an toàn)
        NamespacedKey interestKey = new NamespacedKey(plugin, "merchant_last_interest_day");
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int lastInterestDay = player.getPersistentDataContainer().getOrDefault(interestKey, PersistentDataType.INTEGER, -1);

        if (currentDay != lastInterestDay) {
            vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer = plugin.getPlayerManager().getRPGPlayer(player.getUniqueId());
            long currentBalance = rpgPlayer.getBalance();

            if (currentBalance > 0) {
                // Tính lãi và cộng vào Ngân hàng
                long interestAmount = (long) (currentBalance * interestRate);
                if (interestAmount > 0) {
                    rpgPlayer.addBalance(interestAmount);

                    // Lưu lại ngày đã nhận
                    player.getPersistentDataContainer().set(interestKey, PersistentDataType.INTEGER, currentDay);

                    // Cập nhật tiến độ Nhiệm vụ Tuần (Lãi Mẹ Đẻ Lãi Con)
                    vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_INTEREST, 1);

                    // Gửi thông báo
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        player.sendMessage("§e§l[Ngân Hàng] §fTiền đẻ ra tiền! Bạn vừa nhận được §a" + plugin.getCurrencyManager().formatCurrency(interestAmount) + " §ftiền lãi dựa trên số dư hiện tại.");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    }, 60L); // Delay 3 giây sau khi vào server mới thông báo
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        // Chỉ check khi người chơi di chuyển sang block mới (tránh spam event)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        PlayerJobData data = JobManager.getPlayerData(player);
        if (data == null || !data.getJobId().equalsIgnoreCase("MERCHANT")) return;

        // Tăng tiến độ nhiệm vụ di chuyển 1 block
        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.MERCHANT_MOVE, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractMount(PlayerInteractEntityEvent event) {
        // =========================================================
        // FIX DOUBLE EVENT OFFHAND (1.21+)
        // =========================================================
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();
        // =========================================================
        // VALIDATE ITEM
        // =========================================================
        if (item == null
                || item.getType() == Material.AIR
                || !item.hasItemMeta()) {
            return;
        }
        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId == null) {
            return;
        }
        // Chỉ xử lý item mount
        if (!itemId.equals("misc_mount_retrieval")
                && !itemId.startsWith("misc_mount_upgrade_")) {
            return;
        }
        // =========================================================
        // HỦY TƯƠNG TÁC NGAY LẬP TỨC
        // FIX BUG NGỒI LÊN THÚ Ở 1.21.1
        // =========================================================
        event.setCancelled(true);
        // Force dismount nếu Paper xử lý vehicle trước listener
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isInsideVehicle()) {
                player.leaveVehicle();
            }
        });
        // =========================================================
        // CHECK ĐÂY CÓ PHẢI ACTIVE MOUNT CỦA PLAYER KHÔNG
        // =========================================================
        Entity activeMount = plugin.getMountManager().getActiveMount(player);
        if (activeMount == null || !activeMount.getUniqueId().equals(clicked.getUniqueId())) {
            player.sendMessage("§c§l[!] §cĐây không phải thú cưỡi đang được triệu hồi của bạn!");
            return;
        }
        // =========================================================
        // LẤY SLOT TỪ PDC
        // =========================================================
        NamespacedKey slotKey = new NamespacedKey(plugin, "mount_slot");
        Integer activeSlot = clicked.getPersistentDataContainer()
                .get(slotKey, PersistentDataType.INTEGER);
        if (activeSlot == null) {
            player.sendMessage("§c§l[!] §cKhông thể xác định dữ liệu thú cưỡi!");
            return;
        }
        // =========================================================
        // LOAD PROFILE + MOUNT DATA
        // =========================================================
        vn.minecraft.rPGCore.mounts.data.PlayerMountProfile profile =
                plugin.getMountManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        vn.minecraft.rPGCore.mounts.data.MountData mount =
                profile.getMountAt(activeSlot);
        if (mount == null) {
            player.sendMessage("§c§l[!] §cDữ liệu thú cưỡi không tồn tại!");
            return;
        }
        // =========================================================
        // 1. LOGIC ĐÓNG GÓI
        // =========================================================
        if (itemId.equals("misc_mount_retrieval")) {
            ItemStack card = plugin.getItemManager()
                    .buildItem("misc_mount_packaged", 1);
            if (card == null) {
                player.sendMessage("§cKhông thể tạo thẻ đóng gói!");
                return;
            }
            ItemMeta meta = card.getItemMeta();
            if (meta == null) {
                return;
            }
            PersistentDataContainer pda = meta.getPersistentDataContainer();
            // =========================================================
            // SAVE DATA
            // =========================================================
            pda.set(new NamespacedKey(plugin, "mount_arch"),
                    PersistentDataType.STRING,
                    mount.getType().name());
            pda.set(new NamespacedKey(plugin, "mount_sp"),
                    PersistentDataType.INTEGER,
                    mount.getTierSpeed());
            pda.set(new NamespacedKey(plugin, "mount_jp"),
                    PersistentDataType.INTEGER,
                    mount.getTierJump());
            pda.set(new NamespacedKey(plugin, "mount_hp"),
                    PersistentDataType.INTEGER,
                    mount.getTierDurability());
            pda.set(new NamespacedKey(plugin, "mount_cd"),
                    PersistentDataType.INTEGER,
                    mount.getTierCooldown());
            pda.set(new NamespacedKey(plugin, "mount_st"),
                    PersistentDataType.INTEGER,
                    mount.getTierStorage());
            if (mount.getInventoryBase64() != null
                    && !mount.getInventoryBase64().isEmpty()) {
                pda.set(new NamespacedKey(plugin, "mount_inv"),
                        PersistentDataType.STRING,
                        mount.getInventoryBase64());
            }
            // =========================================================
            // BUILD LORE
            // =========================================================
            List<String> lore = new ArrayList<>();
            if (meta.hasLore()) {
                lore.addAll(meta.getLore());
            }
            lore.add("§8§m-------------------------");
            lore.add("§fGiống loài: §6§l" + mount.getType().getDisplayName());
            lore.add("");
            lore.add("§fThông số phôi đóng gói:");
            lore.add(" §8- §fTốc độ: §b"
                    + mount.getTierSpeed()
                    + " §8/ "
                    + mount.getType().getMaxSpeed());
            lore.add(" §8- §fSức nhảy: §a"
                    + mount.getTierJump()
                    + " §8/ "
                    + mount.getType().getMaxJump());
            lore.add(" §8- §fSức bền: §c"
                    + mount.getTierDurability()
                    + " §8/ "
                    + mount.getType().getMaxDurability());
            lore.add(" §8- §fHồi chiêu: §e"
                    + mount.getTierCooldown()
                    + " §8/ "
                    + mount.getType().getMaxCooldown());
            lore.add(" §8- §fKho chứa: §6"
                    + mount.getTierStorage()
                    + " §8/ "
                    + mount.getType().getMaxStorage());
            lore.add("");
            if (mount.getInventoryBase64() != null
                    && !mount.getInventoryBase64().isEmpty()) {

                lore.add("§d§l[!] Thú cưỡi này đang mang theo hành lý!");
            } else {
                lore.add("§7§o(Túi đồ thú cưỡi trống không)");
            }
            lore.add("§8§m-------------------------");
            lore.add("§a► Click phải để giải nén vào Chuồng Ngựa!");
            meta.setLore(lore);
            card.setItemMeta(meta);
            // =========================================================
            // GIVE ITEM
            // =========================================================
            player.getInventory().addItem(card);
            // XÓA MOUNT KHỎI PROFILE
            profile.setMountAt(activeSlot, null);
            // SAVE DATA
            plugin.getMountDataManager().saveProfile(player.getUniqueId());
            // DESPAWN ACTIVE MOUNT
            plugin.getMountManager().despawnMount(player, false);
            // TRỪ ITEM THU HỒI
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a§l[!] §fĐã thu hồi và đóng gói thành công!");
            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_ILLUSIONER_CAST_SPELL,
                    1f,
                    1f
            );
        }
        // =========================================================
        // 2. LOGIC NÂNG CẤP
        // =========================================================
        else if (itemId.startsWith("misc_mount_upgrade_")) {
            boolean success = false;
            String type = itemId.replace("misc_mount_upgrade_", "");
            if (type.equals("speed")
                    && mount.getTierSpeed() < mount.getType().getMaxSpeed()) {
                mount.setTierSpeed(mount.getTierSpeed() + 1);
                success = true;
            }
            else if (type.equals("jump")
                    && mount.getTierJump() < mount.getType().getMaxJump()) {
                mount.setTierJump(mount.getTierJump() + 1);
                success = true;
            }
            else if (type.equals("durability")
                    && mount.getTierDurability() < mount.getType().getMaxDurability()) {
                mount.setTierDurability(mount.getTierDurability() + 1);
                success = true;
            }
            else if (type.equals("cooldown")
                    && mount.getTierCooldown() < mount.getType().getMaxCooldown()) {
                mount.setTierCooldown(mount.getTierCooldown() + 1);
                success = true;
            }
            else if (type.equals("storage")
                    && mount.getTierStorage() < mount.getType().getMaxStorage()) {
                mount.setTierStorage(mount.getTierStorage() + 1);
                success = true;
            }
            if (success) {
                plugin.getMountDataManager().saveProfile(player.getUniqueId());
                item.setAmount(item.getAmount() - 1);
                player.sendMessage(
                        "§e§l[!] §fĐã dùng Bùa Ép Xung để tăng 1 Bậc chỉ số cho Thú Cưỡi!"
                );
                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_ANVIL_USE,
                        1f,
                        1.5f
                );
            } else {
                player.sendMessage(
                        "§c§l[!] §cChỉ số này đã đạt cấp tối đa!"
                );
            }
        }
    }
}