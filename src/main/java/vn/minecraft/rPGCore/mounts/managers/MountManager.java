package vn.minecraft.rPGCore.mounts.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MountManager {

    private final JavaPlugin plugin;

    // Cache lưu trữ Profile của người chơi đang online
    private final Map<UUID, PlayerMountProfile> loadedProfiles = new HashMap<>();

    // Quản lý thực thể thú cưỡi đang được gọi ra thế giới
    private final Map<UUID, Entity> activeMounts = new HashMap<>();

    // Quản lý các Task đếm ngược 30 giây để Despawn
    private final Map<UUID, Integer> despawnTasks = new HashMap<>();

    public final NamespacedKey MOUNT_OWNER_KEY;

    public MountManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.MOUNT_OWNER_KEY = new NamespacedKey(plugin, "mount_owner_uuid");
    }

    // THÊM HÀM NÀY VÀO ĐỂ FIX LỖI:
    public JavaPlugin getPlugin() {
        return plugin;
    }

    // THÊM HÀM NÀY ĐỂ TRUY XUẤT DỮ LIỆU TỪ RAM:
    public Map<UUID, PlayerMountProfile> getLoadedProfilesMap() {
        return loadedProfiles;
    }

    // --- QUẢN LÝ PROFILE CƠ BẢN ---
    public PlayerMountProfile getProfile(UUID uuid) {
        // Trong thực tế, bạn sẽ load từ JSON ở đây. Tạm thời tạo mới nếu chưa có để test.
        return loadedProfiles.computeIfAbsent(uuid, PlayerMountProfile::new);
    }

    // --- LOGIC TRIỆU HỒI THÚ CƯỠI (/h) ---
    public void spawnMount(Player player, int slot) {
        PlayerMountProfile profile = getProfile(player.getUniqueId());

        if (!profile.hasMountAt(slot)) {
            player.sendMessage("§c§l[!] §cÔ chuồng số " + slot + " của bạn chưa có thú cưỡi!");
            return;
        }

        MountData mountData = profile.getMountAt(slot);

        // 1. Kiểm tra Cooldown chết
        if (mountData.isDead()) {
            long sec = mountData.getRemainingCooldownMs() / 1000;
            player.sendMessage("§c§l[!] §cThú cưỡi đang hồi sức, không thể gọi! Vui lòng chờ: §f" + (sec / 60) + " phút " + (sec % 60) + " giây.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 2. Thu hồi con cũ nếu đang gọi 1 con khác ngoài map
        if (activeMounts.containsKey(player.getUniqueId())) {
            despawnMount(player, false);
        }

        // 3. Triệu hồi thực thể
        Location spawnLoc = player.getLocation();
        EntityType type;
        try {
            type = EntityType.valueOf(mountData.getType().getEntityId());
        } catch (IllegalArgumentException e) {
            // Fallback an toàn nếu EntityID của bạn dùng tên Custom Mob hoặc MythicMobs chưa config
            type = EntityType.HORSE;
        }

        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, type);

        // Đánh dấu bản quyền chủ sở hữu vào NBT của Entity
        entity.getPersistentDataContainer().set(MOUNT_OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        entity.getPersistentDataContainer().set(new NamespacedKey(plugin, "mount_slot"), PersistentDataType.INTEGER, slot);
        entity.setCustomName("§6" + mountData.getCustomName() + " §7của " + player.getName());
        entity.setCustomNameVisible(true);

        entity.setPersistent(false);

        // 4. Áp dụng Chỉ số (Tốc độ, Máu, Nhảy) từ MountData
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            // MÁU: 1 Bậc Sức Bền = 1000 Máu
            double maxHealth = mountData.getTierDurability() * 1000.0;
            if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
                living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
                living.setHealth(maxHealth);
            }

            // TỐC ĐỘ DI CHUYỂN
            if (living.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                double baseSpeed = 0.2; // Tốc độ gốc
                double speedBonus = mountData.getTierSpeed() * 0.02;
                living.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(baseSpeed + speedBonus);
            }

            // SỨC NHẢY (Dành riêng cho nhóm ngựa)
            if (living instanceof AbstractHorse && living.getAttribute(Attribute.JUMP_STRENGTH) != null) {
                double baseJump = 0.5;
                double jumpBonus = mountData.getTierJump() * 0.05;
                living.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(baseJump + jumpBonus);

                // Tự động thuần hóa và gắn yên ngựa cho nhóm Native
                ((AbstractHorse) living).setTamed(true);
                ((AbstractHorse) living).getInventory().setSaddle(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SADDLE));
            }

            // Lợn / Strider cũng cần yên để người chơi chuột phải ngồi lên được
            if (living instanceof Steerable) {
                ((Steerable) living).setSaddle(true);
            }
        }

        // 5. Lưu vào bộ nhớ và tạo hiệu ứng
        activeMounts.put(player.getUniqueId(), entity);
        player.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 30, 0.5, 0.5, 0.5, 0.1);
        player.playSound(spawnLoc, Sound.ENTITY_HORSE_ARMOR, 1f, 1f);
        player.sendMessage("§aĐã triệu hồi thành công: §l" + mountData.getCustomName());
    }

    // --- LOGIC THU HỒI / DESPAWN ---
    public void despawnMount(Player player, boolean isDeath) {
        Entity mount = activeMounts.remove(player.getUniqueId());

        // Hủy bỏ Task đếm ngược nếu có
        cancelDespawnTask(player);

        if (mount != null && !mount.isDead()) {
            player.getWorld().spawnParticle(Particle.WHITE_SMOKE, mount.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
            mount.remove();
        }

        if (!isDeath) {
            player.sendMessage("§eThú cưỡi đã được thu hồi vào chuồng.");
        }
    }

    // --- LOGIC ĐẾM NGƯỢC 30 GIÂY KHI XUỐNG XE ---
    public void startDespawnTimer(Player player) {
        cancelDespawnTask(player); // Hủy task cũ nếu có

        player.sendMessage("§7[!] Thú cưỡi đang chờ bạn... Sẽ tự động rời đi sau 30 giây.");

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeMounts.containsKey(player.getUniqueId())) {
                Entity mount = activeMounts.get(player.getUniqueId());
                // Nếu người chơi KHÔNG ở trên xe thì mới despawn
                if (mount.getPassengers().isEmpty() || !mount.getPassengers().contains(player)) {
                    despawnMount(player, false);
                }
            }
        }, 30 * 20L).getTaskId(); // 30 giây * 20 ticks

        despawnTasks.put(player.getUniqueId(), taskId);
    }

    public void cancelDespawnTask(Player player) {
        if (despawnTasks.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(despawnTasks.remove(player.getUniqueId()));
        }
    }

    public Entity getActiveMount(Player player) {
        return activeMounts.get(player.getUniqueId());
    }

    // ===============================================
    // DỌN DẸP SẠCH SẼ KHI SERVER TẮT (Tránh rác thực thể)
    // ===============================================
    public void despawnAllActiveMounts() {
        // Dùng List copy để tránh lỗi ConcurrentModificationException
        for (UUID uuid : new java.util.ArrayList<>(activeMounts.keySet())) {
            Entity mount = activeMounts.get(uuid);
            if (mount != null && !mount.isDead()) {
                mount.remove();
            }
        }
        activeMounts.clear();
        despawnTasks.clear();
    }
}