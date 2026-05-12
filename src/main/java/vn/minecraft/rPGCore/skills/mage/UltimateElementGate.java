package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class UltimateElementGate implements ISkill {

    private final JavaPlugin plugin;
    private final Map<MageElement, Integer> elementModelIds = new HashMap<>();

    public UltimateElementGate(JavaPlugin plugin) {
        this.plugin = plugin;
        // Mapping CustomModelData cho 6 hệ dựa trên Resource Pack của Sếp
        elementModelIds.put(MageElement.FIRE, 1001);
        elementModelIds.put(MageElement.WATER, 1002);
        elementModelIds.put(MageElement.WIND, 1003);
        elementModelIds.put(MageElement.NATURE, 1004);
        elementModelIds.put(MageElement.LIGHT, 1005);
        elementModelIds.put(MageElement.DARK, 1006);
    }

    // ==========================================
    // 1. THÔNG SỐ CƠ BẢN TỪ ISKILL
    // ==========================================
    @Override public String getId() { return "mage_ultimate_element_gate"; }
    @Override public String getName() { return "§6§l[TUYỆT KỸ TỐI THƯỢNG: VẠN PHÁP QUY TÔNG]"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.END_CRYSTAL; }

    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 50; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
    @Override public boolean isPassiveSkill() { return false; }

    // Năng lượng: Rất đắt (250 - 300 - 350)
    @Override public double getManaCost(int level) { return 200.0 + (level * 50.0); }
    // Hồi chiêu: 60s - 55s - 50s
    @Override public long getCooldown(int level) { return (65 - (level * 5)) * 1000L; }

    // ==========================================
    // 2. CÂN BẰNG SÁT THƯƠNG
    // Mỗi viên đạn cơ bản: 60% - 80% - 100% Sát thương
    // Kích nổ 3 Ấn: 400% - 500% - 600% Sát thương
    // ==========================================
    private double getBulletDamageMultiplier(int level) { return 0.4 + (level * 0.2); }
    private double getDetonateDamageMultiplier(int level) { return 3.0 + (level * 1.0); }

    @Override
    public List<String> getDescription(int level) {
        int bulletDmg = (int) (getBulletDamageMultiplier(level) * 100);
        int detonateDmg = (int) (getDetonateDamageMultiplier(level) * 100);

        return Arrays.asList(
                "§6§o\"Tri thức là sức mạnh, và ta nắm giữ mọi chân lý...\"",
                "",
                "§f§l[THÔNG SỐ TUYỆT KỸ]",
                "§7Triệu hồi các Vòng Ma Pháp sau lưng dựa trên số Nguyên Tố",
                "§7đã mở khóa. Liên tục nã đạn ma pháp theo hướng nhìn.",
                "",
                "§f▪ Thời gian duy trì: §a10 giây",
                "§f▪ Sát thương đạn: §c" + bulletDmg + "% §fMỗi viên",
                "",
                "§e§l❂ KÍCH ỨNG NGUYÊN TỐ ❂",
                "§7Đạn ma pháp áp §e[Ấn Nguyên Tố]§7 lên kẻ địch.",
                "§7Tích đủ §c3 Ấn§7 sẽ kích nổ diện rộng, gây thêm",
                "§c" + detonateDmg + "% §fSát thương chuẩn và xóa Ấn.",
                "",
                "§8§o*Mở khóa càng nhiều hệ, sức mạnh càng khủng khiếp."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // [ĐÃ SỬA]: Lấy danh sách tên nguyên tố (dạng String) từ RPGPlayer
        List<String> unlockedElementNames = rpgPlayer.getUnlockedElements();
        if (unlockedElementNames == null || unlockedElementNames.isEmpty()) {
            player.sendMessage("§cBạn chưa mở khóa bất kỳ nguyên tố nào!");
            return false;
        }

        // [MỚI]: Chuyển đổi các chuỗi tên nguyên tố sang Enum MageElement
        List<MageElement> unlockedElements = new ArrayList<>();
        for (String elName : unlockedElementNames) {
            try {
                unlockedElements.add(MageElement.valueOf(elName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Bỏ qua nếu có chuỗi rác không khớp với hệ thống Enum
            }
        }

        // Đề phòng trường hợp danh sách có lỗi
        if (unlockedElements.isEmpty()) {
            player.sendMessage("§cDữ liệu nguyên tố bị lỗi, không thể kích hoạt!");
            return false;
        }

        // Kiểm tra Mana & Cooldown từ Siêu Hàm
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;
        applyGlobalCooldown(player, rpgPlayer, level);

        player.sendMessage("§6§l[!] VẠN PHÁP QUY TÔNG!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);

        // ==========================================
        // 3. TRIỆU HỒI CÁC ITEM DISPLAY VÒNG MA PHÁP
        // ==========================================
        List<ItemDisplay> gates = new ArrayList<>();

        for (MageElement element : unlockedElements) {
            ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // DÙNG CÚ PHÁP MỚI CỦA 1.21 CHO CUSTOM MODEL DATA
            meta.setCustomModelData(elementModelIds.get(element));
            item.setItemMeta(meta);

            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED); // Dùng FIXED cho hiển thị phẳng
            display.setTeleportDuration(2); // Nội suy mượt mà trong 2 tick

            gates.add(display);
        }

        double bulletDmgMulti = getBulletDamageMultiplier(level);
        double detonateDmgMulti = getDetonateDamageMultiplier(level);

        // ==========================================
        // 4. VÒNG LẶP DUY TRÌ & TOÁN HỌC VECTOR
        // ==========================================
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 200; // 10 giây

            @Override
            public void run() {
                // Tự động hủy nếu người chơi out, chết hoặc hết giờ
                if (!player.isOnline() || player.isDead() || ticks >= maxTicks) {
                    gates.forEach(ItemDisplay::remove);
                    if (player.isOnline()) player.sendMessage("§8§oCác vòng ma pháp đã tan biến...");
                    this.cancel();
                    return;
                }

                Location eyeLoc = player.getEyeLocation();
                Vector lookDir = eyeLoc.getDirection().normalize();

                // Toán học lấy Trục ngang (Right) và Trục dọc (Up) dựa trên hướng nhìn
                Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
                Vector up = right.clone().crossProduct(lookDir).normalize();

                // Điểm tâm bán nguyệt (Lùi sau lưng 1.5 block, cao lên 0.5)
                Location haloCenter = eyeLoc.clone().subtract(lookDir.clone().multiply(1.5)).add(0, 0.5, 0);

                // ==========================================
                // XẾP CÁC VÒNG MA PHÁP THÀNH HÌNH CUNG (VẦNG HÀO QUANG)
                // ==========================================
                int count = gates.size();
                double arcSpread = Math.PI; // Trải rộng 180 độ
                double startAngle = -arcSpread / 2;
                double angleStep = count > 1 ? arcSpread / (count - 1) : 0;
                double radius = 1.8; // Khoảng cách từ tâm đến các vòng

                for (int i = 0; i < count; i++) {
                    ItemDisplay gate = gates.get(i);
                    double angle = startAngle + (i * angleStep);

                    // Tính tọa độ dựa trên góc
                    Location gateLoc = haloCenter.clone()
                            .add(right.clone().multiply(Math.sin(angle) * radius))
                            .add(up.clone().multiply(Math.cos(angle) * radius));

                    // Ép các vòng xoay mặt hướng về phía người chơi đang nhìn
                    gateLoc.setYaw(player.getLocation().getYaw());
                    gateLoc.setPitch(player.getLocation().getPitch());

                    gate.teleport(gateLoc);

                    // ==========================================
                    // NÃ ĐẠN MỖI 10 TICKS (0.5 Giây) / VÒNG
                    // Càng nhiều hệ -> Càng nhiều đạn!
                    // ==========================================
                    if (ticks % 10 == 0) {
                        fireMagicBullet(player, rpgPlayer, gateLoc, lookDir, bulletDmgMulti, detonateDmgMulti);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==========================================
    // 5. CƠ CHẾ ĐẠN MA PHÁP & KÍCH NỔ ẤN
    // ==========================================
    private void fireMagicBullet(Player shooter, RPGPlayer rpgPlayer, Location start, Vector dir, double bulletDmg, double detonateDmg) {
        shooter.getWorld().playSound(start, Sound.ENTITY_ALLAY_HURT, 0.5f, 2.0f);

        new BukkitRunnable() {
            Location current = start.clone();
            int t = 0;

            @Override
            public void run() {
                if (t > 30) { this.cancel(); return; } // Tầm bay tối đa 30 block

                current.add(dir.clone().multiply(1.5)); // Tốc độ bay: 1.5 block / tick

                // Hiệu ứng hạt của đạn
                shooter.getWorld().spawnParticle(Particle.END_ROD, current, 1, 0, 0, 0, 0.05);
                shooter.getWorld().spawnParticle(Particle.GLOW, current, 2, 0.1, 0.1, 0.1, 0.02);

                // Quét chạm quái
                for (org.bukkit.entity.Entity e : shooter.getWorld().getNearbyEntities(current, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                        LivingEntity target = (LivingEntity) e;

                        // Sát thương viên đạn chạm vào
                        shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double dmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, bulletDmg, 0, 0, 0, false, false);
                        CombatManager.applySkillDamage(shooter, target, dmg);
                        shooter.removeMetadata("Ghost_L_Blocker", plugin);

                        // TÍCH LŨY ẤN NGUYÊN TỐ
                        int marks = target.hasMetadata("Mage_Element_Marks") ? target.getMetadata("Mage_Element_Marks").get(0).asInt() : 0;
                        marks++;

                        if (marks >= 3) {
                            // KÍCH NỔ
                            target.removeMetadata("Mage_Element_Marks", plugin);
                            detonateMarks(shooter, rpgPlayer, target, detonateDmg);
                        } else {
                            // Cập nhật Ấn
                            target.setMetadata("Mage_Element_Marks", new FixedMetadataValue(plugin, marks));
                            shooter.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                        }

                        this.cancel();
                        return;
                    }
                }

                // Chạm tường thì hủy
                if (current.getBlock().getType().isSolid()) {
                    shooter.getWorld().spawnParticle(Particle.FIREWORK, current, 5, 0.2, 0.2, 0.2, 0.05);
                    this.cancel();
                }

                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void detonateMarks(Player shooter, RPGPlayer rpgPlayer, LivingEntity epicenter, double detonateDmgMulti) {
        Location loc = epicenter.getLocation().add(0, 1, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.5f);

        // Hiệu ứng nổ cầu vồng
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 50, 1.5, 1.5, 1.5, 0.1);

        // Sát thương chuẩn diện rộng (Bán kính 3 block)
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(shooter, e)) {
                shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));

                // Set true cho bỏ qua phòng thủ (Sát thương chuẩn)
                double splashDmg = CombatManager.calculateDamage(shooter, rpgPlayer, (LivingEntity)e, detonateDmgMulti, 0, 100.0, 0, true, false);
                CombatManager.applySkillDamage(shooter, (LivingEntity)e, splashDmg);

                shooter.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }
}