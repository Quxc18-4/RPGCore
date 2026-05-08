package vn.minecraft.rPGCore.managers;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class SchemaManager {

    // Bộ nhớ Cache lưu trữ dữ liệu Cây (Tải 1 lần lên RAM)
    public static final Map<Vector, BlockData> WORLD_TREE_SCHEMA = new HashMap<>();

    public static void loadSchemas() {
        // 1. Tạo thư mục plugins/RPGCore/schemas nếu chưa có
        File folder = new File(RPGCore.getInstance().getDataFolder(), "schemas");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 2. Tìm file worldtree.schem
        File treeFile = new File(folder, "worldtree.schem");
        if (!treeFile.exists()) {
            RPGCore.getInstance().getLogger().warning("[SchemaManager] Khong tim thay file worldtree.schem! Vui long them file vao thu muc schemas.");
            return;
        }

        // 3. Đọc dữ liệu bằng WorldEdit API
        ClipboardFormat format = ClipboardFormats.findByFile(treeFile);
        if (format == null) {
            RPGCore.getInstance().getLogger().warning("[SchemaManager] Dinh dang file schema khong hop le.");
            return;
        }

        WORLD_TREE_SCHEMA.clear(); // Xóa cache cũ nếu dùng lệnh reload

        try (ClipboardReader reader = format.getReader(new FileInputStream(treeFile))) {
            Clipboard clipboard = reader.read();
            BlockVector3 origin = clipboard.getOrigin();

            // SỬ DỤNG .x(), .y(), .z() THAY CHO .getBlockX(), .getBlockY(), .getBlockZ()
            for (int x = clipboard.getMinimumPoint().x(); x <= clipboard.getMaximumPoint().x(); x++) {
                for (int y = clipboard.getMinimumPoint().y(); y <= clipboard.getMaximumPoint().y(); y++) {
                    for (int z = clipboard.getMinimumPoint().z(); z <= clipboard.getMaximumPoint().z(); z++) {

                        BlockVector3 pos = BlockVector3.at(x, y, z);
                        BaseBlock block = clipboard.getFullBlock(pos);

                        if (!block.getBlockType().getMaterial().isAir()) {
                            // Tương tự, cập nhật ở phần tính tọa độ gốc (origin)
                            Vector relativePos = new Vector(
                                    x - origin.x(),
                                    y - origin.y(),
                                    z - origin.z()
                            );

                            BlockData bukkitData = Bukkit.createBlockData(block.getAsString());
                            WORLD_TREE_SCHEMA.put(relativePos, bukkitData);
                        }
                    }
                }
            }
            RPGCore.getInstance().getLogger().info("[SchemaManager] Da tai thanh cong World Tree Schema (" + WORLD_TREE_SCHEMA.size() + " blocks).");
        } catch (Exception e) {
            RPGCore.getInstance().getLogger().severe("[SchemaManager] Loi khi tai schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}