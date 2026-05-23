package vn.minecraft.rPGCore.mounts.managers;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore; // Ép kiểu về Class chính
import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;
import vn.minecraft.rPGCore.mounts.type.MountArchetype;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MountManager {

    private final RPGCore plugin; // Đổi từ JavaPlugin thành RPGCore để gọi được các Manager khác
    private final Map<UUID, PlayerMountProfile> loadedProfiles = new HashMap<>();
    private final Map<UUID, Entity> activeMounts = new HashMap<>();
    private final Map<UUID, Integer> despawnTasks = new HashMap<>();
    public final NamespacedKey MOUNT_OWNER_KEY;

    public MountManager(RPGCore plugin) {
        this.plugin = plugin;
        this.MOUNT_OWNER_KEY = new NamespacedKey(plugin, "mount_owner_uuid");
    }

    public RPGCore getPlugin() { return plugin; }
    public Map<UUID, PlayerMountProfile> getLoadedProfilesMap() { return loadedProfiles; }

    // =====================================================================
    // CORE LOGIC: SỬA LỖI MẤT DATA KHI RELOAD PLUGIN
    // =====================================================================
    public PlayerMountProfile getProfile(UUID uuid) {
        // 1. Nếu dữ liệu đang nằm trên RAM (người chơi online bình thường), trả về luôn
        if (loadedProfiles.containsKey(uuid)) {
            return loadedProfiles.get(uuid);
        }

        // 2. Nếu RAM trống (Do vừa Reload xong nhưng người chơi vẫn đang online)
        // Gọi MountDataManager nạp lại file JSON cũ lên RAM
        plugin.getMountDataManager().loadProfile(uuid);

        // 3. Nếu sau khi đọc file mà vẫn không có (Người chơi mới tinh lần đầu vào server)
        return loadedProfiles.computeIfAbsent(uuid, PlayerMountProfile::new);
    }

    // =====================================================================
    // CORE LOGIC: TRIỆU HỒI THÚ CƯỠI
    // =====================================================================
    public void spawnMount(Player player, int slot) {
        PlayerMountProfile profile = getProfile(player.getUniqueId());
        if (!profile.hasMountAt(slot)) {
            player.sendMessage("§c§l[!] §cÔ chuồng số " + slot + " của bạn chưa có thú cưỡi!");
            return;
        }

        MountData mountData = profile.getMountAt(slot);
        if (mountData.isDead()) {
            long sec = mountData.getRemainingCooldownMs() / 1000;
            player.sendMessage("§c§l[!] §cThú cưỡi đang hồi sức! Chờ: §f" + (sec / 60) + "m " + (sec % 60) + "s.");
            return;
        }

        if (activeMounts.containsKey(player.getUniqueId())) despawnMount(player, false);

        Location spawnLoc = player.getLocation();
        EntityType type;
        try {
            type = EntityType.valueOf(mountData.getType().getEntityId());
        } catch (IllegalArgumentException e) { type = EntityType.HORSE; }

        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, type);
        entity.getPersistentDataContainer().set(MOUNT_OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

        NamespacedKey slotKey = new NamespacedKey(plugin, "mount_slot");
        entity.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);

        entity.setCustomName("§6" + mountData.getCustomName() + " §7của " + player.getName());
        entity.setCustomNameVisible(true);
        entity.setPersistent(false);

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            // --- 1. CONFIG MÁU CHUẨN (1000 GỐC + 1000 MỖI BẬC) ---
            double maxHealth = 1000.0 + (mountData.getTierDurability() * 1000.0);
            if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
                living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
                living.setHealth(maxHealth);
            }

            // --- 2. CONFIG CHỈ SỐ ĐỘNG THEO TỪNG LOÀI THÚ CƯỠI ---
            configureIndividualAttributes(living, mountData);

            // --- 3. ÉP THUẦN HÓA VÀ TỰ ĐỘNG ĐEO TRANG BỊ ---
            equipIndividualMount(living, mountData.getType());
        }

        activeMounts.put(player.getUniqueId(), entity);
        player.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 30, 0.5, 0.5, 0.5, 0.1);
        player.playSound(spawnLoc, Sound.ENTITY_HORSE_ARMOR, 1f, 1f);
        player.sendMessage("§aĐã triệu hồi thành công: §l" + mountData.getCustomName());
    }

    // =====================================================================
    // PHÂN CHIA CHIẾN LƯỢC: ATTRIBUTE ĐỘNG (THAY ĐỔI THEO GÓP Ý CỦA BẠN)
    // =====================================================================
    private void configureIndividualAttributes(LivingEntity living, MountData data) {
        MountArchetype arch = data.getType();

        double speedBase = 0.2;
        double speedMulti = 0.015;

        double jumpBase = 0.5;
        double jumpMulti = 0.03;

        switch (arch) {
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
                speedBase = 0.25;
                speedMulti = 0.02;
                jumpBase = 0.6;
                jumpMulti = 0.04;
                break;

            case CAMEL:
            case CAMEL_HUSK:
                speedBase = 0.18;
                speedMulti = 0.008;
                jumpBase = 0.35;
                jumpMulti = 0.003;
                break;

            case HAPPY_GHAST:
                try {
                    if (living.getAttribute(Attribute.FLYING_SPEED) != null) {
                        living.getAttribute(Attribute.FLYING_SPEED).setBaseValue(0.05 + (data.getTierSpeed() * 0.0035));
                    }
                } catch (NoSuchFieldError ignored) {}
                break;

            case NAUTILUS:
            case ZOMBIE_NAUTILUS:
                // [TỐI ƯU THEO GÓP Ý]: Nautilus lao đi bằng MOVEMENT_SPEED chứ không phải Efficiency!
                // Nâng mạnh speedBase và speedMulti để tăng lực đẩy vật lý tổng thể dưới nước khi phóng đi
                speedBase = 0.65;   // Tốc độ di chuyển cơ bản rất cao để phá lực ma sát nước
                speedMulti = 0.065; // Thăng tiến cực gắt ở bậc cao

                // Giữ nguyên thuộc tính jump để tương thích phôi gốc nếu cần bật nhảy
                jumpBase = 0.45;
                jumpMulti = 0.045;

                // Bổ sung hiệu suất rẽ nước phụ trợ giúp chuyển động mượt mà không bị khựng hình
                try {
                    if (living.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY) != null) {
                        living.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY).setBaseValue(2.5 + (data.getTierSpeed() * 0.15));
                    }
                } catch (NoSuchFieldError ignored) {}
                break;

            default: // Các dòng Ngựa thường, Lừa
                speedBase = 0.2;
                speedMulti = 0.015;
                jumpBase = 0.5;
                jumpMulti = 0.035;
                break;
        }

        // =====================================
        // BUFF CHỈ SỐ TỪ NGHỀ THƯƠNG NHÂN
        // =====================================
        String ownerUuidStr = living.getPersistentDataContainer().get(MOUNT_OWNER_KEY, PersistentDataType.STRING);
        if (ownerUuidStr != null) {
            Player owner = Bukkit.getPlayer(UUID.fromString(ownerUuidStr));
            if (owner != null && vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(owner) != null
                    && vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerJob(owner).getId().equals("MERCHANT")) {

                // Nội tại 2: Tăng Tốc Độ
                double speedBuff = vn.minecraft.rPGCore.jobs.types.MerchantJob.getMountSpeedBuff(owner);
                speedBase += (speedBase * speedBuff);
                speedMulti += (speedMulti * speedBuff);

                // Ultimate 1: Đoàn Thương Uyển (Tăng 15% Máu, Nhảy, Tốc độ)
                if (vn.minecraft.rPGCore.jobs.types.MerchantJob.hasUltimate(owner, "MERCH_ULT_1")) {
                    speedBase += (speedBase * 0.15);
                    jumpBase += (jumpBase * 0.15);

                    if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
                        double currentMaxHp = living.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                        living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(currentMaxHp * 1.15);
                        living.setHealth(currentMaxHp * 1.15);
                    }
                }
            }
        }

        // Áp chỉ số cuối cùng vào Thực thể
        if (living.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            living.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedBase + (data.getTierSpeed() * speedMulti));
        }
        if (living.getAttribute(Attribute.JUMP_STRENGTH) != null) {
            living.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(jumpBase + (data.getTierJump() * jumpMulti));
        }
    }

    // =====================================================================
    // PHÂN CHIA CHIẾN LƯỢC: TRANG BỊ ĐÍCH DANH THEO ID VẬT PHẨM (FIX CỨNG GHAST)
    // =====================================================================
    private void equipIndividualMount(LivingEntity living, MountArchetype type) {
        // TẤT CẢ đều được ép trạng thái thuần hóa công khai
        if (living instanceof Tameable) {
            ((Tameable) living).setTamed(true);
        }

        switch (type) {
            case HAPPY_GHAST:
                // Ép trực tiếp ID WHITE_HARNESS của bản 1.21+
                Material harnessMat = Material.WHITE_HARNESS;
                ItemStack harnessItem = new ItemStack(harnessMat);

                // Hướng 1: Đeo vào Equipment slots (Chestplate, Helmet, Tay) để hiển thị Model Pack
                if (living.getEquipment() != null) {
                    living.getEquipment().setItem(EquipmentSlot.BODY, harnessItem);
                }

                // Hướng 2: Dự phòng nếu Happy Ghast sử dụng cấu trúc Container giống hệ Ngựa/Lạc đà
                if (living instanceof org.bukkit.inventory.InventoryHolder) {
                    org.bukkit.inventory.Inventory inv = ((org.bukkit.inventory.InventoryHolder) living).getInventory();
                    if (inv != null && inv.getSize() > 0) {
                        inv.setItem(0, harnessItem); // Slot Saddle/Harness gốc
                        if (inv.getSize() > 1) {
                            inv.setItem(1, harnessItem); // Slot dự phòng phụ trợ
                        }
                    }
                }
                break;

            case NAUTILUS:
            case ZOMBIE_NAUTILUS:
                // Đeo yên ngựa trực tiếp vào slot tay của Nautilus để Client hiển thị người chơi đang nắm dây cưỡi
                if (living.getEquipment() != null) {
                    living.getEquipment().setItemInMainHand(new ItemStack(Material.SADDLE));
                }
                // Đồng thời kích hoạt InventoryHolder nếu Nautilus được tạo từ phôi lạc đà/ngựa ngầm
                if (living instanceof org.bukkit.inventory.InventoryHolder) {
                    org.bukkit.inventory.Inventory inv = ((org.bukkit.inventory.InventoryHolder) living).getInventory();
                    if (inv != null && inv.getSize() > 0) inv.setItem(0, new ItemStack(Material.SADDLE));
                }
                break;

            case HORSE:
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
            case MULE:
            case DONKEY:
            case CAMEL:
            case CAMEL_HUSK:
                if (living instanceof AbstractHorse) {
                    AbstractHorse horse = (AbstractHorse) living;
                    if (horse.getInventory() != null) {
                        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                    }
                }
                break;

            default:
                if (living instanceof Steerable) {
                    ((Steerable) living).setSaddle(true);
                }
                break;
        }
    }

    // --- LOGIC THU HỒI / DESPAWN ---
    public void despawnMount(Player player, boolean isDeath) {
        Entity mount = activeMounts.remove(player.getUniqueId());
        cancelDespawnTask(player);
        if (mount != null && !mount.isDead()) {
            player.getWorld().spawnParticle(Particle.WHITE_SMOKE, mount.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
            mount.remove();
        }
        if (!isDeath) player.sendMessage("§eThú cưỡi đã được thu hồi vào chuồng.");
    }

    public void startDespawnTimer(Player player) {
        cancelDespawnTask(player);
        player.sendMessage("§7[!] Thú cưỡi đang chờ bạn... Sẽ tự động rời đi sau 30 giây.");
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeMounts.containsKey(player.getUniqueId())) {
                Entity mount = activeMounts.get(player.getUniqueId());
                if (mount.getPassengers().isEmpty() || !mount.getPassengers().contains(player)) {
                    despawnMount(player, false);
                }
            }
        }, 30 * 20L).getTaskId();
        despawnTasks.put(player.getUniqueId(), taskId);
    }

    public void cancelDespawnTask(Player player) {
        if (despawnTasks.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(despawnTasks.remove(player.getUniqueId()));
        }
    }

    public Entity getActiveMount(Player player) { return activeMounts.get(player.getUniqueId()); }

    public void despawnAllActiveMounts() {
        for (UUID uuid : new java.util.ArrayList<>(activeMounts.keySet())) {
            Entity mount = activeMounts.get(uuid);
            if (mount != null && !mount.isDead()) mount.remove();
        }
        activeMounts.clear();
        despawnTasks.clear();
    }
}