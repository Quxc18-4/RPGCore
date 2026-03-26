package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class PiercingBeamSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PiercingBeamSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "archer_piercing_beam"; }
    @Override public String getName() { return "Tia Năng Lượng"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.LIGHTNING_ROD; }
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 32; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> reqs = new HashMap<>();
        reqs.put("archer_sniper_passive", 5);
        reqs.put("archer_pierce_buff", 5);
        return reqs;
    }

    @Override public double getManaCost(int level) { return 50.0 + (level * 5.0); }
    @Override public long getCooldown(int level) { return 12000L - (level * 200L); }

    @Override
    public List<String> getDescription(int level) {
        int dmgDisplay = (int) ((2.65 + (level - 1) * 0.15) * 100);
        int maxTargets = 5 + level;
        return Arrays.asList(
                "§7Kết tinh của sự chuẩn xác và sức mạnh xuyên tâm.",
                "§7Bắn ra một tia sóng siêu âm nén cực mạnh.",
                "",
                "§f- Sát thương: §c" + dmgDisplay + "% §fSát thương xuyên thấu",
                "§f- Tầm bắn: §a25 block",
                "§f- Xuyên thấu: §e" + maxTargets + " kẻ địch",
                "§8(Bỏ qua hoàn toàn hiệu ứng giảm sát thương xuyên thấu)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

        if (currentTime - lastUsed < finalCd) {
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", (finalCd - (currentTime - lastUsed)) / 1000.0) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);

        player.sendMessage("§a§l[!] TIA NĂNG LƯỢNG!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Set<LivingEntity> hitTargets = new HashSet<>();
        int maxTargets = 5 + level;

        double dmgMulti = 2.65 + (level - 1) * 0.15;
        boolean isCrossbow = player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW;

        for (double d = 0; d <= 25; d += 0.5) {
            Location point = eyeLoc.clone().add(dir.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);

            for (org.bukkit.entity.Entity e : point.getWorld().getNearbyEntities(point, 1.0, 1.0, 1.0)) {
                if (e instanceof LivingEntity && e != player) {

                    // [SỬA LỖI]: Cho phép bắn thú của người khác, chỉ bỏ qua thú của chính mình
                    boolean isOwnPet = false;
                    if (e.hasMetadata("Summoner_Pet")) {
                        String ownerId = e.getMetadata("Summoner_Pet").get(0).asString();
                        if (ownerId.equals(player.getUniqueId().toString())) {
                            isOwnPet = true;
                        }
                    }

                    if (!isOwnPet) {
                        LivingEntity target = (LivingEntity) e;
                        if (!hitTargets.contains(target)) {
                            hitTargets.add(target);

                            double finalDamage = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 50.0, 0.0, 0.0, false, isCrossbow);
                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                            CombatManager.applySkillDamage(player, target, finalDamage);
                            player.removeMetadata("Ghost_L_Blocker", plugin);

                            // Ngắt vòng lặp nếu đã xuyên đủ số mục tiêu cho phép
                            if (hitTargets.size() >= maxTargets) break;
                        }
                    }
                }
            }
            if (hitTargets.size() >= maxTargets) break;
            if (point.getBlock().getType().isSolid()) break;
        }
        return true;
    }
}