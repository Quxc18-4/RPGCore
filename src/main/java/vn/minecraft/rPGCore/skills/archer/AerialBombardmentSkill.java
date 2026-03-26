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
    @Override public String getName() { return "Tuyệt kỹ: Thiên Không Chi Tiễn"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.FIREWORK_ROCKET; }

    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }

    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public int getRequiredPlayerLevel() { return 40; }

    @Override public double getManaCost(int level) { return 150.0 + (level * 30.0); }
    @Override public long getCooldown(int level) { return 45000L; }

    private double getCastTime(int level) {
        if (level == 3) return 1.5;
        if (level == 2) return 1.8;
        return 2.0;
    }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§6§l[TUYỆT KỸ TỐI THƯỢNG]",
                "§7Mượn lực gió bay lên không trung, giương cung",
                "§7Thiên Không và oanh tạc đạn nổ diện rộng.",
                "",
                "§f- Thời gian vận nộ/mũi: §e" + getCastTime(level) + "s",
                "§f- Tổng số phát bắn: §a" + level + " phát",
                "§f- Sát thương Pha lê (R=4): §c250% / phát"
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

        player.sendMessage("§d§l[!] TUYỆT KỸ: THIÊN KHÔNG CHI TIỄN!");
        player.setMetadata("Is_Casting", new FixedMetadataValue(plugin, true));

        boolean wasAllowFlight = player.getAllowFlight();
        player.setAllowFlight(true);

        // BÙM! Nhảy lên trời
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
        player.setVelocity(new Vector(0, 1.2, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.5f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

        int ticksPerCast = (int) (getCastTime(level) * 20);
        int totalArrows = level;

        new BukkitRunnable() {
            int globalTicks = 0;
            int currentArrow = 0;
            int currentCastTick = 0;

            private void finishSkill() {
                player.setGravity(true);
                player.removeMetadata("Is_Casting", plugin);
                player.setFallDistance(0);
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(wasAllowFlight);
                }
                this.cancel();
            }

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    finishSkill();
                    return;
                }

                if (globalTicks == 10) {
                    player.setGravity(false);
                    player.setFlying(true);
                    player.setVelocity(new Vector(0, 0, 0));
                }

                if (globalTicks >= 10) {
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

                    if (currentCastTick % 8 == 0) {
                        float pitch = 1.0f + (currentCastTick / (float)ticksPerCast);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, pitch);
                    }

                    // ==========================================
                    // LOGIC BẮN ĐẠN
                    // ==========================================
                    currentCastTick++;

                    if (currentCastTick >= ticksPerCast) {
                        fireExplosiveBomb(player, 2.5, level);
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

    private void fireExplosiveBomb(Player player, double dmgMulti, int level) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize().multiply(3.5);

        Arrow arrow = player.getWorld().spawnArrow(eyeLoc, dir, 3.5f, 0f);
        arrow.setShooter(player);

        arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, 2.5));
        arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, 50.0));

        if (player.hasMetadata("Archer_Pierce_Buff")) {
            arrow.setMetadata("Is_Piercing_Arrow", new FixedMetadataValue(plugin, true));
            arrow.setMetadata("Pierce_Hit_Count", new FixedMetadataValue(plugin, 0));
            arrow.setMetadata("Pierced_Entities", new FixedMetadataValue(plugin, new java.util.ArrayList<java.util.UUID>()));
        }

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);

        // [HIỆU ỨNG MỚI]: Bắn ra hoa anh đào rơi lả tả
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, eyeLoc, 40, 2.0, 2.0, 2.0, 0.05);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    Location hitLoc = arrow.getLocation();
                    createCrystalBloom(player, hitLoc, dmgMulti);
                    this.cancel();
                    return;
                }
                Particle.DustOptions lightPinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 182, 193), 1.5f);
                arrow.getWorld().spawnParticle(Particle.DUST, arrow.getLocation(), 5, 0.1, 0.1, 0.1, 0, lightPinkDust);
                arrow.getWorld().spawnParticle(Particle.END_ROD, arrow.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // TẠO HOA PHA LÊ HÌNH CHÓP (CRYSTAL PYRAMIDS)
    // ==========================================
    private void createCrystalBloom(Player player, Location loc, double dmgMulti) {
        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 0.5f);

        Location centerBase = loc.clone().subtract(0, 0.5, 0);
        Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 182, 193), 1.2f);
        Particle.DustOptions lightMagentaDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 153, 255), 1.0f);
        Particle.DustOptions whitePinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 228, 225), 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 15) { this.cancel(); return; }

                // MỖI TICK VẼ 2 KHỐI CHÓP PHA LÊ (PYRAMIDS)
                for (int i = 0; i < 2; i++) {
                    // 1. Chọn 1 điểm gốc ngẫu nhiên dưới mặt đất (Bán kính 3 block)
                    double rBase = Math.random() * 3.0;
                    double tBase = Math.random() * 2 * Math.PI;
                    Location baseCenter = centerBase.clone().add(rBase * Math.cos(tBase), 0, rBase * Math.sin(tBase));

                    // 2. Chọn 1 đỉnh (Apex) ngẫu nhiên trên không trung (Cao 1.5 đến 4 block)
                    double apexHeight = 1.5 + Math.random() * 2.5;
                    // Đỉnh hơi lệch khỏi gốc để khối pha lê đâm xéo xéo tự nhiên
                    Location apex = baseCenter.clone().add((Math.random() - 0.5) * 1.5, apexHeight, (Math.random() - 0.5) * 1.5);

                    // 3. Vẽ khối chóp tứ giác (Square Pyramid)
                    int sides = 4;
                    double crystalRadius = 0.3 + Math.random() * 0.4; // Độ dày đáy pha lê
                    double angleOffset = Math.random() * Math.PI;

                    Particle.DustOptions faceColor = (Math.random() > 0.5) ? pinkDust : whitePinkDust;
                    Particle.DustOptions edgeColor = (Math.random() > 0.5) ? lightMagentaDust : pinkDust;

                    for (int j = 0; j < sides; j++) {
                        // Tính góc của điểm dưới đáy
                        double angle = angleOffset + j * (2 * Math.PI / sides);
                        Location baseCorner = baseCenter.clone().add(Math.cos(angle) * crystalRadius, 0, Math.sin(angle) * crystalRadius);

                        // Nối từ đáy lên đỉnh
                        drawParticleLine(baseCorner, apex, faceColor, loc.getWorld());

                        // Nối các điểm đáy với nhau để tạo hình khối
                        double nextAngle = angleOffset + ((j + 1) % sides) * (2 * Math.PI / sides);
                        Location nextCorner = baseCenter.clone().add(Math.cos(nextAngle) * crystalRadius, 0, Math.sin(nextAngle) * crystalRadius);
                        drawParticleLine(baseCorner, nextCorner, edgeColor, loc.getWorld());
                    }
                }

                if (ticks % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 15, 2.0, 2.0, 2.0, 0.05);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 10, 2.0, 2.0, 2.0, 0.1);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Logic Sát thương
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
            if (e instanceof LivingEntity && e != player && !e.hasMetadata("Summoner_Pet")) {
                LivingEntity target = (LivingEntity) e;
                boolean isCrossbow = player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW;
                double finalDamage = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 50.0, 0.0, 0.0, false, isCrossbow);

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, target, finalDamage);
                player.removeMetadata("Ghost_L_Blocker", plugin);
            }
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