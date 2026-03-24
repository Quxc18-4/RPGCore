package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface IPetProfile {
    String getId(); // Tương ứng với Pet_Type (VD: "AlphaSpiritWolf")

    // Xác định loại sát thương.
    boolean isMagicDamage();

    // Trả về ID của dạng Dung hợp (Ví dụ Sói -> "AlphaSpiritWolf"). Trả về null nếu không thể dung hợp.
    String getFusionTargetId();

    // Tên hiển thị của dạng Dung hợp (Ví dụ: "Tinh Lang Vương")
    String getFusionTargetName();

    // [MỚI]: Hàm xử lý sự kiện khi chủ nhân Click chuột phải vào Thú
    // Dùng default để không làm lỗi các file Profile cũ (Sói thường, Golem...)
    default void onInteract(Player player, LivingEntity pet, org.bukkit.inventory.ItemStack item) {}

    // [MỚI]: Định nghĩa sát thương gốc (Flat) của từng loại thú
    double getBaseFlatDamage(int level);

    // [MỚI]: Định nghĩa hệ số nhân sức mạnh (Scaling) từ vũ khí chủ
    double getWeaponScaling(int level);

    // Xử lý bơm chỉ số, kích thước, hiệu ứng khi vừa sinh ra
    void onSpawn(Player owner, LivingEntity pet, int skillLevel, double... extraArgs);

    // Tự quản lý kỹ năng khi đánh. Trả về Hệ số Sát thương (Multiplier)
    double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage);

    // Tự quản lý hiệu ứng khi chết (Phát nổ, Sấm sét...)
    void onDeath(Player owner, LivingEntity pet);
}