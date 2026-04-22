package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElementalSwapSkill implements ISkill {

    // Khóa lưu trữ PDC
    public static final NamespacedKey ACTIVE_ELEMENT_KEY = new NamespacedKey(RPGCore.getInstance(), "Mage_Active_Element");
    public static final NamespacedKey ROTATION_KEY = new NamespacedKey(RPGCore.getInstance(), "Mage_Element_Rotation");

    @Override public String getId() { return "mage_elemental_swap"; }
    @Override public String getName() { return "§d§lChuyển Hóa Nguyên Tố"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.END_CRYSTAL; }

    @Override public int getMaxLevel() { return 1; }
    @Override public int getUpgradeCost(int currentLevel) { return 0; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public double getManaCost(int level) { return 0; }
    @Override public long getCooldown(int level) { return 1500L; } // 1.5s Cooldown

    @Override public boolean isPassiveSkill() { return true; } // Để hiển thị là Bật/Tắt UI

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Cơ chế đặc quyền của hệ phái Pháp Sư.",
                "§7Cho phép luân chuyển giữa các Nguyên tố.",
                "",
                "§8▪ §fPhím tắt: §e[F] §7(Đổi tay / Swap Item)",
                "§8▪ §fHồi chiêu: §a1.5s",
                "",
                "§e[Nội tại: Chuyển Hóa]",
                "§f- §cLửa: §f90% ST Vũ khí, 20% tỷ lệ Thiêu đốt.",
                "§f- §bNước: §f40% ST Vũ khí, xuyên thấu 5 mục tiêu.",
                "§f- §aGió: §f70% ST Vũ khí, đẩy lùi, 10% lốc hút quái.",
                "§f- §2Tự Nhiên: §f80% ST Vũ khí, 15% trói chân.",
                "§f- §5Bóng Tối: §f80% ST Vũ khí, 25% tỷ lệ Mù lòa.",
                "§f- §eÁnh Sáng: §fXuyên 3 mục tiêu, gây Phơi nhiễm.",
                "",
                "§d[Nội tại: Phản Ứng Thuật Pháp]",
                "§f- Kỹ năng trúng đích sẽ áp §nẤn Nguyên Tố§f lên quái.",
                "§f- Kết hợp nguyên tố thứ 2 để kích nổ Phản ứng:",
                "§f  §8• §7Cùng hệ: §bBùng Nổ Thuật Pháp §7(115% Sát thương).",
                "§f  §8• §cLửa §f+ §bNước: §cBốc Hơi §7(150% Sát thương).",
                "§f  §8• §eSáng §f+ §5Tối: §dHỗn Độn §7(150% Sát thương, Phá giáp).",
                "§f  §8• §cLửa §f+ §eSáng: §eChớp Giật §7(150% Sát thương, Tê liệt).",
                "§f  §8• §cLửa §f+ §5Tối: §5Hắc Hỏa §7(Rút máu theo % đã mất).",
                "§f  §8• §cLửa §f+ §2Thảo: §6Thiêu Rụi §7(Đốt cháy 7s).",
                "§f  §8• §bNước §f+ §2Thảo: §aSinh Sôi §7(Trói AoE, Hồi máu)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) return false;
        }

        List<String> unlocked = rpgPlayer.getUnlockedElements();
        if (unlocked.isEmpty()) {
            player.sendMessage("§cBạn chưa thức tỉnh bất kỳ Nguyên tố nào!");
            return false;
        }

        // 1. Lấy chuỗi xoay vòng (Rotation) do người chơi sắp xếp từ UI
        String rotationStr = player.getPersistentDataContainer().getOrDefault(ROTATION_KEY, PersistentDataType.STRING, String.join(",", unlocked));
        List<String> rotationList = Arrays.stream(rotationStr.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        if (rotationList.isEmpty()) {
            rotationList = unlocked; // Nếu UI xếp lỗi, fallback về list gốc
        }

        // 2. Lấy nguyên tố hiện tại
        String currentActive = player.getPersistentDataContainer().getOrDefault(ACTIVE_ELEMENT_KEY, PersistentDataType.STRING, rotationList.get(0));

        // 3. Tìm vị trí hiện tại và tiến lên 1 bước (Xoay vòng tròn)
        int currentIndex = rotationList.indexOf(currentActive);
        int nextIndex = (currentIndex + 1) % rotationList.size();
        String nextElementStr = rotationList.get(nextIndex);

        // 4. Lưu lại và áp dụng hiệu ứng
        player.getPersistentDataContainer().set(ACTIVE_ELEMENT_KEY, PersistentDataType.STRING, nextElementStr);
        MageElement elementEnum = MageElement.valueOf(nextElementStr);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);
        player.getWorld().spawnParticle(elementEnum.getParticle(), player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);

        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§f[Nguyên Tố]: " + elementEnum.getDisplayName()));

        // Lưu Cooldown 1.5s
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + finalCd));

        return true;
    }

    // Tiện ích lấy Nguyên tố hiện tại dùng cho các Kỹ năng khác
    public static MageElement getActiveElement(Player player) {
        if (!player.getPersistentDataContainer().has(ACTIVE_ELEMENT_KEY, PersistentDataType.STRING)) return MageElement.FIRE; // Default fallback
        try {
            return MageElement.valueOf(player.getPersistentDataContainer().get(ACTIVE_ELEMENT_KEY, PersistentDataType.STRING));
        } catch (Exception e) {
            return MageElement.FIRE;
        }
    }
}