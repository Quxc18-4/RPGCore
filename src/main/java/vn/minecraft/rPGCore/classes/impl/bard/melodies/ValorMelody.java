package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneModifier;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class ValorMelody implements IMelody {

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, Location targetLocation) {

        // 1. ÁP DỤNG HỆ SỐ TỪ TONE MODIFIER
        double radius = 5.0 * ToneModifier.getEchoRadiusMultiplier(player, rpgPlayer);
        double durationTicks = (8.0 * 20.0) * ToneModifier.getLowDurationMultiplier(player);
        int sharpCount = ToneModifier.getSharpToneCount(player);

        // Âm Cao (+25% Hệ số Buff). Gốc của Dũng Cảm là +10% DMG.
        // CombatManager sẽ đọc cờ Meta cường hóa riêng nếu có Âm Cao (Ví dụ: +12.5% thay vì +10%).
        double highMultiplier = ToneModifier.getHighToneMultiplier(player);

        // 2. HIỆU ỨNG THỊ GIÁC VÀ ÂM THANH (VISUALS & AUDIO)
        player.getWorld().playSound(targetLocation, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.2f);
        player.getWorld().playSound(targetLocation, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);

        // Vẽ vòng tròn Hào quang dưới đất
        drawAuraRing(targetLocation, radius);

        // 3. QUÉT THỰC THỂ VÀ BUFF (THREAD-SAFE & TARGET FILTERING)
        int allyCount = 0;

        for (Entity e : targetLocation.getWorld().getNearbyEntities(targetLocation, radius, radius, radius)) {
            if (e instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) e;

                // Lọc Đồng Minh: Là chính bản thân, HOẶC là người chơi/pet không thể bị tấn công
                boolean isAlly = target.equals(player) || (target instanceof Player && !CombatManager.canTarget(player, target));

                if (isAlly) {
                    allyCount++;
                    long expireTime = System.currentTimeMillis() + (long) (durationTicks * 50); // 50ms mỗi tick

                    // Gắn Buff Dũng Cảm (BardClass Hook sẽ đọc Meta này)
                    // Nếu muốn scale % theo Âm Cao, ta có thể nhét highMultiplier vào value thay vì dùng expireTime
                    // Tuy nhiên để tối giản, ta gán cờ expireTime và sẽ chỉnh CombatManager đọc mảng Object nếu cần scale
                    target.setMetadata(BardClass.META_VALOR_BUFF, new FixedMetadataValue(RPGCore.getInstance(), expireTime));

                    // Xử lý Âm Sắc (Sharp Tone): Nếu có, cấp thêm Kháng Hiệu ứng
                    if (sharpCount > 0) {
                        target.setMetadata("BARD_SHARP_BUFF", new FixedMetadataValue(RPGCore.getInstance(), expireTime));
                    }

                    // Hiệu ứng nhận Buff cho từng đồng minh (Sử dụng Hạt chuẩn 1.21)
                    target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 2, 0), 3, 0.2, 0.2, 0.2, 0);
                }
            }
        }

        // 4. KIỂM TRA LÕI NÂNG CẤP 9: GIAI ĐIỆU VĨNH CỬU
        if (allyCount >= 4 && rpgPlayer.hasSetBonus("BARD_CORE_9", 1)) {
            player.setMetadata("BARD_ETERNAL_MELODY_TRIGGERED", new FixedMetadataValue(RPGCore.getInstance(), true));
            player.sendMessage("§d§l[Giai Điệu Vĩnh Cửu] §fCộng hưởng với " + allyCount + " đồng minh! Giảm mạnh Hồi chiêu!");
        }
    }

    /**
     * Vòng lặp vẽ hạt Particle cho Hào quang
     * Cập nhật chuẩn Spigot 1.21.x: Sử dụng DUST_COLOR_TRANSITION
     */
    private void drawAuraRing(Location center, double radius) {
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step > 1) { // Vẽ trong 2 ticks để tạo độ mượt
                    this.cancel();
                    return;
                }
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                    double x = Math.cos(i) * radius;
                    double z = Math.sin(i) * radius;
                    Location loc = center.clone().add(x, 0.1, z); // Anti-Ground: Dâng lên 0.1 block

                    // Khởi tạo hiệu ứng Dust có khả năng chuyển màu (Từ Vàng sang Cam)
                    Particle.DustTransition dustTransition = new Particle.DustTransition(Color.YELLOW, Color.ORANGE, 0.8f);

                    // Gọi hàm spawnParticle theo cú pháp mới
                    center.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0, dustTransition);
                }
                step++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}