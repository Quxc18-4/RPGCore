package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodPrisonSkill implements ISkill {

    private final JavaPlugin plugin;

    public BloodPrisonSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "reaper_blood_prison"; }
    @Override public String getName() { return "Huyết Ngục"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.CRIMSON_FUNGUS; }

    @Override public int getMaxLevel() { return 8; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 45.0 + (level * 3.0); // 48 -> 69 Thể lực
    }

    @Override
    public long getCooldown(int level) {
        double cd = 23.0 - (level * 1.0); // Cấp 1: 22.0s -> Cấp 8: 15.0s
        return (long) (Math.max(15.0, cd) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);
        int explosionMulti = 270 + (displayLvl * 50); // Cấp 1: 320% -> Cấp 8: 670%

        return List.of(
                "§7Phóng ra một quả cầu huyết năng cô đặc",
                "§7về phía trước (Tầm bay tối đa 18 block).",
                "",
                "§e[Thi Triển - Điểm Neo Huyết Ngục]",
                "§f- Khi va chạm mục tiêu hoặc địa hình, tạo một",
                "§f  §cĐiểm Neo§f tồn tại trong §c3.5 giây§f.",
                "§f- Phóng xích máu trói toàn bộ kẻ địch trong",
                "§f  vùng §e5x5 block§f, ngăn chặn thoát ra ngoài.",
                "",
                "§4[Kết Thúc - Huyết Ngục Bùng Nổ]",
                "§f- Khi điểm neo hết hiệu lực, phát nổ dữ dội,",
                "§f  gây §c" + explosionMulti + "% ST Vũ Khí§f lên kẻ địch",
                "§f  trong phạm vi 5x5."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                double timeLeft = (player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        World world = player.getWorld();
        if (world == null) return false;

        // Thanh toán tài nguyên & Cooldown chuẩn V2
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.4f);

        launchBloodOrb(player, rpgPlayer, level);
        return true;
    }

    // ==========================================
    // GIAI ĐOẠN 1: PHÓNG QUẢ CẦU MÁU (RAY MARCHING SUB-STEP)
    // ==========================================
    private void launchBloodOrb(Player player, RPGPlayer rpgPlayer, int level) {
        final Location startLoc = player.getEyeLocation();
        final World world = startLoc.getWorld();
        if (world == null) return;

        final Vector direction = startLoc.getDirection().normalize();
        final double speedPerTick = 1.2; // 1.2 block/tick * 15 ticks = 18 block tầm xa
        final double subStep = 0.3;      // Chia nhỏ bước quét chống xuyên địa hình mỏng

        new BukkitRunnable() {
            final Location currentLoc = startLoc.clone();
            int ticks = 0;
            final int maxTicks = 15;

            final Particle.DustOptions coreDust = new Particle.DustOptions(Color.fromRGB(160, 0, 20), 1.8f);
            final Particle.DustOptions auraDust = new Particle.DustOptions(Color.BLACK, 1.2f);

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !world.equals(player.getWorld())) {
                    this.cancel();
                    return;
                }

                for (double moved = 0.0; moved < speedPerTick; moved += subStep) {
                    double actualStep = Math.min(subStep, speedPerTick - moved);
                    Location prevLoc = currentLoc.clone();
                    currentLoc.add(direction.clone().multiply(actualStep));

                    // Vẽ lõi cầu máu và vệt khói đen liên tục
                    world.spawnParticle(Particle.DUST, currentLoc, 2, 0.12, 0.12, 0.12, 0, coreDust);
                    world.spawnParticle(Particle.DUST, currentLoc, 1, 0.22, 0.22, 0.22, 0, auraDust);

                    // 1. Va chạm địa hình (Block cứng)
                    if (!currentLoc.getBlock().isPassable()) {
                        spawnBloodAnchor(player, rpgPlayer, prevLoc, level);
                        this.cancel();
                        return;
                    }

                    // 2. Va chạm kẻ địch hợp lệ
                    for (Entity e : world.getNearbyEntities(currentLoc, 0.75, 0.75, 0.75)) {
                        if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                            spawnBloodAnchor(player, rpgPlayer, currentLoc, level);
                            this.cancel();
                            return;
                        }
                    }
                }

                // 3. Hết tầm bay tối đa -> Tự tạo điểm neo tại điểm cuối
                if (ticks >= maxTicks) {
                    spawnBloodAnchor(player, rpgPlayer, currentLoc, level);
                    this.cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // GIAI ĐOẠN 2: ĐIỂM NEO & TRÓI TRONG VÙNG 5x5
    // ==========================================
    private void spawnBloodAnchor(Player player, RPGPlayer rpgPlayer, Location impactLoc, int level) {
        final World world = impactLoc.getWorld();
        if (world == null) return;

        // Căn chỉnh điểm neo sát mặt đất
        final Location anchorLoc = impactLoc.clone();
        for (int i = 0; i < 6; i++) {
            if (anchorLoc.clone().subtract(0, 0.5, 0).getBlock().isPassable()) {
                anchorLoc.subtract(0, 0.5, 0);
            } else {
                break;
            }
        }

        final double halfSize = 2.5; // Bán kính 2.5 block => Vùng vuông 5x5 block
        final Set<UUID> trappedEntities = new HashSet<>();

        // Quét kẻ địch trong vùng 5x5 ngay thời điểm va chạm
        for (Entity e : world.getNearbyEntities(anchorLoc, halfSize, 2.5, halfSize)) {
            if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                trappedEntities.add(e.getUniqueId());
            }
        }

        world.playSound(anchorLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.2f, 0.6f);
        world.playSound(anchorLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);

        // [CÂN BẰNG]: Bù trừ trễ sát thương 3.5s -> 320% (Lv1) đến 670% (Lv8) ST Vũ Khí
        final double dmgMulti = 2.70 + (level * 0.50);

        new BukkitRunnable() {
            int ticks = 0;
            final int durationTicks = 70; // 3.5 giây tồn tại

            final Particle.DustOptions anchorDust = new Particle.DustOptions(Color.fromRGB(130, 0, 0), 2.2f);
            final Particle.DustOptions borderDust = new Particle.DustOptions(Color.fromRGB(200, 20, 40), 1.0f);
            final Particle.DustOptions chainDust = new Particle.DustOptions(Color.fromRGB(90, 0, 15), 1.1f);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // GIAI ĐOẠN 3: HẾT GIỜ -> KÍCH NỔ HUYẾT NGỤC
                if (ticks >= durationTicks) {
                    detonateAnchor(player, rpgPlayer, anchorLoc, halfSize, trappedEntities, dmgMulti);
                    this.cancel();
                    return;
                }

                // 1. Vẽ lõi Điểm Neo ở tâm vùng 5x5
                world.spawnParticle(Particle.DUST, anchorLoc.clone().add(0, 0.6, 0), 4, 0.15, 0.25, 0.15, 0, anchorDust);

                // 2. Vẽ đường viền khung vuông 5x5 trên mặt đất
                if (ticks % 4 == 0) {
                    drawSquareBoundary(anchorLoc, halfSize, borderDust);
                }

                // 3. Giữ chân kẻ địch bị trói bên trong vùng 5x5 & Vẽ xích máu
                Iterator<UUID> it = trappedEntities.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);

                    if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()
                            || !world.equals(target.getWorld())) {
                        it.remove();
                        continue;
                    }

                    Location targetLoc = target.getLocation();
                    double dx = targetLoc.getX() - anchorLoc.getX();
                    double dy = targetLoc.getY() - anchorLoc.getY();
                    double dz = targetLoc.getZ() - anchorLoc.getZ();

                    // Nếu bị dịch chuyển quá xa (trên 8 block) thì đứt xích
                    if (Math.abs(dx) > 8.0 || Math.abs(dz) > 8.0 || Math.abs(dy) > 8.0) {
                        it.remove();
                        continue;
                    }

                    // Giật ngược vào tâm mỗi 2 ticks nếu chạm rìa ngang (2.2) hoặc nhảy/bay quá cao (dy > 2.0)
                    if (ticks % 2 == 0) {
                        boolean outHorizontal = Math.abs(dx) > 2.2 || Math.abs(dz) > 2.2;
                        boolean outVertical = dy > 2.0;

                        if (outHorizontal || outVertical) {
                            Vector pullBack = anchorLoc.toVector().subtract(targetLoc.toVector());
                            if (!outVertical) {
                                pullBack.setY(0);
                            }
                            if (pullBack.lengthSquared() > 0.001) {
                                pullBack.normalize().multiply(0.65);
                                if (!outVertical) {
                                    pullBack.setY(Math.min(target.getVelocity().getY(), 0.05));
                                }
                                target.setVelocity(pullBack);
                            }
                        }
                    }

                    // Vẽ sợi xích máu nối từ Điểm Neo tới ngực mục tiêu
                    if (ticks % 2 == 0) {
                        drawBloodChain(anchorLoc.clone().add(0, 0.6, 0), targetLoc.clone().add(0, 1.0, 0), chainDust);
                    }
                }

                // Âm thanh nhịp đập cảnh báo sắp nổ ở 1 giây cuối
                if (ticks == 50 || ticks == 60) {
                    world.playSound(anchorLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void detonateAnchor(Player player, RPGPlayer rpgPlayer, Location anchorLoc, double halfSize, Set<UUID> trappedEntities, double dmgMulti) {
        World world = anchorLoc.getWorld();
        if (world == null) return;

        world.playSound(anchorLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        world.playSound(anchorLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.4f);

        world.spawnParticle(Particle.EXPLOSION_EMITTER, anchorLoc.clone().add(0, 1.0, 0), 1);
        world.spawnParticle(Particle.BLOCK_CRUMBLE, anchorLoc.clone().add(0, 1.0, 0), 60, 1.5, 0.8, 1.5, Material.REDSTONE_BLOCK.createBlockData());

        Set<UUID> victimsToHit = new HashSet<>(trappedEntities);

        // Quét thêm toàn bộ mục tiêu đang đứng trong vùng 5x5 tại thời điểm nổ
        for (Entity e : world.getNearbyEntities(anchorLoc, halfSize, 2.5, halfSize)) {
            if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                victimsToHit.add(e.getUniqueId());
            }
        }

        for (UUID uuid : victimsToHit) {
            Entity e = Bukkit.getEntity(uuid);
            if (e instanceof LivingEntity victim && victim.isValid() && !victim.isDead() && world.equals(victim.getWorld())) {
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, dmgMulti, 0.0, false, false);

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, victim, finalDmg);
                player.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }

    private void drawSquareBoundary(Location center, double halfSize, Particle.DustOptions dust) {
        World world = center.getWorld();
        if (world == null) return;

        double y = center.getY() + 0.15;
        for (double offset = -halfSize; offset <= halfSize; offset += 0.5) {
            world.spawnParticle(Particle.DUST, center.getX() + offset, y, center.getZ() - halfSize, 1, 0, 0, 0, 0, dust);
            world.spawnParticle(Particle.DUST, center.getX() + offset, y, center.getZ() + halfSize, 1, 0, 0, 0, 0, dust);
            world.spawnParticle(Particle.DUST, center.getX() - halfSize, y, center.getZ() + offset, 1, 0, 0, 0, 0, dust);
            world.spawnParticle(Particle.DUST, center.getX() + halfSize, y, center.getZ() + offset, 1, 0, 0, 0, 0, dust);
        }
    }

    private void drawBloodChain(Location from, Location to, Particle.DustOptions dust) {
        World world = from.getWorld();
        if (world == null) return;

        Vector diff = to.toVector().subtract(from.toVector());
        double distance = diff.length();
        if (distance <= 0.1) return;

        Vector step = diff.normalize().multiply(0.4);
        Location current = from.clone();

        for (double d = 0; d < distance; d += 0.4) {
            world.spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0, dust);
            current.add(step);
        }
    }
}