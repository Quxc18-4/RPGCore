package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementalFusionSkill implements ISkill {

    private final JavaPlugin plugin;

    public ElementalFusionSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_elemental_fusion"; }
    @Override public String getName() { return "Ma Pháp Dung Hợp"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.NETHER_STAR; }
    @Override public boolean isPassiveSkill() { return false; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 20; }

    // ==========================================
    // [CẬP NHẬT]: ĐIỀU KIỆN MỞ KHÓA KỸ NĂNG
    // ==========================================
    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> reqs = new HashMap<>();
        reqs.put("mage_charge_attack", 5);     // Tụ lực Lv 5
        reqs.put("mage_elemental_blast", 5);   // Burst Lv 5
        reqs.put("mage_elemental_sphere", 5);  // Sphere Lv 5
        return reqs;
    }

    // ==========================================
    // [CẬP NHẬT]: TỐI ƯU HÓA MANA COST
    // ==========================================
    @Override
    public double getManaCost(int level) {
        double cost = 90.0;
        if (level >= 3) cost += 20.0;
        if (level >= 6) cost += 20.0;
        if (level >= 9) cost += 20.0;
        return cost; // Mốc: 90 -> 110 -> 130 -> 150
    }

    @Override public long getCooldown(int level) { return (long) ((18.0 - (level * 0.5)) * 1000L); }

    @Override public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int lv = Math.max(1, level);
        // Sát thương gốc: Cấp 1 (352%) -> Cấp 10 (550%)
        double baseDmgPct = 330.0 + (lv * 22.0);

        MageElement nth = MageElement.FIRE;
        MageElement tnt = nth;
        String tntName = "§8(Chưa có)";

        if (player != null) {
            nth = ElementalSwapSkill.getActiveElement(player);
            if (player.hasMetadata("Mage_Previous_Element")) {
                try {
                    tnt = MageElement.valueOf(player.getMetadata("Mage_Previous_Element").get(0).asString());
                    tntName = tnt.getDisplayName();
                } catch (Exception ignored) {}
            } else {
                tnt = nth;
            }
        }

        // ==========================================
        // KHỞI TẠO LORE CƠ BẢN
        // ==========================================
        List<String> lore = new java.util.ArrayList<>(Arrays.asList(
                "§7Dung hợp 2 loại nguyên tố với nhau, sự giao thoa của",
                "§7hiện tại và quá khứ để kích hoạt phản ứng dữ dội.",
                "",
                "§8▪ §fNguyên Tố Trước: " + tntName,
                "§8▪ §fNguyên Tố Hiện Tại: " + nth.getDisplayName(),
                "",
                "§f- Sát thương gốc: §b" + (int)baseDmgPct + "% §fSát thương kỹ năng.",
                "",
                "§e[Kích hoạt - Hình thái Tối thượng]",
                "§7Tùy thuộc vào tổ hợp Nguyên tố, Ma Pháp Dung Hợp sẽ",
                "§7biến đổi hình thái và vùng ảnh hưởng:"
        ));

        // ==========================================
        // DYNAMIC LORE: TỔ HỢP ĐỒNG NGUYÊN TỐ (AAA)
        // ==========================================
        if (tnt == MageElement.FIRE && nth == MageElement.FIRE) {
            double finalDmg = baseDmgPct * 1.40;
            lore.add("§8▪ §c§lĐẠI HỎA CẦU §8(§cLửa §f+ §cLửa§8)");
            lore.add("§f- Triệu hồi Thiên thạch rực lửa giáng xuống khu vực.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");

        } else if (tnt == MageElement.WATER && nth == MageElement.WATER) {
            double finalDmg = baseDmgPct * 1.25;
            lore.add("§8▪ §b§lHẢI THẦN NỘ §8(§bNước §f+ §bNước§8)");
            lore.add("§f- Dựng cột nước khổng lồ hất văng mục tiêu lên không.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");

        } else if (tnt == MageElement.NATURE && nth == MageElement.NATURE) {
            double finalDmg = baseDmgPct * 1.25;
            lore.add("§8▪ §2§lMỘC THẦN ĐẠO §8(§2Thảo §f+ §2Thảo§8)");
            lore.add("§f- Trải thảm dây leo thẳng về phía trước (Duy trì 3s).");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fST và §aLàm chậm 30%§f.");

        } else if (tnt == MageElement.WIND && nth == MageElement.WIND) {
            double finalDmg = baseDmgPct * 1.25;
            lore.add("§8▪ §a§lCUỒNG PHONG TÁP §8(§aGió §f+ §aGió§8)");
            lore.add("§f- Phóng Lốc xoáy kéo rê kẻ địch và nổ ở cuối đường.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");

        } else if (tnt == MageElement.LIGHT && nth == MageElement.LIGHT) {
            double finalDmg = baseDmgPct * 1.40;
            lore.add("§8▪ §e§lQUANG THẦN PHÁO §8(§eSáng §f+ §eSáng§8)");
            lore.add("§f- Bắn tia Laser xuyên thấu tức thời mọi mục tiêu.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");

        } else if (tnt == MageElement.DARK && nth == MageElement.DARK) {
            double finalDmg = baseDmgPct * 1.20;
            lore.add("§8▪ §5§lHƯ KHÔNG ĐOẠN LIỆT §8(§5Tối §f+ §5Tối§8)");
            lore.add("§f- Rẽ Rãnh nứt Hư không (3s), Gây §c§l" + (int)finalDmg + "% §fST.");
            lore.add("§f- Kẻ địch bước lên bị Làm chậm và §cGiảm 10% DMG gây ra§f.");

            // ==========================================
            // DYNAMIC LORE: TỔ HỢP PHẢN ỨNG (REACTIONS)
            // ==========================================
        } else if (isMatch(tnt, nth, MageElement.FIRE, MageElement.WATER)) {
            double finalDmg = baseDmgPct * 1.50;
            lore.add("§8▪ §c§lBỐC HƠI §8(§cLửa §f+ §bNước§8)");
            lore.add("§f- Phóng Sóng nhiệt hình nón càn quét phía trước.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");

        } else if (isMatch(tnt, nth, MageElement.LIGHT, MageElement.DARK)) {
            double finalDmg = baseDmgPct * 1.50;
            lore.add("§8▪ §d§lHỖN ĐỘN §8(§eSáng §f+ §5Tối§8)");
            lore.add("§f- Xé rách không gian, gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");
            lore.add("§f- Áp dụng bùa hại: Phá §c20% Giáp & Kháng Phép §ftrong 5s.");

        } else if (isMatch(tnt, nth, MageElement.FIRE, MageElement.LIGHT)) {
            double finalDmg = baseDmgPct * 1.50;
            lore.add("§8▪ §e§lCHỚP GIẬT §8(§cLửa §f+ §eSáng§8)");
            lore.add("§f- Gọi Sét diện rộng, gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");
            lore.add("§f- Tê liệt (Làm chậm cực mạnh) mọi mục tiêu trong 2s.");

        } else if (isMatch(tnt, nth, MageElement.FIRE, MageElement.NATURE)) {
            double finalDmg = baseDmgPct * 1.0;
            lore.add("§8▪ §6§lTHIÊU RỤI §8(§cLửa §f+ §2Thảo§8)");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương nổ ban đầu.");
            lore.add("§f- Thiêu đốt kẻ địch liên tục trong §c7 giây§f.");

        } else if (isMatch(tnt, nth, MageElement.FIRE, MageElement.DARK)) {
            double finalDmg = baseDmgPct * 1.0;
            lore.add("§8▪ §5§lHẮC HỎA §8(§cLửa §f+ §5Tối§8)");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương nổ ban đầu.");
            lore.add("§f- Rút §c5% Máu đã mất §fmỗi giây trong 4s (Max 350 DMG/s).");

        } else if (isMatch(tnt, nth, MageElement.WATER, MageElement.NATURE)) {
            double finalDmg = baseDmgPct * 1.0;
            lore.add("§8▪ §a§lSINH SÔI §8(§bNước §f+ §2Thảo§8)");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");
            lore.add("§f- Trói chân kẻ địch trong §a3s §fvà Hồi phục §a5% Max HP§f.");

        } else {
            // MẶC ĐỊNH (Phòng hờ trường hợp Nước + Gió chưa có Code)
            double finalDmg = baseDmgPct * 1.15;
            lore.add("§8▪ §b§lBÙNG NỔ THUẬT PHÁP");
            lore.add("§f- Kích nổ vùng không gian ma thuật xung quanh.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương kỹ năng.");
        }

        return lore;
    }

    private boolean isMatch(MageElement e1, MageElement e2, MageElement r1, MageElement r2) {
        return (e1 == r1 && e2 == r2) || (e1 == r2 && e2 == r1);
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        MageElement nth = ElementalSwapSkill.getActiveElement(player);
        MageElement tnt = nth;

        if (player.hasMetadata("Mage_Previous_Element")) {
            try {
                tnt = MageElement.valueOf(player.getMetadata("Mage_Previous_Element").get(0).asString());
            } catch (Exception ignored) {}
        }

        // CHỈ TÍNH HỆ SỐ TRUYỀN XUỐNG DƯỚI (Multiplier)
        double baseMultiplier = (330.0 + (level * 22.0)) / 100.0;

        player.sendMessage("§d§l[!] MA PHÁP DUNG HỢP: " + tnt.getDisplayName() + " §f+ " + nth.getDisplayName());

        fireFusionProjectile(player, rpgPlayer, tnt, nth, baseMultiplier);

        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }

    private void fireFusionProjectile(Player caster, RPGPlayer rpgPlayer, MageElement tnt, MageElement nth, double baseMultiplier) {
        Location startLoc = caster.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(1.5);

        caster.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.5f);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 20 || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                currentLoc.add(direction);

                currentLoc.getWorld().spawnParticle(tnt.getParticle(), currentLoc, 4, 0.2, 0.2, 0.2, 0.0);
                currentLoc.getWorld().spawnParticle(nth.getParticle(), currentLoc, 4, 0.2, 0.2, 0.2, 0.0);
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 1, 0.0, 0.0, 0.0, 0.0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    // TRUYỀN VECTOR HƯỚNG BAY VÀO ĐỂ TÍNH GÓC CHO SKILL CONE
                    triggerFusionExplosion(caster, rpgPlayer, currentLoc, direction, tnt, nth, baseMultiplier);
                    this.cancel();
                    return;
                }

                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (!(entity instanceof LivingEntity) || entity.equals(caster)) continue;

                    if (CombatManager.canTarget(caster, (LivingEntity) entity)) {
                        triggerFusionExplosion(caster, rpgPlayer, currentLoc, direction, tnt, nth, baseMultiplier);
                        this.cancel();
                        return;
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // [AAA ROUTER]: ĐIỀU HƯỚNG HIỆU ỨNG VÀ HITBOX
    // ==========================================
    // Thay thế hàm triggerFusionExplosion cũ bằng đúng 2 dòng này
    private void triggerFusionExplosion(Player caster, RPGPlayer rpgPlayer, Location loc, Vector direction, MageElement tnt, MageElement nth, double baseMultiplier) {
        // 1. Hủy TNT của người chơi để reset vòng lặp combo
        caster.removeMetadata("Mage_Previous_Element", RPGCore.getInstance());

        // 2. CHUYỂN GIAO TOÀN QUYỀN CHO LÕI ĐỊNH TUYẾN (ENGINE)
        vn.minecraft.rPGCore.skills.mage.fusion.FusionEngine.processFusionHit(caster, rpgPlayer, loc, direction, tnt, nth, baseMultiplier);
    }
}