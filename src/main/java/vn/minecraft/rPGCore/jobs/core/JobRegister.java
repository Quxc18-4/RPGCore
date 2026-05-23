package vn.minecraft.rPGCore.jobs.core;

import vn.minecraft.rPGCore.jobs.types.*;
// Import các nghề khác sau này (VD: ChefJob, MerchantJob...)

public class JobRegister {

    // Gọi hàm này 1 lần duy nhất khi khởi động server
    public static void registerAllJobs() {
        JobManager.registerJob(new BlacksmithJob());
        JobManager.registerJob(new ChefJob()); // ĐÃ KÍCH HOẠT
        JobManager.registerJob(new FarmerJob());
        JobManager.registerJob(new FishermanJob());
        JobManager.registerJob(new MerchantJob());
    }
}