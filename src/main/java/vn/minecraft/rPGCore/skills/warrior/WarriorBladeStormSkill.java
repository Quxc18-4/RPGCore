package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WarriorBladeStormSkill implements ISkill {

    private final Map<UUID, List<ItemDisplay>> activeDisplays = new HashMap<>();

    @Override public String getId() { return "warrior_bladestorm"; }
    @Override public String getName() { return "§b§lGiáo Trận"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.IRON_SPEAR; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 10; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return false; }

    @Override public double getManaCost(int level) { return 50; }
    @Override public long getCooldown(int level) { return 25000L - (level * 1000L); }

    @Override
    public List<String> getDescription(int level) {
        int normalHiltDmg = (int) ((0.20 + (level * 0.02)) * 100);
        int normalSweetDmg = (int) ((0.35 + (level * 0.04)) * 100);
        int awakenHiltDmg = (int) ((0.40 + (level * 0.03)) * 100); // Cán thức tỉnh
        int awakenSweetDmg = (int) ((0.80 + (level * 0.04)) * 100); // Mũi thức tỉnh

        int spearCount = 3 + (level / 3);

        double hitPerSec;
        if (spearCount <= 3) hitPerSec = 2.5;
        else if (spearCount == 4) hitPerSec = 3.3;
        else if (spearCount == 5) hitPerSec = 5.0;
        else hitPerSec = 6.7;

        return Arrays.asList(
                "§7Triệu hồi các mũi giáo bay quanh bản thân.",
                "§7Yêu cầu kỹ năng giữ khoảng cách (Spacing).",
                "",
                "§e[Kích hoạt: Giáo Trận]",
                "§f- Gọi §e" + spearCount + "§f cây giáo xoay quanh trong §a3 giây§f.",
                "§f- Tốc độ cưa: §e" + String.format("%.1f", hitPerSec) + " hit/giây§f.",
                "§f- Bán kính quét: §a2.8 Block§f.",
                "§f- Quẹt trúng Cán: Gây §7" + normalHiltDmg + "% ST§f.",
                "§f- Quẹt trúng Mũi: Gây §c" + normalSweetDmg + "% ST§f.",
                "",
                "§c[Thức Tỉnh Kỹ Năng] (Tiêu hao 50 Nộ)",
                "§f- Giáo hóa Kim Cương, duy trì §a4 giây§f.",
                "§f- Lưỡi cắt kim cương bỏ qua §e30% Giáp§f.",
                "§f- Quẹt Cán Thức tỉnh: §7" + awakenHiltDmg + "% ST§f.",
                "§f- Quẹt Mũi Thức tỉnh: §c" + awakenSweetDmg + "% ST§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            List<org.bukkit.metadata.MetadataValue> metaList = player.getMetadata(cdKey);
            if (!metaList.isEmpty()) {
                long cdEnd = metaList.get(0).asLong();
                if (System.currentTimeMillis() < cdEnd) {
                    player.sendMessage("§cKỹ năng đang hồi chiêu!");
                    return false;
                }
            } else { player.removeMetadata(cdKey, RPGCore.getInstance()); }
        }

        cleanupDisplays(player);

        double cost = getManaCost(level);
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= cost;

        long durationTicks;
        double hiltDmgMulti;
        double sweetSpotDmgMulti;
        double armorPenetration;
        ItemStack weaponItem;

        int spearCount = 3 + (level / 3);
        double hiltRadius = 0.8;

        float scaleSize = 1.5f;
        double maxReach = hiltRadius + 2.0;

        int hitIntervalTicks;
        if (spearCount <= 3) hitIntervalTicks = 8;
        else if (spearCount == 4) hitIntervalTicks = 6;
        else if (spearCount == 5) hitIntervalTicks = 4;
        else hitIntervalTicks = 3;

        int ticksPerRevolution = hitIntervalTicks * spearCount;
        double anglePerTick = (Math.PI * 2) / ticksPerRevolution;

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
            player.sendMessage("§c§l[THỨC TỈNH] §fGiáo Trận Cường Hóa!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.5f);

            durationTicks = 80;

            // Sát thương Thức Tỉnh
            hiltDmgMulti = 0.40 + (level * 0.03);      // Cán nộ: 40% + 3%/cấp
            sweetSpotDmgMulti = 0.80 + (level * 0.04); // Mũi nộ: 80% + 4%/cấp

            armorPenetration = 0.30;
            weaponItem = new ItemStack(Material.valueOf("DIAMOND_SPEAR"));
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
            durationTicks = 60;

            // Sát thương Thường
            hiltDmgMulti = 0.20 + (level * 0.02);      // Cán thường: 20% + 2%/cấp
            sweetSpotDmgMulti = 0.35 + (level * 0.04); // Mũi thường: 35% + 4%/cấp

            armorPenetration = 0.0;
            weaponItem = new ItemStack(Material.IRON_SPEAR);
        }

        List<ItemDisplay> displays = new ArrayList<>();
        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setPitch(0f);

        for (int i = 0; i < spearCount; i++) {
            ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
            display.setItemStack(weaponItem);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setPersistent(false);

            // ==========================================
            // [TRẢ LẠI CODE MŨI GIÁO]: Dùng lại đoạn nghiêng 45 độ chuẩn xác của bạn
            // ==========================================
            AxisAngle4f layFlat = new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);
            AxisAngle4f tilt45 = new AxisAngle4f((float) Math.toRadians(45), 0, 0, 1);

            Transformation trans = new Transformation(
                    new Vector3f(0, 0, scaleSize * 0.5f),
                    layFlat,
                    new Vector3f(scaleSize, scaleSize, scaleSize),
                    tilt45
            );

            display.setTransformation(trans);
            display.setInvulnerable(true);

            displays.add(display);
        }
        activeDisplays.put(player.getUniqueId(), displays);

        // ==========================================
        // VÒNG LẶP XOAY VÀ HITBOX
        // ==========================================
        new BukkitRunnable() {
            int ticks = 0;
            double currentAngle = 0;

            @Override
            public void run() {
                if (ticks > durationTicks || !player.isOnline() || player.isDead()) {
                    for (ItemDisplay d : displays) {
                        if (d.isValid()) d.remove();
                    }
                    if (activeDisplays.get(player.getUniqueId()) == displays) {
                        activeDisplays.remove(player.getUniqueId());
                    }
                    this.cancel();
                    return;
                }

                Location center = player.getLocation().clone();
                center.setPitch(0f);
                center.add(0, 1.2, 0);

                currentAngle -= anglePerTick;

                for (int i = 0; i < spearCount; i++) {
                    ItemDisplay display = displays.get(i);
                    if (!display.isValid()) continue;

                    double angleOffset = currentAngle + (i * Math.PI * 2 / spearCount);
                    double x = Math.cos(angleOffset) * hiltRadius;
                    double z = Math.sin(angleOffset) * hiltRadius;

                    Location weaponLoc = center.clone().add(x, 0, z);

                    // ==========================================
                    // [TRẢ LẠI CODE MŨI GIÁO]: Bù trừ Yaw -45 độ để mũi hướng thẳng ra ngoài
                    // ==========================================
                    weaponLoc.setPitch(0f);
                    weaponLoc.setYaw((float) Math.toDegrees(angleOffset) - 45);

                    display.teleport(weaponLoc);

                    Location tipLoc = weaponLoc.clone().add(weaponLoc.getDirection().multiply(scaleSize * 1.5));
                    if (isEmpowered) {
                        display.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, tipLoc, 1, 0, 0, 0, 0);
                    } else {
                        display.getWorld().spawnParticle(Particle.FLAME, tipLoc, 1, 0, 0, 0, 0);
                    }
                }

                if (ticks % hitIntervalTicks == 0) {
                    player.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);

                    for (Entity e : center.getWorld().getNearbyEntities(center, maxReach, 1.5, maxReach)) {
                        if (e instanceof LivingEntity && !(e instanceof ArmorStand) && CombatManager.canTarget(player, e)) {
                            LivingEntity target = (LivingEntity) e;

                            double distanceToTarget = center.distance(target.getLocation());
                            double finalDmgMulti;

                            // Trúng Rìa Mũi (Sweet Spot)
                            if (distanceToTarget >= maxReach - 2.0 && distanceToTarget <= maxReach) {
                                finalDmgMulti = sweetSpotDmgMulti; // Lấy DMG Mũi
                                player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                            }
                            // Trúng Cán Giáo (Hilt)
                            else {
                                finalDmgMulti = hiltDmgMulti; // Lấy DMG Cán
                                player.getWorld().spawnParticle(Particle.ASH, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
                            }

                            // Đưa vào tính toán
                            double dmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, armorPenetration * 100, false, false);

                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(player, target, dmg);
                            player.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.1);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        return true;
    }

    private void cleanupDisplays(Player player) {
        List<ItemDisplay> displays = activeDisplays.remove(player.getUniqueId());
        if (displays != null) {
            for (ItemDisplay display : displays) {
                if (display.isValid()) display.remove();
            }
        }
    }
}