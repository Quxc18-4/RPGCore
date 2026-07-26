package vn.minecraft.rPGCore.jobs.quests;

public enum QuestAction {
    FORGE_SUCCESS,         // Đập đồ thành công
    FORGE_FAIL,            // Đập đồ thất bại
    FORGE_ANY,             // Đập đồ (Bất kể thành bại)
    FISH_CATCH,            // Câu được cá
    GATHER_ITEM,           // Nhặt/Thu thập vật phẩm
    MINE_BLOCK,            // Đào block

    // [CÁC HÀNH ĐỘNG MỚI BỔ SUNG]
    USE_MASTER_FORGE,      // Dùng nút rèn bậc thầy
    CONSUME_UPGRADE_STONE, // Tiêu hao đá nâng cấp
    FORGE_STREAK,           // Chuỗi đập thành công liên tiếp

    // --- CỦA ĐẦU BẾP (MỚI THÊM) ---
    COOK_ANY,
    COOK_RUINED,
    COOK_SINGLE,
    COOK_FULL,
    CONSUME_FOOD,     // Hành động ăn thức ăn
    BOREDOM_GAIN,      // Hành động bị tăng điểm ngấy

    // --- FARMER ACTIONS ---
    PLANT_SEED,             // Trồng hạt giống
    HARVEST_CROP,           // Thu hoạch nông sản thành công
    COLLECT_NATURE_ESSENCE, // Nhặt được Tinh chất tự nhiên
    HARVEST_MUTATION,       // Thu hoạch ra Nông sản 1.5x hoặc 2.0x
    USE_FERTILIZER,         // Dùng phân bón AoE
    RECOVER_SEED,           // Thu hồi lại được hạt giống sau khi hái
    HARVEST_MAGIC_CROP,      // Thu hoạch Nông sản Ma thuật

    // --- NGƯ DÂN ACTIONS ---
    FISH_FAIL,             // Câu trượt / Đứt dây
    FISH_TRASH,            // Câu dính rác
    FISH_TREASURE,         // Câu được rương kho báu
    CRAFT_BAIT,            // Chế tạo mồi câu thành công
    COLLECT_OCEAN_TEAR,     // Nhặt được Nước Mắt Đại Dương

    // --- THƯƠNG NHÂN ACTIONS ---
    MERCHANT_MOVE,         // Di chuyển block
    MERCHANT_SELL,         // Bán item
    MERCHANT_BUY,          // Mua item
    MERCHANT_SPEND,        // Tiêu hao tiền
    MERCHANT_EARN,         // Nhận tiền
    MERCHANT_INTEREST,     // Nhận lãi ngân hàng
    MERCHANT_DELIVER,       // Giao hàng vận tải

    // --- DƯỢC SƯ ACTIONS (MỚI BỔ SUNG) ---
    BREW_SUCCESS,          // Nấu thuốc thành công
    BREW_FAIL,             // Nấu thuốc thất bại (Ra thuốc kinh tởm)
    BREW_ANY,              // Tiến hành nấu thuốc (Bất kể thành bại)
    ALCH_HARVEST_RARE,     // Thu hoạch thảo dược RARE trở lên
    COLLECT_MANA_ESSENCE,  // Tích lũy Tinh Chất Ma Lực
    BREW_PERFECT,           // Chế tạo ra thuốc Hoàn Hảo / Cường Hóa
    ALCH_HARVEST_ANY
}