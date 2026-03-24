package vn.minecraft.rPGCore.skills.summoner;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
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
        public PetTrait(String archetype, String element) {
            this.archetype = archetype;
            this.element = element;
        }
    }

    public ResonanceSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "resonance"; }
    @Override public String getName() { return "Dung Hợp"; }
    @Override public String getRequiredClass() { return "summoner"; }
    @Override public Material getIcon() { return Material.END_CRYSTAL; }
    @Override public int getMaxLevel() { return 8; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    // [CÂN BẰNG]: Năng lượng giảm mạnh
    @Override public double getManaCost(int level) { return 60.0 + (level * 10.0); }

    // [CÂN BẰNG]: Giảm 1.5s hồi chiêu mỗi level
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
        if (level >= 8) return 60;
        if (level >= 6) return 50;
        if (level >= 3) return 40;
        return 30;
    }

    // [MỚI]: Hút nhiều mục tiêu cùng lúc
    private int getMaxAbsorbTargets(int level) {
        if (level >= 6) return 3;
        if (level >= 3) return 2;
        return 1;
    }

    // [MỚI]: Thời gian vận chiêu giảm theo Level
    private int getChannelTicks(int level) {
        // Base 2.2s (44 ticks). Giảm 0.1s (2 ticks) mỗi level. Tối thiểu 1.0s (20 ticks)
        return Math.max(20, 44 - ((level - 1) * 2));
    }

    @Override
    public List<String> getDescription(int level) {
        int ticks = getChannelTicks(level);
        return Arrays.asList(
                "§7Hấp thụ Linh thú để nhận chỉ số đặc biệt.",
                "",
                "§fMục tiêu tối đa: §a" + getMaxAbsorbTargets(level) + " con/lần",
                "§fThời gian vận: §e" + String.format("%.1f", ticks / 20.0) + "s",
                "§fDuy trì: §6" + getDuration(level) + "s §f(Max §e" + getMaxStacks(level) + " Stack§f)",
                "",
                "§c[!] Bị đánh ngắt chiêu:",
                "§7Hoàn 40% Mana & Giảm 50% Hồi chiêu.",
                "",
                "§fTiêu hao: §b" + getManaCost(level) + " Mana",
                "§fHồi chiêu: §e" + (getCooldown(level) / 1000.0) + "s"
        );
    }

    private boolean isMyPet(Entity e, Player p) {
        return e.hasMetadata("Summoner_Pet") && e.getMetadata("Summoner_Pet").get(0).asString().equals(p.getUniqueId().toString());
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
        long finalCd = getCooldown(level); // Hàm getFinalCooldown bị ẩn, dùng trực tiếp

        if (currentTime - lastUsed < finalCd) {
            player.sendMessage("§cKỹ năng đang hồi chiêu!");
            return false;
        }

        // 1. SMART TARGETING & MULTI-ABSORB
        List<LivingEntity> targets = new ArrayList<>();
        int maxAbsorb = getMaxAbsorbTargets(level);

        // Ưu tiên con đang nhìn (RayTrace)
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0,
                entity -> entity instanceof LivingEntity && isMyPet(entity, player)
        );

        if (result != null && result.getHitEntity() != null) {
            targets.add((LivingEntity) result.getHitEntity());
        }

        // Tự động dò tìm các con khác ở gần (Bù vào số lượng còn thiếu)
        if (targets.size() < maxAbsorb) {
            List<LivingEntity> nearbyPets = new ArrayList<>();
            for (Entity e : player.getNearbyEntities(15, 10, 15)) {
                if (e instanceof LivingEntity && isMyPet(e, player) && !targets.contains(e)) {
                    nearbyPets.add((LivingEntity) e);
                }
            }
            // Sắp xếp ưu tiên con gần nhất
            nearbyPets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())));

            for (LivingEntity pet : nearbyPets) {
                targets.add(pet);
                if (targets.size() >= maxAbsorb) break;
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§c§l[!] §fKhông tìm thấy Linh Thú nào ở gần để dung hợp!");
            return false;
        }

        // 2. BẮT ĐẦU VẬN CHIÊU
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        // [ISKILL]: Thiết lập thông báo khi hồi chiêu xong
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isValid()) {
                    player.sendMessage("§a§l[!] §fKỹ năng §dDung Hợp §fđã sẵn sàng!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, finalCd / 50L);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 2.0f);
        player.sendMessage("§d§l[Dung Hợp] §fĐang hấp thụ §e" + targets.size() + "§f Linh Thú...");

        int targetTicks = getChannelTicks(level);

        new BukkitRunnable() {
            int ticks = 0;
            double lastHealth = player.getHealth();

            @Override
            public void run() {
                targets.removeIf(p -> p.isDead() || !p.isValid());

                // Nếu người chơi chết hoặc toàn bộ thú đang hút bị chết -> Hủy
                if (!player.isOnline() || player.isDead() || targets.isEmpty()) {
                    this.cancel();
                    return;
                }

                // NGẮT CHIÊU
                if (player.getHealth() < lastHealth) {
                    player.sendMessage("§c§l[!] Bị ngắt chiêu do nhận sát thương!");
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.5f);

                    rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + (cost * 0.4));
                    cooldowns.put(player.getUniqueId(), currentTime - (finalCd / 2));

                    this.cancel();
                    return;
                }
                lastHealth = player.getHealth();

                // [VISUAL UPDATE]: Hiệu ứng Dòng chảy năng lượng (Absorb)
                Location pLoc = player.getLocation().add(0, 1.2, 0);

                // Thuật toán "Băng chuyền": Đẩy hạt di chuyển về phía người chơi
                double spacing = 1.5; // Khoảng cách giữa các hạt (Giãn ra để chống lag)
                double speed = 0.6;   // Tốc độ bay (Khuyến nghị 0.6 block/tick)
                double phase = (ticks * speed) % spacing; // Dịch chuyển vị trí theo thời gian

                for (LivingEntity pet : targets) {
                    Location petLoc = pet.getLocation().add(0, 1.0, 0);
                    Vector dir = pLoc.toVector().subtract(petLoc.toVector());
                    double dist = dir.length();
                    dir.normalize();

                    // Hạt bắt đầu từ Thú (phase) và trượt dần đến Người (dist)
                    for (double d = phase; d <= dist; d += spacing) {
                        Location p = petLoc.clone().add(dir.clone().multiply(d));

                        // Particle.GLOW: Hạt siêu nhỏ, phát sáng nhẹ, cực mượt và không gây khói (Rất giống đom đóm)
                        player.getWorld().spawnParticle(Particle.GLOW, p, 1, 0, 0, 0, 0);

                        // Có thể điểm xuyết thêm hạt End Rod (nhỏ, màu trắng sáng) để tia nhìn sắc nét hơn
                        player.getWorld().spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
                    }
                }

                ticks++;

                // HOÀN THÀNH
                if (ticks >= targetTicks) {
                    List<PetTrait> myStacks = playerStacks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

                    for (LivingEntity targetPet : targets) {
                        String petType = targetPet.hasMetadata("Pet_Type") ? targetPet.getMetadata("Pet_Type").get(0).asString() : "SpiritWolf";
                        IPetProfile profile = SummonPetManager.getProfile(petType);

                        if (profile != null) {
                            myStacks.add(new PetTrait(profile.getArchetype(), profile.getElement()));
                        }

                        targetPet.getWorld().spawnParticle(Particle.EXPLOSION, targetPet.getLocation(), 1);
                        targetPet.getWorld().playSound(targetPet.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                        targetPet.remove();
                    }

                    // Giữ số Stack không vượt quá Max
                    while (myStacks.size() > getMaxStacks(level)) {
                        myStacks.remove(0);
                    }

                    recalculateAndApplyBuffs(player, myStacks, getDuration(level));
                    player.sendMessage("§a§l[Thành công] §fNhận thêm §e" + targets.size() + "§f Stack! (Tổng: §a" + myStacks.size() + "/" + getMaxStacks(level) + "§f)");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void recalculateAndApplyBuffs(Player player, List<PetTrait> stacks, int durationSeconds) {
        Map<String, Double> finalBuffs = new HashMap<>();

        for (PetTrait trait : stacks) {
            switch (trait.archetype) {
                case "Attacker": finalBuffs.put("DamageMultiplier", finalBuffs.getOrDefault("DamageMultiplier", 0.0) + 0.10); break;
                case "Swift":
                    finalBuffs.put("MoveSpeed", finalBuffs.getOrDefault("MoveSpeed", 0.0) + 0.12);
                    finalBuffs.put("AttackSpeed", finalBuffs.getOrDefault("AttackSpeed", 0.0) + 0.05);
                    break;
                case "Defender": finalBuffs.put("DamageReduction", finalBuffs.getOrDefault("DamageReduction", 0.0) + 0.08); break;
                case "Caster": finalBuffs.put("CooldownReduction", finalBuffs.getOrDefault("CooldownReduction", 0.0) + 0.05); break;
                case "Vanguard": finalBuffs.put("Lifesteal", finalBuffs.getOrDefault("Lifesteal", 0.0) + 0.05); break;
            }

            switch (trait.element) {
                case "Spirit": finalBuffs.put("ArmorPen", finalBuffs.getOrDefault("ArmorPen", 0.0) + 0.05); break;
                case "Fire": finalBuffs.put("BurnChance", finalBuffs.getOrDefault("BurnChance", 0.0) + 0.15); break;
                case "Ice": finalBuffs.put("SlowChance", finalBuffs.getOrDefault("SlowChance", 0.0) + 0.20); break;
                case "Light": finalBuffs.put("HealOnHit", finalBuffs.getOrDefault("HealOnHit", 0.0) + 5.0); break;
                case "Earth":
                    double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    player.setAbsorptionAmount(player.getAbsorptionAmount() + (maxHp * 0.15));
                    break;
            }
        }

        player.setMetadata("Resonance_Stats", new FixedMetadataValue(plugin, finalBuffs));

        if (player.hasMetadata("Resonance_Task")) {
            BukkitRunnable oldTask = (BukkitRunnable) player.getMetadata("Resonance_Task").get(0).value();
            oldTask.cancel();
        }

        BukkitRunnable durationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.removeMetadata("Resonance_Stats", plugin);
                    playerStacks.remove(player.getUniqueId());
                    player.sendMessage("§7§o(Trạng thái Dung Hợp đã phai nhạt...)");
                }
            }
        };
        durationTask.runTaskLater(plugin, durationSeconds * 20L);
        player.setMetadata("Resonance_Task", new FixedMetadataValue(plugin, durationTask));
    }
}