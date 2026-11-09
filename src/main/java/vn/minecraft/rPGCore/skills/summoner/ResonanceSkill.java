package vn.minecraft.rPGCore.skills.summoner;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile;
import vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResonanceSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, List<PetTrait>> playerStacks = new ConcurrentHashMap<>();

    private static class PetTrait {
        String archetype;
        String element;
        double petMaxHp;

        // Thêm tham số petMaxHp vào hàm khởi tạo
        public PetTrait(String archetype, String element, double petMaxHp) {
            this.archetype = archetype;
            this.element = element;
            this.petMaxHp = petMaxHp;
        }
    }

    public ResonanceSkill(JavaPlugin plugin) {
        this.plugin = plugin;

        // [CHUẨN SOLID]: Tự đăng ký hiệu ứng giảm sát thương của Summoner vào Core
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getIncomingDamageReduction(LivingEntity victim, LivingEntity attacker) {
                // Kiểm tra xem người chơi có đang trong trạng thái Cộng hưởng không
                if (victim.hasMetadata("Resonance_Stats")) {
                    Object val = victim.getMetadata("Resonance_Stats").get(0).value();
                    if (val instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Double> buffs = (Map<String, Double>) val;

                        // Trả về lượng giảm sát thương (VD: 0.32 cho 8 Golem)
                        // Con số này sẽ được CombatManager gộp lại và CAP ở 40%
                        return buffs.getOrDefault("DamageReduction", 0.0);
                    }
                }
                return 0.0;
            }

            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                // Nếu có buff tăng sát thương từ Resonance, cũng trả về ở đây để được Multiplicative
                if (attacker.hasMetadata("Resonance_Stats")) {
                    Object val = attacker.getMetadata("Resonance_Stats").get(0).value();
                    if (val instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Double> buffs = (Map<String, Double>) val;
                        return buffs.getOrDefault("DamageMultiplier", 0.0);
                    }
                }
                return 0.0;
            }
        });
    }

    @Override public String getId() { return "resonance"; }
    @Override public String getName() { return "Cộng hưởng Linh thú"; }
    @Override public String getRequiredClass() { return "summoner"; }
    @Override public Material getIcon() { return Material.END_CRYSTAL; }
    @Override public int getMaxLevel() { return 8; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 60.0 + (level * 10.0); }

    @Override public long getCooldown(int level) {
        long baseCd = 35000L;
        return Math.max(15000L, baseCd - (level * 1500L));
    }

    private int getMaxStacks(int level) {
        if (level >= 8) return 8;
        if (level >= 6) return 7;
        if (level >= 3) return 6;
        return 5;
    }

    private int getDuration(int level) {
        if (level >= 8) return 50;
        if (level >= 6) return 40;
        if (level >= 3) return 35;
        return 30;
    }

    private int getMaxAbsorbTargets(int level) {
        if (level >= 6) return 3;
        if (level >= 3) return 2;
        return 1;
    }

    @Override
    public List<String> getDescription(int level) {
        // Tính tốc độ bay (Khởi điểm 0.45 block/tick ở Lv1 -> Nhân 20 ra block/giây)
        double speedPerSec = (0.4 + (level * 0.05)) * 20.0;

        return Arrays.asList(
                "§7Hấp thụ Linh thú, hóa chúng thành",
                "§7Quả Cầu Linh Hồn bay về phía bạn.",
                "",
                "§fSố lượng nạp: §a" + getMaxAbsorbTargets(level) + " Linh thú/lần",
                "§fTốc độ bay: §b" + String.format("%.1f", speedPerSec) + " block/s",
                "§fThời gian duy trì: §6" + getDuration(level) + "s §f(Tối đa §e" + getMaxStacks(level) + " Stack§f)",
                "",
                "§c[!] Cảnh báo đánh chặn:",
                "§7Nếu quả cầu đập vào Tường hoặc Kẻ địch,",
                "§7nó sẽ vỡ vụn và tan biến vào hư vô!"
        );
    }

    private boolean isMyPet(Entity e, Player p) {
        if (!e.hasMetadata("Summoner_Pet")) return false;

        java.util.List<org.bukkit.metadata.MetadataValue> metaList = e.getMetadata("Summoner_Pet");
        if (metaList == null || metaList.isEmpty()) return false; // Chặn đứng lỗi "Index 0 out of bounds"

        return metaList.get(0).asString().equals(p.getUniqueId().toString());
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
        long finalCd = getCooldown(level);

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§c§l[!] §cKỹ năng đang hồi chiêu (§c" + String.format("%.1f", timeLeft) + "s§c)!");
            return false;
        }

        List<LivingEntity> targets = new ArrayList<>();
        int maxAbsorb = getMaxAbsorbTargets(level);

        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0,
                entity -> entity instanceof LivingEntity && isMyPet(entity, player)
        );

        if (result != null && result.getHitEntity() != null) {
            targets.add((LivingEntity) result.getHitEntity());
        }

        if (targets.size() < maxAbsorb) {
            List<LivingEntity> nearbyPets = new ArrayList<>();
            for (Entity e : player.getNearbyEntities(15, 10, 15)) {
                if (e instanceof LivingEntity && isMyPet(e, player) && !targets.contains(e)) {
                    nearbyPets.add((LivingEntity) e);
                }
            }
            nearbyPets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())));

            for (LivingEntity pet : nearbyPets) {
                targets.add(pet);
                if (targets.size() >= maxAbsorb) break;
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§c§l[!] §fKhông tìm thấy Linh Thú nào ở gần để Cộng hưởng!");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        // Thông báo khi kỹ năng hồi xong
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isValid()) {
                    player.sendMessage("§a§l[!] §fKỹ năng §dCộng hưởng Linh thú §fđã sẵn sàng!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, finalCd / 50L);

        player.sendMessage("§d§l[Cộng hưởng] §fTrích xuất §e" + targets.size() + "§f Linh Hồn!");

        // ==========================================
        // GIAI ĐOẠN 1: TIA RÚT HỒN BẮN VÀO THÚ (0.5 Giây)
        // ==========================================
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 10; // Thời gian tia bay (10 ticks = 0.5s)

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location pLoc = player.getLocation().add(0, 1.2, 0);

                // Vẽ tia ma thuật lao thẳng đến từng con thú
                for (LivingEntity pet : targets) {
                    if (pet.isDead() || !pet.isValid()) continue;

                    Location petLoc = pet.getLocation().add(0, 1.0, 0);
                    Vector dir = petLoc.toVector().subtract(pLoc.toVector());
                    double dist = dir.length();
                    dir.normalize();

                    // Vị trí đầu của tia Rút Hồn đang bay
                    double currentDist = (dist / maxTicks) * ticks;
                    Location point = pLoc.clone().add(dir.clone().multiply(currentDist));

                    // Đầu tia: Lửa linh hồn xanh (Soul Fire)
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 3, 0.1, 0.1, 0.1, 0.02);
                    player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);

                    // Đuôi tia: Vệt phép thuật tím (Spell Witch)
                    for (double d = Math.max(0, currentDist - 2.0); d < currentDist; d += 0.5) {
                        Location tail = pLoc.clone().add(dir.clone().multiply(d));
                        player.getWorld().spawnParticle(Particle.WITCH, tail, 1, 0, 0, 0, 0);
                    }
                }

                ticks++;

                // Khi tia đã chạm vào thú
                if (ticks >= maxTicks) {
                    // ==========================================
                    // GIAI ĐOẠN 2: THÚ NỔ TUNG THÀNH QUẢ CẦU
                    // ==========================================
                    for (LivingEntity targetPet : targets) {
                        if (targetPet.isDead() || !targetPet.isValid()) continue;

                        String petType = targetPet.hasMetadata("Pet_Type") ? targetPet.getMetadata("Pet_Type").get(0).asString() : "SpiritWolf";
                        IPetProfile profile = SummonPetManager.getProfile(petType);
                        if (profile == null) continue;

                        String pArchetype = profile.getArchetype();
                        String pElement = profile.getElement();

                        // Lấy máu tối đa của thú trước khi xóa nó và Ép thú nổ tung
                        double petHp = targetPet.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

                        Location spawnLoc = targetPet.getLocation().add(0, 1.0, 0);
                        targetPet.getWorld().spawnParticle(Particle.EXPLOSION, spawnLoc, 1);
                        targetPet.getWorld().playSound(spawnLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
                        targetPet.remove();

                        // Gọi hàm phóng Quả Cầu Linh Hồn bay ngược lại
                        spawnSoulOrb(player, spawnLoc, pArchetype, pElement, level, petHp);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==========================================
    // LOGIC CỦA QUẢ CẦU LINH HỒN - ĐƯỜNG CONG TỰ BẺ LÁI
    // ==========================================
    private void spawnSoulOrb(Player player, Location startLoc, String archetype, String element, int level, double petMaxHp) {
        new BukkitRunnable() {
            Location orbLoc = startLoc.clone();
            int ticksLived = 0;
            double speed = 0.4 + (level * 0.05); // Tốc độ rượt đuổi tối đa

            // Vận tốc khởi điểm (Bắn vọt lên trên và tỏa ra các hướng ngẫu nhiên)
            Vector velocity = new Vector(
                    (Math.random() - 0.5) * 0.6,
                    0.6, // Lực hất tung lên trời
                    (Math.random() - 0.5) * 0.6
            );

            @Override
            public void run() {
                // 1. Quá thời gian hoặc chủ nhân chết -> Tự hủy
                if (!player.isOnline() || player.isDead() || ticksLived > 200) {
                    orbLoc.getWorld().spawnParticle(Particle.WHITE_SMOKE, orbLoc, 10, 0.2, 0.2, 0.2, 0.05);
                    this.cancel();
                    return;
                }

                Location targetLoc = player.getLocation().add(0, 1.0, 0);
                Vector toTarget = targetLoc.toVector().subtract(orbLoc.toVector());
                double distance = toTarget.length();

                // 2. NẾU CHẠM VÀO NGƯỜI CHƠI (Hấp thụ thành công)
                if (distance <= 1.2) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.2);

                    List<PetTrait> myStacks = playerStacks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
                    synchronized (myStacks) {
                        myStacks.add(new PetTrait(archetype, element, petMaxHp));
                        while (myStacks.size() > getMaxStacks(level)) {
                            myStacks.remove(0);
                        }
                    }

                    recalculateAndApplyBuffs(player, myStacks, getDuration(level));
                    player.sendMessage("§a§l[+] §fHấp thụ thành công! (Stack: §a" + myStacks.size() + "/" + getMaxStacks(level) + "§f)");

                    this.cancel();
                    return;
                }

                // ==========================================
                // 3. THUẬT TOÁN ĐƯỜNG CONG (STEERING BEHAVIOR)
                // ==========================================
                // Lực hút về phía người chơi (Vector lý tưởng)
                Vector desiredVelocity = toTarget.normalize().multiply(speed);

                // Độ bẻ lái: Lúc đầu bẻ nhẹ (tạo đường cong lả lướt), càng về sau bẻ càng gắt để rượt kịp mục tiêu
                double steerForce = Math.min(0.08 + (ticksLived * 0.015), 1.0);

                // Trộn vector: Kéo vận tốc hiện tại hướng dần về phía vector lý tưởng
                velocity.add(desiredVelocity.subtract(velocity).multiply(steerForce));

                orbLoc.add(velocity);

                // ==========================================
                // 4. KIỂM TRA VA CHẠM (Sau 15 tick tàng hình để vượt qua đầu quái)
                // ==========================================
                if (ticksLived > 15) {
                    // Kiểm tra chạm tường
                    if (orbLoc.getBlock().getType().isSolid()) {
                        shatterOrb("§c§l[!] Quả cầu đập vào tường và tan vỡ!");
                        return;
                    }

                    // Kiểm tra chạm quái vật (Đánh chặn)
                    for (Entity e : orbLoc.getWorld().getNearbyEntities(orbLoc, 0.6, 0.6, 0.6)) {
                        if (e instanceof LivingEntity && !e.equals(player) && !(e instanceof ArmorStand) && !isMyPet(e, player)) {
                            shatterOrb("§c§l[!] Quả cầu bị chặn lại bởi " + e.getName() + "!");
                            e.getWorld().strikeLightningEffect(e.getLocation());
                            return;
                        }
                    }
                }

                // ==========================================
                // 5. VẼ VISUAL CHO QUẢ CẦU
                // ==========================================
                Particle.DustOptions coreDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 255), 2.0f);
                orbLoc.getWorld().spawnParticle(Particle.DUST, orbLoc, 5, 0.1, 0.1, 0.1, 0, coreDust);
                orbLoc.getWorld().spawnParticle(Particle.END_ROD, orbLoc, 2, 0.2, 0.2, 0.2, 0.01);

                if (ticksLived % 5 == 0) {
                    orbLoc.getWorld().playSound(orbLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                }

                ticksLived++;
            }

            // Hàm tạo hiệu ứng vỡ nát khi bị đánh chặn
            private void shatterOrb(String message) {
                orbLoc.getWorld().playSound(orbLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                orbLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, orbLoc, 30, 0.3, 0.3, 0.3, 0.2);
                player.sendMessage(message);
                this.cancel();
            }

        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void recalculateAndApplyBuffs(Player player, List<PetTrait> stacks, int durationSeconds) {
        Map<String, Double> finalBuffs = new HashMap<>();
        double totalEarthShield = 0.0;

        for (PetTrait trait : stacks) {
            switch (trait.archetype) {
                case "Attacker": finalBuffs.put("DamageMultiplier", finalBuffs.getOrDefault("DamageMultiplier", 0.0) + 0.10); break;
                case "Swift": finalBuffs.put("MoveSpeed", finalBuffs.getOrDefault("MoveSpeed", 0.0) + 0.12); break;
                case "Vanguard": finalBuffs.put("DamageReduction", finalBuffs.getOrDefault("DamageReduction", 0.0) + 0.04); break;
                case "Caster": finalBuffs.put("MagicPen", finalBuffs.getOrDefault("MagicPen", 0.0) + 0.05); break;
                case "Support": finalBuffs.put("RegenRate", finalBuffs.getOrDefault("RegenRate", 0.0) + 5.0); break;
            }

            switch (trait.element) {
                case "Spirit": finalBuffs.put("ArmorPen", finalBuffs.getOrDefault("ArmorPen", 0.0) + 0.05); break;
                case "Fire": finalBuffs.put("BurnChance", finalBuffs.getOrDefault("BurnChance", 0.0) + 0.15); break;
                case "Ice": finalBuffs.put("SlowChance", finalBuffs.getOrDefault("SlowChance", 0.0) + 0.20); break;
                case "Wind": finalBuffs.put("EffectResistance", finalBuffs.getOrDefault("EffectResistance", 0.0) + 10.0); break;
                case "Light": finalBuffs.put("HealOnHit", finalBuffs.getOrDefault("HealOnHit", 0.0) + 5.0); break;
                case "Earth":
                    totalEarthShield += (trait.petMaxHp * 0.15);
                    break;
            }
        }

        // ==========================================
        // [CẬP NHẬT 1]: ÁP DỤNG TRỰC TIẾP TỐC ĐỘ CHẠY (WALK SPEED)
        // ==========================================

        if (totalEarthShield > 0) {
            // [CHUẨN RPGCORE V2]: Cộng dồn giáp hiện tại và ủy quyền Vẽ UI cho RegenTask
            double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;
            player.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(plugin, currentShield + totalEarthShield));
            player.setMetadata("Hard_Shield_Active", new FixedMetadataValue(plugin, true)); // Bảo vệ khỏi decay
        }

        player.setMetadata("Resonance_Stats", new FixedMetadataValue(plugin, finalBuffs));

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer != null) {
            rpgPlayer.updateMoveSpeed(); // Hệ thống tự động đọc lại 'Tốc độ di chuyển' trong Map
        }

        // ==========================================
        // IN BẢNG DEBUG (Đã thêm Tốc chạy & Kháng)
        // ==========================================
        player.sendMessage(" ");
        player.sendMessage("§d§l[CỘNG HƯỞNG] §fCác trạng thái bạn vừa hấp thụ: ");
        if (totalEarthShield > 0) {
            player.sendMessage("§f- §aKhiên Ảo (Thổ): §e+" + String.format("%.1f", totalEarthShield) + " Máu");
        }
        if (finalBuffs.containsKey("DamageReduction")) {
            player.sendMessage("§f- §6Giảm Sát Thương (Tiên Phong): §c-" + (int)(finalBuffs.get("DamageReduction") * 100) + "%");
        }
        if (finalBuffs.containsKey("DamageMultiplier")) {
            player.sendMessage("§f- §cTăng Sát Thương (Chiến Đấu): §c+" + (int)(finalBuffs.get("DamageMultiplier") * 100) + "%");
        }
        if (finalBuffs.containsKey("MoveSpeed")) {
            player.sendMessage("§f- §bTăng Tốc Chạy (Nhanh Nhẹn): §b+" + (int)(finalBuffs.get("MoveSpeed") * 100) + "%");
        }
        if (finalBuffs.containsKey("EffectResistance")) {
            player.sendMessage("§f- §aKháng Hiệu Ứng (Gió): §a+" + finalBuffs.get("EffectResistance") + "%");
        }
        player.sendMessage(" ");

        if (player.hasMetadata("Resonance_Task")) {
            java.util.List<org.bukkit.metadata.MetadataValue> taskMeta = player.getMetadata("Resonance_Task");
            if (taskMeta != null && !taskMeta.isEmpty()) {
                Object val = taskMeta.get(0).value();
                if (val instanceof BukkitRunnable) {
                    ((BukkitRunnable) val).cancel();
                }
            }
        }

        BukkitRunnable durationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.removeMetadata("Resonance_Stats", plugin);
                    player.removeMetadata("RPG_Absorption_Shield", plugin);
                    player.removeMetadata("Hard_Shield_Active", plugin); // [MỚI]: Xóa thẻ bảo vệ giáp
                    playerStacks.remove(player.getUniqueId());

                    // [CẬP NHẬT 2]: Trả lại tốc độ gốc khi hết buff
                    RPGPlayer rp = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
                    if (rp != null) {
                        rp.updateMoveSpeed();
                    }

                    player.sendMessage("§7§o(Trạng thái Cộng hưởng đã tan biến...)");
                }
            }
        };
        durationTask.runTaskLater(plugin, durationSeconds * 20L);
        player.setMetadata("Resonance_Task", new FixedMetadataValue(plugin, durationTask));
    }
}