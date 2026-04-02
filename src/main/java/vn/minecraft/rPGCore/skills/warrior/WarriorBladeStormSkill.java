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
        int baseDmg = (int) ((0.4 + (level * 0.05)) * 100);
        int awakenDmg = (int) ((0.7 + (level * 0.12)) * 100);

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
                "§f- Quẹt trúng Cán giáo: Gây §7" + (int)(baseDmg * 0.6) + "% Sát thương§f.",
                "§f- Quẹt trúng Rìa Mũi: Gây §c" + (int)(baseDmg * 1.5) + "% Sát thương§f.",
                "",
                "§c[Thức Tỉnh Kỹ Năng] (Tiêu hao 50 Nộ)",
                "§f- Giáo hóa Kim Cương, duy trì §a4 giây§f.",
                "§f- Lưỡi cắt kim cương bỏ qua §e30% Giáp§f.",
                "§f- Quẹt Mũi giáo Thức tỉnh: §c" + (int)(awakenDmg * 1.5) + "% ST§f."
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
        double baseDmgMultiplier;

        // [ĐÃ SỬA LỖI ĐỎ]: Chỉ khai báo, KHÔNG gán giá trị mặc định ở đây
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

        weaponItem = new ItemStack(Material.IRON_SWORD);
        org.bukkit.inventory.meta.ItemMeta meta = weaponItem.getItemMeta();

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
            player.sendMessage("§c§l[THỨC TỈNH] §fGiáo Trận Cường Hóa!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.5f);

            durationTicks = 80;
            baseDmgMultiplier = 0.7 + (level * 0.12);

            // Gán giá trị 1 lần duy nhất cho nhánh Thức Tỉnh
            armorPenetration = 0.30;

            if (meta != null) meta.setCustomModelData(1005);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
            durationTicks = 60;
            baseDmgMultiplier = 0.4 + (level * 0.05);

            // Gán giá trị 1 lần duy nhất cho nhánh Thường
            armorPenetration = 0.0;

            int modelIdToSet = 1001;
            if (spearCount == 3) modelIdToSet = 1001;
            else if (spearCount == 4) modelIdToSet = 1002;
            else if (spearCount == 5) modelIdToSet = 1003;
            else if (spearCount >= 6) modelIdToSet = 1004;

            if (meta != null) meta.setCustomModelData(modelIdToSet);
        }

        if (meta != null) weaponItem.setItemMeta(meta);

        List<ItemDisplay> displays = new ArrayList<>();
        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setPitch(0f);

        for (int i = 0; i < spearCount; i++) {
            ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
            display.setItemStack(weaponItem);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setPersistent(false);

            AxisAngle4f pointOut = new AxisAngle4f((float) Math.toRadians(-90), 0, 0, 1);
            AxisAngle4f layFlat = new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);

            Transformation trans = new Transformation(
                    new Vector3f(0, 0, scaleSize * 0.5f),
                    layFlat,
                    new Vector3f(scaleSize, scaleSize, scaleSize),
                    pointOut
            );

            display.setTransformation(trans);
            display.setInvulnerable(true);

            displays.add(display);
        }
        activeDisplays.put(player.getUniqueId(), displays);

        // ==========================================
        // VÒNG LẶP XOAY VÀ HITBOX (Java sẽ không báo lỗi nữa)
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

                    weaponLoc.setPitch(0f);
                    weaponLoc.setYaw((float) Math.toDegrees(angleOffset));

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
                            double finalDmgMulti = baseDmgMultiplier;

                            if (distanceToTarget >= maxReach - 2.0 && distanceToTarget <= maxReach) {
                                finalDmgMulti *= 1.5;
                                player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                            } else if (distanceToTarget < maxReach - 2.0) {
                                finalDmgMulti *= 0.6;
                                player.getWorld().spawnParticle(Particle.ASH, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
                            }

                            // Biến armorPenetration bây giờ là "effectively final" nên dùng thoải mái ở đây
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