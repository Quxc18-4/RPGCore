package vn.minecraft.rPGCore.jobs.quests;

public enum QuestAction {
    FORGE_SUCCESS,         // Đập đồ thành công
    FORGE_FAIL,            // Đập đồ thất bại
    FORGE_ANY,             // Đập đồ (Bất kể thành bại)
    COOK_FINISH,           // Nấu xong 1 món
    FISH_CATCH,            // Câu được cá
    GATHER_ITEM,           // Nhặt/Thu thập vật phẩm
    MINE_BLOCK,            // Đào block

    // [CÁC HÀNH ĐỘNG MỚI BỔ SUNG]
    USE_MASTER_FORGE,      // Dùng nút rèn bậc thầy
    CONSUME_UPGRADE_STONE, // Tiêu hao đá nâng cấp
    FORGE_STREAK           // Chuỗi đập thành công liên tiếp
}