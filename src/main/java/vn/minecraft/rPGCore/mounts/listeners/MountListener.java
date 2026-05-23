package vn.minecraft.rPGCore.mounts.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import vn.minecraft.rPGCore.mounts.data.MountData;
import vn.minecraft.rPGCore.mounts.data.PlayerMountProfile;
import vn.minecraft.rPGCore.mounts.managers.MountManager;
import vn.minecraft.rPGCore.mounts.ui.MountStorageGUI;

public class MountListener implements Listener {

    private final MountManager mountManager;
    private final NamespacedKey slotKey;
    private final MountStorageGUI storageGUI;

    public MountListener(MountManager mountManager, MountStorageGUI storageGUI) {
        this.mountManager = mountManager;
        this.storageGUI = storageGUI;
        this.slotKey = new NamespacedKey(mountManager.getPlugin(), "mount_slot");
    }

    // ==========================================================
    // 1. CHẶN COMBAT KHI ĐANG CƯỠI (Chặn cả Cận chiến lẫn Đạn đạo)
    // ==========================================================
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        Player attacker = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        if (attacker != null && attacker.isInsideVehicle()) {
            Entity vehicle = attacker.getVehicle();
            if (vehicle != null && vehicle == mountManager.getActiveMount(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage("§c§l[!] §cBạn không thể tấn công khi đang ở trên thú cưỡi!");
            }
        }
    }

    // ==========================================================
    // 2. NGĂN NGƯỜI KHÁC TRÈO LÊN & HỦY ĐẾM NGƯỢC KHI LÊN LẠI
    // ==========================================================
    @EventHandler
    public void onMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Entity vehicle = event.getMount();

            if (vehicle.getPersistentDataContainer().has(mountManager.MOUNT_OWNER_KEY, PersistentDataType.STRING)) {
                String ownerUuidStr = vehicle.getPersistentDataContainer().get(mountManager.MOUNT_OWNER_KEY, PersistentDataType.STRING);

                if (!ownerUuidStr.equals(player.getUniqueId().toString())) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[!] §cĐây không phải là thú cưỡi của bạn! Không thể trèo lên.");
                    return;
                }

                if (vehicle == mountManager.getActiveMount(player)) {
                    mountManager.cancelDespawnTask(player);
                    player.sendMessage("§aTiếp tục hành trình!");
                }
            }
        }
    }

    // ==========================================================
    // 3. ĐẾM NGƯỢC THU HỒI KHI XUỐNG XE
    // ==========================================================
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Entity vehicle = event.getDismounted();

            if (vehicle == mountManager.getActiveMount(player)) {
                mountManager.startDespawnTimer(player);
            }
        }
    }

    // ==========================================================
    // 4. XỬ LÝ THÚ CƯỠI BỊ ĐÁNH CHẾT (Cooldown Hồi Sức)
    // ==========================================================
    @EventHandler
    public void onMountDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getPersistentDataContainer().has(mountManager.MOUNT_OWNER_KEY, PersistentDataType.STRING)) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            String ownerUuidStr = entity.getPersistentDataContainer().get(mountManager.MOUNT_OWNER_KEY, PersistentDataType.STRING);
            Player owner = Bukkit.getPlayer(java.util.UUID.fromString(ownerUuidStr));

            if (owner != null && mountManager.getActiveMount(owner) == entity) {
                if (entity.getPersistentDataContainer().has(slotKey, PersistentDataType.INTEGER)) {
                    int slot = entity.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
                    PlayerMountProfile profile = mountManager.getProfile(owner.getUniqueId());

                    if (profile != null && profile.hasMountAt(slot)) {
                        MountData data = profile.getMountAt(slot);
                        data.setDead();

                        owner.sendMessage("§c§l[!] §cThú cưỡi của bạn đã gục ngã! Đang đưa về chuồng để hồi sức.");
                        owner.playSound(owner.getLocation(), Sound.ENTITY_HORSE_DEATH, 1f, 1f);

                        mountManager.despawnMount(owner, true);
                    }
                }
            }
        }
    }

    // ==========================================================
    // 5. THU HỒI NGAY LẬP TỨC KHI NGƯỜI CHƠI THOÁT GAME
    // ==========================================================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        mountManager.despawnMount(event.getPlayer(), false);
    }

    // ==========================================================
    // 6. TƯƠNG TÁC: SHIFT + CHUỘT PHẢI ĐỂ MỞ KHO ĐỒ
    // ==========================================================
    @EventHandler
    public void onInteractMount(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (entity.getPersistentDataContainer().has(mountManager.MOUNT_OWNER_KEY, PersistentDataType.STRING)) {
            Player player = event.getPlayer();
            String ownerUuidStr = entity.getPersistentDataContainer().get(mountManager.MOUNT_OWNER_KEY, PersistentDataType.STRING);

            if (ownerUuidStr.equals(player.getUniqueId().toString())) {
                if (player.isSneaking()) {
                    event.setCancelled(true); // Chặn Vanilla UI

                    if (entity.getPersistentDataContainer().has(slotKey, PersistentDataType.INTEGER)) {
                        int slot = entity.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
                        PlayerMountProfile profile = mountManager.getProfile(player.getUniqueId());
                        MountData data = profile.getMountAt(slot);

                        player.sendMessage("§e[Hệ thống] Đang mở kho đồ (Dung lượng: " + (data.getTierStorage() * 3) + " ô)...");
                        storageGUI.openStorage(player, data);
                    }
                }
            }
        }
    }
}