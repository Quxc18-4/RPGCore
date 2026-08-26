package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.warrior.WarriorAuraSkill;
import org.bukkit.Location;
import vn.minecraft.rPGCore.skills.warrior.WarriorSunderArmor;

import java.util.Map;

public class WarriorClass extends RPGClass {

    public WarriorClass(String id, String displayName, Material icon, java.util.List<String> description, double maxHealth, double maxResource, ResourceType resourceType, double regenRate, boolean enchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, enchanted);
        this.lifeStealCap = 80.0;
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        stats.put("Tỉ lệ hút máu", stats.getOrDefault("Tỉ lệ hút máu", 0.0) + 12.0);

        // ==========================================
        // [NỘI TẠI MỚI]: LƯỠI GƯƠM NHUỘM MÁU
        // ==========================================
        int bloodBladeLevel = rpgPlayer.getSkillData().getSkillLevel("warrior_bloodbathed_blade");
        if (bloodBladeLevel > 0) {
            double passiveLifesteal = bloodBladeLevel * 2.0; // Mỗi cấp +2%
            stats.put("Tỉ lệ hút máu", stats.getOrDefault("Tỉ lệ hút máu", 0.0) + passiveLifesteal);
        }

        // ==========================================
        // [HUYẾT GIÁP BẠO PHÁ]: ÉP TỈ LỆ HÚT MÁU TRỰC TIẾP
        // Đẩy vào đây để hàm Cha bên dưới tự động chặn Overflow!
        // ==========================================
        Player player = rpgPlayer.getBukkitPlayer();
        if (player.hasMetadata("BloodShield_Active")) {
            int level = player.getMetadata("BloodShield_Active").get(0).asInt();
            double extraLifesteal = 12.0 + (level - 1) * 2.0; // Cấp 1: 12%, Cấp 10: 30%
            stats.put("Tỉ lệ hút máu", stats.getOrDefault("Tỉ lệ hút máu", 0.0) + extraLifesteal);
        }

        // [MỚI]: Cộng 8% Hút máu nếu đang có Giáp Ảo (RPG_Absorption_Shield)
        if (player.hasMetadata("RPG_Absorption_Shield")) {
            double currentShield = player.getMetadata("RPG_Absorption_Shield").get(0).asDouble();
            if (currentShield > 0) {
                stats.put("Tỉ lệ hút máu", stats.getOrDefault("Tỉ lệ hút máu", 0.0) + 8.0);
            }
        }

        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // ==========================================
    // GỌI KỸ NĂNG PARRY THÔNG QUA PHÍM F
    // ==========================================
    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // [ĐÃ SỬA]: Thủ thế là bản năng, ép nó thành cấp 1 kể cả khi chưa cộng điểm
        int skillLevel = Math.max(1, rpgPlayer.getSkillData().getSkillLevel("warrior_parry"));

        ISkill skill = RPGCore.getInstance().getSkillManager().getSkill("warrior_parry");
        if (skill != null) {
            skill.execute(player, rpgPlayer, skillLevel);
        }

        // LUÔN LUÔN trả về TRUE để chặn vứt vũ khí sang tay trái!
        return true;
    }

    // ==========================================
    // NHẬN SÁT THƯƠNG -> CHECK PARRY -> TĂNG NỘ -> VFX/SFX HOÀNH TRÁNG
    // ==========================================
    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, LivingEntity attacker, double incomingDamage) {
        // Gọi cha để nó trừ Giáp trước, xong xuôi mới mang xuống tính Parry
        double finalDamage = super.onTakeDamage(victim, rpgVictim, attacker, incomingDamage);

        if (victim.hasMetadata("Warrior_Parry_Window")) {
            victim.removeMetadata("Warrior_Parry_Window", RPGCore.getInstance());
            finalDamage *= 0.5; // Giảm 50% DMG

            Location loc = victim.getLocation().add(0, 1.2, 0); // Vị trí ngang ngực để spawn hiệu ứng

            // ==========================================
            // SFX: DÀN ÂM THANH "KENG KENG" BINH KHÍ VA CHẠM (image_0.png)
            // ==========================================
            // Tiếng Block cơ bản: Đã sửa lại đúng tên Enum cho Spigot 1.21
            victim.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.8f);

            // Dàn KENGS KENGS 1: Tiếng Chuông vang dội
            victim.getWorld().playSound(loc, Sound.BLOCK_BELL_USE, 1.2f, 2.0f); // Pitch cao tạo tiếng vang metallic

            // Dàn KENGS KENGS 2: Tiếng Sắt đập vào Cửa Sắt
            victim.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f); // Pitch cao tạo cảm giác va chạm mạnh

            // Dàn KENGS KENGS 3: Tiếng Tảng Đá đập vào Đe Sắt (Tạo tiếng 'Thud' nặng nề của va chạm)
            victim.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f); // Pitch thấp

            // ==========================================
            // VFX: PARTICLE "HỎA TINH BÙNG NỔ" (image_0.png)
            // ==========================================
            // 1. Tia lửa bùng nổ radiating outwards (Particle.FIREWORK_SPARKS)
            victim.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.4, 0.4, 0.4, 0.1);

            // 2. Tia lửa va chạm (Particle.DAMAGE_INDICATOR)
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 15, 0.2, 0.2, 0.2, 0.2);

            // 3. Hiệu ứng Flash năng lượng xanh (Dùng Particle.CRIT_MAGIC y như trong ảnh, bám theo Color check cũ của 1.21 nếu có)
            victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 25, 0.6, 0.6, 0.6, 0.2);

            // 4. Bùng nổ nhỏ (Particle.EXPLOSION)
            victim.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);

            // Gửi Action Bar thông báo cho sướng
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent("§a§l[PARRY THÀNH CÔNG!] §c-50% CD"));

            // [NỘ KHÍ]: Hồi 15 Nộ khi Parry thành công
            rpgVictim.setCurrentResource(rpgVictim.getCurrentResource() + 15);

            // [NỘ KHÍ]: Bị đánh trúng khi đang Parry vẫn hồi 1 Nộ
            rpgVictim.setCurrentResource(rpgVictim.getCurrentResource() + 1);

            // [COOLDOWN]: Giảm 50% thời gian hồi chiêu hiện tại
            if (victim.hasMetadata("Warrior_Parry_CD_Time")) {
                long currentCdEnd = victim.getMetadata("Warrior_Parry_CD_Time").get(0).asLong();
                long now = System.currentTimeMillis();
                long remaining = currentCdEnd - now;
                if (remaining > 0) {
                    long newCdEnd = now + (remaining / 2);
                    victim.setMetadata("Warrior_Parry_CD_Time", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), newCdEnd));
                }
            }
        } else {
            // [NỘ KHÍ]: Bị đánh bình thường không hồi Nộ
        }
        return finalDamage;
    }

    // ==========================================
    // CHÉM TRÚNG -> TĂNG NỘ -> GỌI BỘ ĐẾM KIẾM KHÍ
    // ==========================================
    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        super.onAttackLanded(attacker, target);

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
        if (rpgPlayer != null) {
            // [YÊU CẦU MỚI]: Chém trúng địch hồi 3 Nộ
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + 2);

            int skillLevel = rpgPlayer.getSkillData().getSkillLevel("warrior_aura");
            if (skillLevel > 0) {
                ISkill skill = RPGCore.getInstance().getSkillManager().getSkill("warrior_aura");
                if (skill instanceof WarriorAuraSkill) {
                    ((WarriorAuraSkill) skill).addStack(attacker, rpgPlayer, target, skillLevel);
                }
            }
            // ==========================================
            // 2. [MỚI]: TÍCH ĐIỂM TOÁI GIÁP (SUNDER ARMOR)
            // ==========================================
            int sunderLevel = rpgPlayer.getSkillData().getSkillLevel("warrior_sunder_armor");
            if (sunderLevel > 0) {
                ISkill skill = RPGCore.getInstance().getSkillManager().getSkill("warrior_sunder_armor");
                if (skill instanceof WarriorSunderArmor) {
                    ((WarriorSunderArmor) skill).processAttackLanded(attacker, target, sunderLevel);
                }
            }
        }
    }

    // ==========================================
    // CAN THIỆP SÁT THƯƠNG ĐÁNH TAY (MELEE HIT HOOK)
    // Xả Buff của các kỹ năng Cường hóa vũ khí tại đây
    // ==========================================
    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, double rawDamage) {

        // ==========================================
        // [KIẾM ĐIỆN STATIKK] - MẢNH SẤM SÉT (TỐI ĐA 8 STACK)
        // ==========================================
        if (attacker.hasMetadata("Warrior_Judgment_Stacks")) {
            int stacks = attacker.getMetadata("Warrior_Judgment_Stacks").get(0).asInt();

            if (stacks <= 1) {
                attacker.removeMetadata("Warrior_Judgment_Stacks", RPGCore.getInstance());
            } else {
                attacker.setMetadata("Warrior_Judgment_Stacks", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), stacks - 1));
            }

            // Sát thương 145% AOE
            double statikkDmg = vn.minecraft.rPGCore.managers.CombatManager.calculateDamage(attacker, rpgAttacker, target, 1.45, 0.0, false, false);

            java.util.List<LivingEntity> chainTargets = new java.util.ArrayList<>();
            chainTargets.add(target);
            for (org.bukkit.entity.Entity e : target.getNearbyEntities(6, 6, 6)) {
                if (chainTargets.size() >= 3) break; // Lan tối đa 3 mục tiêu
                if (e instanceof LivingEntity && e != target && vn.minecraft.rPGCore.managers.CombatManager.canTarget(attacker, e)) {
                    chainTargets.add((LivingEntity) e);
                }
            }

            attacker.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

            // Lấy điểm bắt đầu từ tay của người chơi
            org.bukkit.Location prevLoc = attacker.getLocation().clone().add(0, 1.2, 0);

            for (LivingEntity chainTarget : chainTargets) {
                // 1. Gây sát thương
                vn.minecraft.rPGCore.managers.CombatManager.applySkillDamage(attacker, chainTarget, statikkDmg);

                // [ĐÃ XÓA]: chainTarget.getWorld().strikeLightningEffect(...)

                // Âm thanh điện giật trúng mục tiêu (thay thế cho tiếng sét Vanilla ồn ào)
                chainTarget.getWorld().playSound(chainTarget.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);

                // 2. Thuật toán vẽ tia chớp điện Zíc-Zắc (Chain Lightning)
                org.bukkit.Location currentLoc = chainTarget.getLocation().clone().add(0, 1.0, 0);
                org.bukkit.util.Vector dir = currentLoc.toVector().subtract(prevLoc.toVector());
                double length = dir.length();

                if (length > 0) {
                    dir.normalize();
                    // Dày hạt hơn (0.3 thay vì 0.5)
                    for (double i = 0; i < length; i += 0.3) {
                        org.bukkit.Location lineLoc = prevLoc.clone().add(dir.clone().multiply(i));

                        // Thêm độ nhiễu ngẫu nhiên để tia điện giật zíc-zắc thay vì đường thẳng
                        double offsetX = (Math.random() - 0.5) * 0.5;
                        double offsetY = (Math.random() - 0.5) * 0.5;
                        double offsetZ = (Math.random() - 0.5) * 0.5;
                        lineLoc.add(offsetX, offsetY, offsetZ);

                        // Vẽ tia lửa điện và lõi trắng
                        chainTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, lineLoc, 3, 0.05, 0.05, 0.05, 0);
                        chainTarget.getWorld().spawnParticle(Particle.END_ROD, lineLoc, 1, 0, 0, 0, 0);
                    }
                }
                prevLoc = currentLoc;
            }
            attacker.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            // Âm thanh chém sấm sét tổng (Giữ lại để có lực)
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);

            return 0.0; // Triệt tiêu đòn đánh tay
        }

        // 1. KIỂM TRA XEM CÓ BUFF "ĐÁNH BẬT" KHÔNG
        if (attacker.hasMetadata("Warrior_Knockup_Buff")) {
            String buffData = attacker.getMetadata("Warrior_Knockup_Buff").get(0).asString();

            // Xóa Buff ngay lập tức để không bị lạm dụng
            attacker.removeMetadata("Warrior_Knockup_Buff", RPGCore.getInstance());

            String[] parts = buffData.split("_");
            int level = Integer.parseInt(parts[0]);
            boolean isEmpowered = Boolean.parseBoolean(parts[1]);

            double dmgMulti = isEmpowered ? (2.2 + (level - 1) * 0.25) : (0.4 + (level - 1) * 0.1);

            // ==========================================
            // VFX 1: ĐƯỜNG KIẾM CRIT HẤT CHÉO 30 ĐỘ LÊN TRÊN
            // ==========================================
            org.bukkit.Location startLoc = attacker.getLocation().clone().add(0, 0.2, 0);
            org.bukkit.util.Vector forward = startLoc.getDirection().setY(0).normalize();

            for (int i = 0; i <= 20; i++) {
                double t = i / 20.0;
                double distance = t * 3.5; // Chém vút tới trước 3.5 block
                double height = Math.sin(t * Math.PI / 2) * 1.8; // Tạo độ cong mềm mại hất lên 1.8 block

                org.bukkit.Location pLoc = startLoc.clone()
                        .add(forward.clone().multiply(distance))
                        .add(0, height, 0);

                attacker.getWorld().spawnParticle(Particle.CRIT, pLoc, 2, 0.1, 0.1, 0.1, 0.05);
            }

            // ==========================================
            // 2. XỬ LÝ VẬT LÝ & HIỆU ỨNG (PHYSICS) - MỨC ĐỘ TÀN PHÁ CỰC HẠN
            // ==========================================
            if (isEmpowered) {
                // [HẤT VĂNG CHÍN TẦNG MÂY]
                org.bukkit.util.Vector trajectory = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();

                // Lực ngang x2.5, Hất tung Y = 3.0 (Tương đương bay bổng lên cỡ 40-50 blocks!)
                trajectory.multiply(2.5).setY(3.0);
                target.setVelocity(trajectory);

                attacker.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f); // Chỉnh pitch thấp xuống để nghe "nặng" hơn

                // Chờ mục tiêu chạm đất để kích hoạt Chấn động
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isOnGround() || target.isDead()) {
                            // VFX: MỤC TIÊU CHẠM ĐẤT -> NỔ + KHÓI MÙ MỊCH
                            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 4, 1.0, 0.2, 1.0, 0);
                            target.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, target.getLocation(), 60, 3.0, 1.0, 3.0, 0.1);
                            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 50, 2.0, 0.5, 2.0, 0.2);

                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

                            for (org.bukkit.entity.Entity nearby : target.getNearbyEntities(2.5, 2.5, 2.5)) { // Tăng nhẹ tầm nổ
                                if (nearby instanceof LivingEntity && vn.minecraft.rPGCore.managers.CombatManager.canTarget(attacker, nearby)) {
                                    vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect((LivingEntity) nearby, PotionEffectType.SLOWNESS, 60, 10);
                                }
                            }
                            this.cancel();
                        }
                    }
                }.runTaskTimer(RPGCore.getInstance(), 10L, 2L); // Bắt đầu check sau nửa giây vì bay quá cao

            } else {
                // [ĐẨY LÙI NHƯ XE TẢI TÔNG]
                org.bukkit.util.Vector push = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();

                // Nhấc bổng bứt mặt đất (Y=0.8) và đẩy lùi với vận tốc ngang khổng lồ (multiply=6.0)
                // Cú đánh này sẽ ném quái vật bay xa khoảng 15-20 blocks trong chớp mắt
                push.multiply(6.0).setY(0.8);

                target.setVelocity(push);

                attacker.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.8f); // Đổi tiếng đập cửa nghe cho bạo lực
            }

            // 1. TÍNH TOÁN VÀ GÂY SÁT THƯƠNG TRƯỚC (Để Vanilla Knockback xảy ra trước)
            double finalDmg = vn.minecraft.rPGCore.managers.CombatManager.calculateDamage(attacker, rpgAttacker, target, dmgMulti, 0.0, false, false);

            attacker.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
            vn.minecraft.rPGCore.managers.CombatManager.applySkillDamage(attacker, target, finalDmg);
            attacker.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

            // ==========================================
            // 2. DELAY 1 TICK RỒI MỚI ÁP DỤNG VẬT LÝ
            // ==========================================
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    // Kiểm tra xem mục tiêu còn sống không sau khi ăn sát thương
                    if (target.isDead() || !target.isValid()) return;

                    if (isEmpowered) {
                        // [HẤT VĂNG CÓ NỘ: TẦM TRUNG - XA]
                        org.bukkit.util.Vector trajectory = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();

                        // Ngang: 2.25 | Dọc: 1.54 (-30% cao, +50% xa)
                        trajectory.multiply(2.25).setY(1.54);
                        target.setVelocity(trajectory);

                        attacker.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);

                        // Task phụ: Chờ mục tiêu chạm đất để nổ
                        new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                if (target.isOnGround() || target.isDead()) {
                                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 4, 1.0, 0.2, 1.0, 0);
                                    target.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, target.getLocation(), 60, 3.0, 1.0, 3.0, 0.1);
                                    target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 50, 2.0, 0.5, 2.0, 0.2);

                                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

                                    for (org.bukkit.entity.Entity nearby : target.getNearbyEntities(2.5, 2.5, 2.5)) {
                                        if (nearby instanceof LivingEntity && vn.minecraft.rPGCore.managers.CombatManager.canTarget(attacker, nearby)) {
                                            vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect((LivingEntity) nearby, PotionEffectType.SLOWNESS, 60, 10);
                                        }
                                    }
                                    this.cancel();
                                }
                            }
                        }.runTaskTimer(RPGCore.getInstance(), 10L, 2L);

                    } else {
                        // [ĐẨY LÙI KHÔNG NỘ: TRƯỢT DÀI TRÊN ĐẤT]
                        org.bukkit.util.Vector push = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();

                        // Ngang: 5.25 | Dọc: 0.42 (-30% cao, +50% xa)
                        push.multiply(5.25).setY(0.42);
                        target.setVelocity(push);

                        attacker.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.8f);
                    }
                }
            }.runTaskLater(RPGCore.getInstance(), 1L); // <--- Vẫn giữ chìa khóa 1 Tick

            return 0.0; // Triệt tiêu sát thương tay gốc
        }

        // Nếu không có Buff, xử lý chém tay bình thường
        return super.onMeleeHit(attacker, rpgAttacker, target, rawDamage);
    }
}