package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorMeditation extends AbstractEnhancementSkill implements Listener {

    // Thông số theo cấp (Index 0 bỏ trống để dùng Index 1->5 tương ứng Level)
    private final int[] RAGE_GEN = {0, 2, 2, 3, 4, 5};
    private final double[] BUFF_DMG = {0.0, 0.10, 0.15, 0.20, 0.25, 0.30};

    public WarriorMeditation(JavaPlugin plugin) {
        super(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // [CHUẨN SOLID]: Đăng ký Hook tăng sát thương khi Thiền thành công
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                if (attacker.hasMetadata("Meditation_Damage_Buff") && attacker.hasMetadata("Meditation_Buff_Expire")) {
                    if (System.currentTimeMillis() <= attacker.getMetadata("Meditation_Buff_Expire").get(0).asLong()) {
                        return attacker.getMetadata("Meditation_Damage_Buff").get(0).asDouble();
                    } else {
                        // Dọn rác khi hết hạn
                        attacker.removeMetadata("Meditation_Damage_Buff", RPGCore.getInstance());
                        attacker.removeMetadata("Meditation_Buff_Expire", RPGCore.getInstance());
                    }
                }
                return 0.0;
            }

            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                return 0.0; // Kỹ năng này không giảm sát thương nhận vào
            }
        });
    }

    @Override public String getId() { return "warrior_meditation"; }
    @Override public String getName() { return "§b§lTĩnh Tâm Thiền Định"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.LIGHT_BLUE_DYE; }
    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 10; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public boolean isPassiveSkill() { return false; }
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 18000L; } // 18 Giây gốc

    @Override
    public List<String> getDescription(int level) {
        int rage = RAGE_GEN[level];
        int buff = (int)(BUFF_DMG[level] * 100);

        return Arrays.asList(
                "§7Tiến vào trạng thái vô ngã, tịnh tâm hồi phục Nộ khí.",
                "",
                "§b[Kích hoạt: Thiền Định]",
                "§f- Đứng yên tuyệt đối để vận công trong tối đa §a8 giây§f.",
                "§f- Mỗi giây phục hồi §c" + rage + " Nộ khí§f.",
                "§f- Kỹ năng sẽ bị ngắt nếu bạn §eNgồi (Shift)§f hoặc §cbị tấn công§f.",
                "",
                "§e[Nội tại: Tịnh Tâm]",
                "§f- Nếu duy trì đủ 8 giây không bị ngắt quãng, bạn sẽ",
                "§f  nhận thêm §c" + buff + "% Sát thương§f trong §a7 giây§f.",
                "",
                "§8(Mẹo: Nếu ngắt thiền sớm, thời gian hồi chiêu sẽ được",
                "§8giảm đi tương đương với số giây chưa thiền xong.)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // NẾU ĐANG THIỀN MÀ BẤM LẠI -> NGẮT THIỀN
        if (player.hasMetadata("Meditation_Channeling")) {
            player.setMetadata("Meditation_Interrupted", new FixedMetadataValue(plugin, true));
            return true;
        }

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey) && System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
            player.sendMessage("§cKỹ năng đang hồi chiêu!");
            return false;
        }

        // BẮT ĐẦU THIỀN
        StatusEffectManager.applyAbsoluteRoot(player, 160); // Khóa 8 giây
        player.setMetadata("Meditation_Channeling", new FixedMetadataValue(plugin, true));
        player.removeMetadata("Meditation_Interrupted", plugin);

        player.sendMessage("§b§l[!] §fBắt đầu thiền...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);

        int rageAmount = RAGE_GEN[level];

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.hasMetadata("Meditation_Channeling")) {
                    this.cancel();
                    return;
                }

                // KIỂM TRA ĐIỀU KIỆN NGẮT CHIÊU (Bấm Shift hoặc Bị Đánh Trúng)
                if (player.hasMetadata("Meditation_Interrupted") || player.isSneaking()) {
                    finishMeditation(player, rpgPlayer, level, ticks, false);
                    this.cancel();
                    return;
                }

                // HỒI NỘ MỖI GIÂY (20 Ticks)
                if (ticks > 0 && ticks % 20 == 0) {
                    // [ĐÃ SỬA]: Gọi đúng hàm getTotalMaxResource() của RPGPlayer
                    double maxRage = rpgPlayer.getTotalMaxResource();
                    double currentRage = rpgPlayer.getCurrentResource();
                    rpgPlayer.setCurrentResource(Math.min(maxRage, currentRage + rageAmount));

                    // VFX Nhỏ mỗi giây
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4, 0);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                }

                // VFX Tĩnh tâm liên tục
                if (ticks % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 10, 0.5, 1.0, 0.5, 0.1);
                }

                ticks++;

                // HOÀN THÀNH 8 GIÂY (160 Ticks)
                if (ticks >= 160) {
                    finishMeditation(player, rpgPlayer, level, ticks, true);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true; // Không tính CD ở đây, CD sẽ tính lúc ngắt chiêu
    }

    private void finishMeditation(Player player, RPGPlayer rpgPlayer, int level, int ticksChanneled, boolean success) {
        StatusEffectManager.removeAbsoluteRoot(player);
        player.removeMetadata("Meditation_Channeling", plugin);
        player.removeMetadata("Meditation_Interrupted", plugin);
        player.stopSound(Sound.BLOCK_BEACON_AMBIENT);

        long baseCd = getCooldown(level);

        if (success) {
            // THÀNH CÔNG: Cấp Buff
            player.sendMessage("§a§l[!] §fThiền hoàn tất! Nhận " + (int)(BUFF_DMG[level] * 100) + "% Sát thương trong 7s.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

            long expireTime = System.currentTimeMillis() + 7000L; // 7 giây
            player.setMetadata("Meditation_Damage_Buff", new FixedMetadataValue(plugin, BUFF_DMG[level]));
            player.setMetadata("Meditation_Buff_Expire", new FixedMetadataValue(plugin, expireTime));
        } else {
            // THẤT BẠI: Tính toán hoàn trả CD
            int secondsChanneled = ticksChanneled / 20;
            int remainingSec = 8 - secondsChanneled;

            // Ví dụ: Thiền 8s, ngắt lúc 1s -> Giảm 7s CD. CD = 18 - 7 = 11s.
            baseCd = baseCd - (remainingSec * 1000L);

            player.sendMessage("§c§l[!] §fThiền bị gián đoạn!");
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
        }

        // Tính toán với hệ số giảm hồi chiêu của Server (Nếu có)
        long finalCd = getFinalCooldown(rpgPlayer, baseCd);
        player.setMetadata("CD_" + getId(), new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
    }

    // ==========================================
    // BẮT SỰ KIỆN BỊ ĐÁNH TRÚNG ĐỂ NGẮT THIỀN
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.hasMetadata("Meditation_Channeling")) {
                // Gắn cờ để Task tự động hủy chiêu
                player.setMetadata("Meditation_Interrupted", new FixedMetadataValue(plugin, true));
            }
        }
    }
}