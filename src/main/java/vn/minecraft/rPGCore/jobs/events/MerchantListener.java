package vn.minecraft.rPGCore.jobs.events;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.jobs.types.MerchantJob;
import vn.minecraft.rPGCore.jobs.core.JobManager;
import vn.minecraft.rPGCore.jobs.core.PlayerJobData;

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

        // Kiểm tra xem có phải quái hình người không
        if (!humanoids.contains(event.getEntityType())) return;

        PlayerJobData data = JobManager.getPlayerData(player);
        if (!data.getJobId().equalsIgnoreCase("MERCHANT")) return;

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

        // 3. Kiểm tra Buff từ Cân Tiểu Ly Mạ Vàng trong túi đồ
        boolean hasScaleBuff = false;
        NamespacedKey scaleKey = new NamespacedKey(plugin, "scale_expire_time");
        long now = System.currentTimeMillis();

        for (ItemStack item : player.getInventory().getContents()) {
            // [ĐÃ SỬA DÒNG NÀY]: Đảo "misc_gold_scale" lên trước .equals() để chống Null
            if (item != null && item.hasItemMeta() && "misc_gold_scale".equals(plugin.getItemManager().getItemId(item))) {
                long expire = item.getItemMeta().getPersistentDataContainer().getOrDefault(scaleKey, PersistentDataType.LONG, 0L);
                if (expire > now) {
                    hasScaleBuff = true;
                    break;
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

    @EventHandler
    public void onInteractMount(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;
        String itemId = plugin.getItemManager().getItemId(item);
        if (itemId == null) return;

        // Chỉ xử lý nếu đang cầm Giấy thu hồi hoặc Bùa ép xung
        if (!itemId.equals("misc_mount_retrieval") && !itemId.startsWith("misc_mount_upgrade_")) return;

        // KIỂM TRA ĐÂY CÓ PHẢI LÀ THÚ CƯỠI CỦA NGƯỜI CHƠI HAY KHÔNG
        // Chú ý: Bạn hãy thay đổi hàm "getActiveMountSlot" dưới đây theo đúng hàm check thú cưỡi của bạn!
        int activeSlot = -1; // plugin.getMountManager().getActiveMountSlot(player, event.getRightClicked());

        // (Giả sử bạn có hàm tìm xem con Entity bị click thuộc về Slot nào của người chơi)
        // Ví dụ: activeSlot = plugin.getMountManager().getSlotOfSpawnedMount(player, event.getRightClicked());

        if (activeSlot == -1) {
            player.sendMessage("§c§l[!] §cĐây không phải là thú cưỡi bạn đang triệu hồi!");
            return;
        }

        vn.minecraft.rPGCore.mounts.data.PlayerMountProfile profile = plugin.getMountManager().getProfile(player.getUniqueId());
        vn.minecraft.rPGCore.mounts.data.MountData mount = profile.getMountAt(activeSlot);
        if (mount == null) return;

        event.setCancelled(true); // Hủy tương tác leo lên thú

        // 1. LOGIC ĐÓNG GÓI
        if (itemId.equals("misc_mount_retrieval")) {
            // Tạo thẻ bài
            ItemStack card = plugin.getItemManager().buildItem("misc_mount_packaged", 1);
            ItemMeta meta = card.getItemMeta();
            org.bukkit.persistence.PersistentDataContainer pda = meta.getPersistentDataContainer();

            pda.set(new org.bukkit.NamespacedKey(plugin, "mount_arch"), PersistentDataType.STRING, mount.getType().name());
            pda.set(new org.bukkit.NamespacedKey(plugin, "mount_sp"), PersistentDataType.INTEGER, mount.getTierSpeed());
            pda.set(new org.bukkit.NamespacedKey(plugin, "mount_jp"), PersistentDataType.INTEGER, mount.getTierJump());
            pda.set(new org.bukkit.NamespacedKey(plugin, "mount_hp"), PersistentDataType.INTEGER, mount.getTierDurability());
            pda.set(new org.bukkit.NamespacedKey(plugin, "mount_cd"), PersistentDataType.INTEGER, mount.getTierCooldown());
            pda.set(new org.bukkit.NamespacedKey(plugin, "mount_st"), PersistentDataType.INTEGER, mount.getTierStorage());
            if (mount.getInventoryBase64() != null) {
                pda.set(new org.bukkit.NamespacedKey(plugin, "mount_inv"), PersistentDataType.STRING, mount.getInventoryBase64());
            }

            // Gắn Lore hiển thị chỉ số lên Thẻ
            // =========================================================
            // GẮN LORE HIỂN THỊ CHI TIẾT ĐỂ NGƯỜI CHƠI DỄ COI / GIAO DỊCH
            // =========================================================
            List<String> lore = new ArrayList<>();
            // Lấy lại các dòng mô tả gốc của vật phẩm (nếu có đăng ký trong ItemManager)
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

            // Tính năng ẩn: Check hộ hành lý bên trong rương thú cưỡi luôn!
            if (mount.getInventoryBase64() != null && !mount.getInventoryBase64().isEmpty()) {
                lore.add("§d§l[!] Thú cưỡi này đang mang theo hành lý!");
            } else {
                lore.add("§7§o(Túi đồ thú cưỡi trống không)");
            }
            lore.add("§8§m-------------------------");
            lore.add("§a► Click phải để giải nén vào Chuồng Ngựa!");

            meta.setLore(lore);
            card.setItemMeta(meta);

            // Gửi Thẻ cho người chơi, Xóa Thú khỏi chuồng và Despawn thú hiện tại
            player.getInventory().addItem(card);
            profile.setMountAt(activeSlot, null);
            plugin.getMountDataManager().saveProfile(player.getUniqueId());

            // Xóa thú cưỡi khỏi thế giới (Thay bằng hàm despawn thú của bạn)
            event.getRightClicked().remove();

            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a§l[!] §fĐã thu hồi và đóng gói thành công!");
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
        }

        // 2. LOGIC NÂNG CẤP TRỰC TIẾP
        else if (itemId.startsWith("misc_mount_upgrade_")) {
            boolean success = false;
            String type = itemId.replace("misc_mount_upgrade_", "");

            if (type.equals("speed") && mount.getTierSpeed() < mount.getType().getMaxSpeed()) { mount.setTierSpeed(mount.getTierSpeed() + 1); success = true; }
            else if (type.equals("jump") && mount.getTierJump() < mount.getType().getMaxJump()) { mount.setTierJump(mount.getTierJump() + 1); success = true; }
            else if (type.equals("durability") && mount.getTierDurability() < mount.getType().getMaxDurability()) { mount.setTierDurability(mount.getTierDurability() + 1); success = true; }
            else if (type.equals("cooldown") && mount.getTierCooldown() < mount.getType().getMaxCooldown()) { mount.setTierCooldown(mount.getTierCooldown() + 1); success = true; }
            else if (type.equals("storage") && mount.getTierStorage() < mount.getType().getMaxStorage()) { mount.setTierStorage(mount.getTierStorage() + 1); success = true; }

            if (success) {
                plugin.getMountDataManager().saveProfile(player.getUniqueId());
                item.setAmount(item.getAmount() - 1);
                player.sendMessage("§e§l[!] §fĐã dùng Bùa Ép Xung để tăng 1 Bậc chỉ số cho Thú Cưỡi!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            } else {
                player.sendMessage("§c§l[!] §cChỉ số này đã đạt cấp tối đa, không thể dùng bùa nữa!");
            }
        }
    }
}