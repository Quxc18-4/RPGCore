package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.managers.CombatManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AerialBombardmentSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AerialBombardmentSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "archer_aerial_bombardment"; }
    @Override public String getName() { return "§d§l[TUYỆT KỸ TỐI THƯỢNG: CHÂN NGÃ]"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.CACTUS_FLOWER; }

    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }

    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public int getRequiredPlayerLevel() { return 50; }

    @Override public double getManaCost(int level) { return 150.0 + (level * 40.0); }
    @Override
    public long getCooldown(int level) {
        return 45000L - ((level - 1) * 4000L);
    }

    private double getCastTime(int level) {
        return 2.0;
    }

    // [CẬP NHẬT]: Công thức sát thương 900% - 1100% - 1400%
    private double getDamageMultiplier(int level) {
        if (level == 3) return 14.0;
        if (level == 2) return 11.0;
        return 9.0;
    }

    @Override
    public List<String> getDescription(int level) {
        int dmgPercent = (int) (getDamageMultiplier(level) * 100);
        double cdSeconds = getCooldown(level) / 1000.0;

        return Arrays.asList(
                "§d§o\"Vì vẻ đẹp của nhân gian, vì sự rực rỡ của tương lai...\"",
                "",
                "§7Một ý chí thuần khiết lơ lửng giữa thời không vô định,",
                "§7hi sinh và kết tinh thành những cánh hoa pha lê rực rỡ",
                "§7dệt nên một câu chuyện rực rỡ khép lại thời đại hoàng kim.",
                "",
                "§f§l[THÔNG SỐ TUYỆT KỸ]",
                "§f▪ Sát thương: §c" + dmgPercent + "% §fSát thương mỗi phát bắn",
                "§f▪ Số đợt tấn công: §a" + level + " đợt",
                "",
                "§d§l❀ HOA PHA LÊ: NỞ RỘ ❀",
                "§7Mỗi mũi tên khi chạm đích sẽ kiêu hãnh nở rộ thành một",
                "§7đóa hoa pha lê, quét sạch mọi kẻ địch xung quanh.",
                "",
                "§5§o(Mũi tên mang theo ý chí của thiếu nữ thuần khiết nhất)"
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

        player.sendMessage("§d§l[!] TUYỆT KỸ TỐI THƯỢNG: CHÂN NGÃ!");
        player.setMetadata("Is_Casting", new FixedMetadataValue(plugin, true));

        double dmgMulti = getDamageMultiplier(level);

        // Khai báo biến loc lấy vị trí của người chơi
        Location loc = player.getLocation();
        // ==========================================
        // 1. CẤT CÁNH: BÃO HOA HỒNG PASTEL MAGENTA
        // ==========================================
        Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 182, 193), 2.0f);
        Particle.DustOptions magentaDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 153, 255), 1.5f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);

        player.getWorld().spawnParticle(Particle.DUST, loc, 50, 0.5, 0.5, 0.5, pinkDust);
        player.getWorld().spawnParticle(Particle.DUST, loc, 30, 1.0, 1.0, 1.0, whiteDust);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > 15) { this.cancel(); return; }
                double radius = 1.0 + t * 0.1;
                for (int i = 0; i < 5; i++) {
                    double angle = i * (Math.PI * 2 / 5) + (t * 0.3);
                    Location pLoc = loc.clone().add(Math.cos(angle) * radius, t * 0.3, Math.sin(angle) * radius);
                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, magentaDust);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);        player.setVelocity(new Vector(0, 1.2, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.5f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

        int ticksPerCast = (int) (getCastTime(level) * 20);
        int totalArrows = level;

        new BukkitRunnable() {
            int globalTicks = 0;
            int currentArrow = 0;
            int currentCastTick = 0;

            private void finishSkill() {
                player.removeMetadata("Is_Casting", plugin);

                // Gắn cờ Universal Safe Fall thay vì cờ cũ
                player.setMetadata("RPGCore_SafeFall", new FixedMetadataValue(plugin, true));

                // Thu hồi quyền bay của CustomHover nếu nó chưa kịp hết
                vn.minecraft.rPGCore.managers.StatusEffectManager.removeCustomHover(player);

                // ==========================================
                // 2. ĐÁP ĐẤT: RAYCAST HÃM PHANH & MÂY HỒNG ĐỠ
                // ==========================================
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline() || player.isDead()) {
                            this.cancel(); return;
                        }

                        if (player.isOnGround()) {
                            // Cờ SafeFall sẽ do PlayerListener tự xóa khi chạm đất
                            this.cancel(); return;
                        }

                        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 2.5);
                        if (result != null && result.getHitBlock() != null) {
                            player.setVelocity(new Vector(0, -0.05, 0));
                            player.setFallDistance(0);

                            Particle.DustOptions cloudDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 228, 225), 3.0f);
                            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().subtract(0, 0.2, 0), 30, 1.0, 0.2, 1.0, cloudDust);
                            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.5, 0.1, 0.5, 0.05);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

                            // Dự phòng: Xóa cờ sau 2s nếu nhảy xuống vực sâu không chạm đất ngay
                            new BukkitRunnable() {
                                @Override public void run() { if (player.isOnline() && player.hasMetadata("RPGCore_SafeFall")) player.removeMetadata("RPGCore_SafeFall", plugin); }
                            }.runTaskLater(plugin, 40L);

                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                // KHÔNG CẦN DÙNG setGravity(true) nữa!
                this.cancel();
            }

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    finishSkill();
                    return;
                }

                // Dùng hệ thống Hover Universal (An toàn 100% khi Disconnect)
                if (globalTicks == 10) {
                    player.setVelocity(new Vector(0, 0, 0));
                    // Cho lơ lửng bằng thời gian cast (ticksPerCast) + 10 tick trễ
                    StatusEffectManager.applyCustomHover(player, ticksPerCast + 10, 0.0f, true);
                }

                if (globalTicks >= 10) {
                    // Ép vị trí liên tục thay vì tắt Gravity
                    player.setVelocity(new Vector(0, 0, 0));

                    Location eyeLoc = player.getEyeLocation();
                    Vector dir = eyeLoc.getDirection().normalize();
                    Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                    Vector up = right.clone().crossProduct(dir).normalize();

                    // MÀU SẮC ĐÃ ĐƯỢC LÀM NHẠT VÀ TƯƠI SÁNG HƠN (Pink Petal Theme)
                    Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.2f);
                    Particle.DustOptions lightMagentaDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 153, 255), 1.0f); // Tím nhạt
                    Particle.DustOptions whitePinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 228, 225), 0.9f); // Trắng Hồng

                    // ==========================================
                    // 1. CÁNH HÌNH THOI (SAU LƯNG)
                    // ==========================================
                    Location backCenter = eyeLoc.clone().subtract(dir.clone().multiply(0.5)).subtract(up.clone().multiply(0.2));
                    double backRectW = 0.5;
                    double backRectH = 0.8;

                    for (int dx : new int[]{-1, 1}) {
                        for (int dy : new int[]{-1, 1}) {
                            Location corner = backCenter.clone().add(right.clone().multiply(dx * backRectW)).add(up.clone().multiply(dy * backRectH));
                            double scale = (dy == 1) ? 1.5 : 0.9;
                            double length = 2.5 * scale;
                            double maxWidth = 0.7 * scale;

                            Vector outDir = right.clone().multiply(dx).add(up.clone().multiply(dy)).normalize();
                            outDir = outDir.add(dir.clone().multiply(-0.4)).normalize();
                            Vector crossDir = outDir.clone().crossProduct(dir).normalize();

                            for (double t = 0; t <= length; t += 0.3) {
                                double currentWidth = (t < length * 0.25) ? maxWidth * (t / (length * 0.25)) : maxWidth * (1.0 - (t - length * 0.25) / (length * 0.75));
                                for (double w = -currentWidth; w <= currentWidth; w += 0.2) {
                                    Location pLoc = corner.clone().add(outDir.clone().multiply(t)).add(crossDir.clone().multiply(w));
                                    Particle.DustOptions color = (Math.abs(w) >= currentWidth - 0.25) ? lightMagentaDust : whitePinkDust;
                                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, color);
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 2. TƠ RUA BẺ CONG (TRƯỚC MẶT)
                    // ==========================================
                    Location frontCenter = eyeLoc.clone().add(dir.clone().multiply(2.0));
                    double frontRectW = 1.15;
                    double frontRectH = 1.25;

                    for (int dx : new int[]{-1, 1}) {
                        for (int dy : new int[]{-1, 1}) {
                            Location corner = frontCenter.clone().add(right.clone().multiply(dx * frontRectW)).add(up.clone().multiply(dy * frontRectH));
                            Vector outDir = right.clone().multiply(dx).add(up.clone().multiply(dy)).normalize();
                            Vector crossDir = outDir.clone().crossProduct(dir).normalize();

                            double length = 3.5;
                            double maxWidth = 0.3;

                            for (double t = 0; t <= length; t += 0.2) {
                                Vector lateralOutDir = right.clone().multiply(dx * (1.0 + t * 0.5)).add(up.clone().multiply(dy * (1.0 + t * 0.5))).normalize();
                                double currentWidth = maxWidth * (1.0 - (t / length));
                                Vector curveOffset = dir.clone().multiply(-t * t * 0.35);

                                for (double w = -currentWidth; w <= currentWidth; w += 0.1) {
                                    Location pLoc = corner.clone().add(lateralOutDir.clone().multiply(t)).add(crossDir.clone().multiply(w)).add(curveOffset);
                                    Particle.DustOptions color = (Math.abs(w) >= currentWidth - 0.1) ? lightMagentaDust : whiteDust;
                                    player.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, color);
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 3. TÂM NGẮM CHỮ V
                    // ==========================================
                    Location vCenter = frontCenter.clone().subtract(up.clone().multiply(0.85));
                    Location leftTip = vCenter.clone().add(right.clone().multiply(0.6)).add(up.clone().multiply(0.5));
                    Location rightTip = vCenter.clone().add(right.clone().multiply(-0.6)).add(up.clone().multiply(0.5));

                    leftTip.add(dir.clone().multiply(0.2));
                    rightTip.add(dir.clone().multiply(0.2));

                    for (double t = 0; t <= 1.0; t += 0.1) {
                        player.getWorld().spawnParticle(Particle.DUST, vCenter.clone().add(leftTip.clone().subtract(vCenter).multiply(t)), 1, 0, 0, 0, 0, whiteDust);
                        player.getWorld().spawnParticle(Particle.DUST, vCenter.clone().add(rightTip.clone().subtract(vCenter).multiply(t)), 1, 0, 0, 0, 0, whiteDust);
                    }

                    // ==========================================
                    // GIAI ĐIỆU HOA PHA LÊ NỞ (CRYSTAL MELODY)
                    // ==========================================
                    if (currentCastTick % 5 == 0) {
                        // Dải âm giai ngũ cung (Pentatonic) gồm 8 nốt vút dần lên cao
                        float[] melody = {0.707f, 0.793f, 0.890f, 1.059f, 1.189f, 1.414f, 1.781f, 2.0f};

                        int noteIndex = (currentCastTick / 5);
                        if (noteIndex >= melody.length) noteIndex = melody.length - 1;

                        float pitch = melody[noteIndex];

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, pitch);
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, pitch);
                    }

                    // ==========================================
                    // LOGIC BẮN ĐẠN
                    // ==========================================
                    currentCastTick++;

                    if (currentCastTick >= ticksPerCast) {
                        fireExplosiveBomb(player, dmgMulti);
                        currentArrow++;
                        currentCastTick = 0;

                        if (currentArrow >= totalArrows) {
                            finishSkill();
                            return;
                        }
                    }
                }
                globalTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==========================================
    // 3. MŨI TÊN PARTICLE & 2 DẢI LỐC XOÁY (VORTEX)
    // ==========================================
    private void fireExplosiveBomb(Player player, double dmgMulti) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize().multiply(2.5);

        Arrow arrow = player.getWorld().spawnArrow(eyeLoc, dir, 2.5f, 0f);
        arrow.setShooter(player);
        arrow.setGravity(false);
        arrow.setSilent(true);

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 2.0f);
        player.getWorld().playSound(eyeLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.5f);

        Particle.DustOptions arrowColor = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 105, 180), 1.2f);
        Particle.DustOptions vortexColor = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 228, 225), 1.0f);

        new BukkitRunnable() {
            Location startLoc = arrow.getLocation();
            int t = 0; // Bộ đếm cho Vortex

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) { this.cancel(); return; }

                double dist = startLoc.distance(arrow.getLocation());

                LivingEntity hitTarget = null;
                for (org.bukkit.entity.Entity e : arrow.getWorld().getNearbyEntities(arrow.getLocation(), 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        hitTarget = (LivingEntity) e; break;
                    }
                }

                boolean hitBlock = arrow.isInBlock();
                boolean outOfRange = dist >= 30.0;

                if (hitTarget != null || hitBlock || outOfRange) {
                    Location explodeLoc = arrow.getLocation();

                    if (hitTarget != null) {
                        Location feetLoc = hitTarget.getLocation();
                        if (feetLoc.clone().subtract(0, 0.5, 0).getBlock().getType().isSolid() || ((org.bukkit.entity.Entity)hitTarget).isOnGround()) {
                            createCrystalBloom(player, feetLoc, dmgMulti);
                        } else {
                            createMidAirLoveArrows(player, hitTarget.getLocation(), dmgMulti);
                        }
                    } else if (hitBlock) {
                        createCrystalBloom(player, explodeLoc, dmgMulti);
                    } else if (outOfRange) {
                        createMidAirLoveArrows(player, explodeLoc, dmgMulti);
                    }

                    arrow.remove();
                    this.cancel();
                    return;
                }

                // VẼ MŨI TÊN & LỐC XOÁY KÉP (DOUBLE VORTEX)
                Location current = arrow.getLocation();
                Vector vDir = arrow.getVelocity().normalize();

                // Trục hạt trung tâm
                arrow.getWorld().spawnParticle(Particle.END_ROD, current, 1, 0, 0, 0, 0);
                for (double i = 0; i <= 1.5; i += 0.3) {
                    arrow.getWorld().spawnParticle(Particle.DUST, current.clone().subtract(vDir.clone().multiply(i)), 1, arrowColor);
                }

                // Toán học vẽ 2 dải Vortex xoay quanh hướng bay
                Vector up = new Vector(0, 1, 0);
                if (Math.abs(vDir.getY()) > 0.9) up = new Vector(1, 0, 0);
                Vector ortho1 = vDir.clone().crossProduct(up).normalize();
                Vector ortho2 = vDir.clone().crossProduct(ortho1).normalize();

                double vRadius = 0.6; // Độ rộng của vòng lốc xoáy
                double angle = t * 0.8; // Tốc độ xoay

                // Điểm xoắn 1
                Location p1 = current.clone().subtract(vDir.clone().multiply(0.5))
                        .add(ortho1.clone().multiply(Math.cos(angle) * vRadius))
                        .add(ortho2.clone().multiply(Math.sin(angle) * vRadius));
                // Điểm xoắn 2 (Đối xứng 180 độ)
                Location p2 = current.clone().subtract(vDir.clone().multiply(0.5))
                        .add(ortho1.clone().multiply(Math.cos(angle + Math.PI) * vRadius))
                        .add(ortho2.clone().multiply(Math.sin(angle + Math.PI) * vRadius));

                arrow.getWorld().spawnParticle(Particle.DUST, p1, 1, vortexColor);
                arrow.getWorld().spawnParticle(Particle.DUST, p2, 1, vortexColor);
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 4. VẼ ĐÓA HOA PHA LÊ (NỞ ĐỒNG LOẠT BÙNG NỔ CÓ TỔ CHỨC)
    // Đã căn chỉnh lại góc độ để hoa vươn cao, thanh thoát hơn
    // ==========================================
    private void createCrystalBloom(Player player, Location loc, double dmgMulti) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.8f);

        Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 182, 193), 1.2f);
        Particle.DustOptions lightMagentaDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 153, 255), 1.2f);
        Particle.DustOptions whitePinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 228, 225), 1.5f);

        Location centerBase = loc.clone();
        org.bukkit.util.RayTraceResult centerRay = loc.getWorld().rayTraceBlocks(loc.clone().add(0, 2, 0), new Vector(0, -1, 0), 4.0);
        if (centerRay != null && centerRay.getHitBlock() != null) {
            centerBase = centerRay.getHitPosition().toLocation(loc.getWorld());
        } else {
            centerBase = loc.clone().subtract(0, 0.5, 0);
        }

        Location finalCenter = centerBase.clone().add(0, 0.2, 0);

        // NỞ HOA NGAY LẬP TỨC

        // 1. LÕI TRUNG TÂM (Đâm thẳng lên trời)
        drawCrystalSpike(finalCenter, new Vector(0, 1, 0), 5.5, 1.5, pinkDust, lightMagentaDust, loc.getWorld());

        // 2. LỚP CÁNH TRONG (Góc 60 độ - Ngả đều ra xung quanh)
        for(int i = 0; i < 6; i++) {
            double azimuth = i * (Math.PI / 3);
            double elevation = Math.PI / 3; // ~60 độ (Giảm từ 72 độ)
            Vector dir = new Vector(Math.cos(elevation)*Math.cos(azimuth), Math.sin(elevation), Math.cos(elevation)*Math.sin(azimuth));
            drawCrystalSpike(finalCenter, dir, 4.5, 1.5, whitePinkDust, pinkDust, loc.getWorld());
        }

        // 3. LỚP CÁNH NGOÀI (Góc 30 độ - Tỏa rộng mượt mà)
        for(int i = 0; i < 8; i++) {
            double azimuth = i * (Math.PI / 4) + (Math.PI / 8);
            double elevation = Math.PI / 6; // ~30 độ (Giảm từ ~51 độ)
            Vector dir = new Vector(Math.cos(elevation)*Math.cos(azimuth), Math.sin(elevation), Math.cos(elevation)*Math.sin(azimuth));
            drawCrystalSpike(finalCenter, dir, 6.0, 2.2, pinkDust, lightMagentaDust, loc.getWorld());
        }

        // 4. LỚP CÁNH BỆ ĐỠ BÊN DƯỚI (14 cánh, Góc 10 độ - Đài hoa dày đặc bám đất)
        for(int i = 0; i < 14; i++) { // Tăng từ 10 lên 14 cánh
            double azimuth = i * (Math.PI / 7); // Chia đều 14 hướng
            double elevation = Math.PI / 18; // ~10 độ (Hơi nhích nhẹ khỏi mặt đất)
            Vector dir = new Vector(Math.cos(elevation)*Math.cos(azimuth), Math.sin(elevation), Math.cos(elevation)*Math.sin(azimuth));
            drawCrystalSpike(finalCenter, dir, 7.0, 2.8, whitePinkDust, pinkDust, loc.getWorld());
        }

        loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, finalCenter.clone().add(0, 2, 0), 50, 4.0, 2.0, 4.0, 0.05);

        applyAoEDamage(player, finalCenter, dmgMulti, 7.0);
    }

    // ==========================================
    // 5. NỔ LƠ LỬNG TRÊN KHÔNG & MƯA TÊN (STARBURST 3D)
    // ==========================================
    private void createMidAirLoveArrows(Player player, Location loc, double dmgMulti) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.5f, 1.2f);

        Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 182, 193), 1.2f);
        Particle.DustOptions magenta = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 105, 180), 1.5f);

        // VẼ HOA KHÔNG GIAN BÙNG NỔ (Sea Urchin Style)
        // Thay vì chỉ vươn lên, đóa hoa lơ lửng sẽ đâm gai đều ra CẢ BÊN TRÊN LẪN BÊN DƯỚI
        for (int i = 0; i < 14; i++) {
            double phi = Math.acos(1 - 2 * ((i + 0.5) / 14)); // Phân bổ đều hình cầu
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            Vector dir = new Vector(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta)).normalize();

            drawCrystalSpike(loc, dir, 4.0, 1.5, pinkDust, magenta, loc.getWorld());
        }
        loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 30, 2.0, 2.0, 2.0, 0.05);

        // TÌM MẶT ĐẤT ĐỂ TẠO MƯA TÊN & NỞ HOA DƯỚI ĐẤT
        Location groundCenter = loc.clone();
        org.bukkit.util.RayTraceResult downRay = loc.getWorld().rayTraceBlocks(loc, new Vector(0, -1, 0), 40.0);
        if (downRay != null && downRay.getHitBlock() != null) {
            groundCenter = downRay.getHitPosition().toLocation(loc.getWorld());
        } else {
            groundCenter.subtract(0, 15, 0);
        }

        Location finalGround = groundCenter;

        // Gọi hoa nở bùm dưới mặt đất
        createCrystalBloom(player, finalGround, dmgMulti);

        // Mưa tên dội xuống
        new BukkitRunnable() {
            int arrowCount = 0;
            @Override
            public void run() {
                if (arrowCount > 25) { this.cancel(); return; }

                double offsetX = (Math.random() - 0.5) * 10.0;
                double offsetZ = (Math.random() - 0.5) * 10.0;

                Location startDrop = loc.clone().add(offsetX, 0, offsetZ);
                Location endDrop = finalGround.clone().add(offsetX, 0, offsetZ);

                org.bukkit.util.RayTraceResult dropRay = loc.getWorld().rayTraceBlocks(startDrop.clone().add(0, 2, 0), new Vector(0, -1, 0), 40.0);
                if (dropRay != null && dropRay.getHitBlock() != null) {
                    endDrop = dropRay.getHitPosition().toLocation(loc.getWorld());
                }

                drawParticleLine(startDrop, endDrop, magenta, loc.getWorld());
                loc.getWorld().spawnParticle(Particle.HEART, endDrop.clone().add(0, 0.5, 0), 1);

                arrowCount++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Gây sát thương lơ lửng cho quái bay R=4.0
        applyAoEDamage(player, loc, dmgMulti, 4.0);
    }

    // ==========================================
    // HÀM TOÁN HỌC VẼ 1 CÁNH HOA PHA LÊ (pointed pyramid - Chóp ngũ giác nhọn)
    // Phối hợp với ảnh reference để tạo khối glowing từ lõi và viền hình học
    // ==========================================
    private void drawCrystalSpike(Location center, Vector dir, double length, double baseRadius, Particle.DustOptions faceColor, Particle.DustOptions edgeColor, org.bukkit.World world) {
        Location topCenter = center.clone().add(dir.clone().multiply(length)); // Đỉnh nhọn (Apex)

        Vector up = new Vector(0, 1, 0);
        if (Math.abs(dir.getY()) > 0.9) up = new Vector(1, 0, 0); // Đề phòng lỗi chia 0 nếu đâm thẳng đứng
        Vector ortho1 = dir.clone().crossProduct(up).normalize();
        Vector ortho2 = dir.clone().crossProduct(ortho1).normalize();

        int sides = 5; // Dùng chóp ngũ giác cho giống ảnh reference
        double angleOffset = Math.random() * Math.PI; // Xoay nhẹ từng chóp

        // --- VẼ LÕI TRUNG TÂM GLOWING (FACE COLOR - HỒNG/MAGENTA) ---
        // Nối từ tâm gốc (center) lên đỉnh nhọn (topCenter) của từng chóp
        drawParticleLine(center, topCenter, faceColor, world);

        for (int j = 0; j < sides; j++) {
            double angle = angleOffset + j * (2 * Math.PI / sides);
            double nextAngle = angleOffset + ((j + 1) % sides) * (2 * Math.PI / sides);

            // 1. Tọa độ các góc của 1 cạnh ĐÁY (Nằm sát tâm center)
            Location baseCorner = center.clone()
                    .add(ortho1.clone().multiply(Math.cos(angle) * baseRadius))
                    .add(ortho2.clone().multiply(Math.sin(angle) * baseRadius));
            Location nextBaseCorner = center.clone()
                    .add(ortho1.clone().multiply(Math.cos(nextAngle) * baseRadius))
                    .add(ortho2.clone().multiply(Math.sin(nextAngle) * baseRadius));

            // --- BẮT ĐẦU VẼ CÁC ĐƯỜNG NỐI ---

            // 2. Vẽ viền hình học bên ngoài (EDGE COLOR - TRẮNG/BLUE)
            // Viền tứ giác ở Đáy
            drawParticleLine(baseCorner, nextBaseCorner, edgeColor, world);
            // Viền 5 cạnh bên nối từ Đáy chụm lên Đỉnh nhọn Apex (topCenter)
            drawParticleLine(baseCorner, topCenter, edgeColor, world);

            // 3. Vẽ các đường nối nội tâm (FACE COLOR - HỒNG/MAGENTA)
            // Nối từ tâm gốc (center) đến các góc đáy của chóp để lấp đầy không gian faces rỗng
            drawParticleLine(center, baseCorner, faceColor, world);
        }
    }

    // ==========================================
    // 6. GÂY SÁT THƯƠNG DIỆN RỘNG (ĐÃ FIX LỖI KO GÂY DMG)
    // ==========================================
    private void applyAoEDamage(Player player, Location loc, double dmgMulti, double radius) {
        RPGPlayer rpgPlayer = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        boolean hitSomething = false;

        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity target = (LivingEntity) e;
                boolean isCrossbow = player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW;
                double finalDamage = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 50.0, 0.0, 0.0, false, isCrossbow);

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, target, finalDamage);
                player.removeMetadata("Ghost_L_Blocker", plugin);

                hitSomething = true;
            }
        }

        if (hitSomething) {
            loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
        }
    }

    private void drawParticleLine(Location start, Location end, Particle.DustOptions dust, org.bukkit.World world) {
        Vector dirLine = end.toVector().subtract(start.toVector());
        double distLine = dirLine.length();
        if (distLine == 0) return;
        dirLine.normalize();
        for (double t = 0; t <= distLine; t += 0.2) {
            world.spawnParticle(Particle.DUST, start.clone().add(dirLine.clone().multiply(t)), 1, 0, 0, 0, 0, dust);
        }
    }
}