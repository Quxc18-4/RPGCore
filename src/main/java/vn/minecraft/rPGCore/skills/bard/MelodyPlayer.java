//package vn.minecraft.rPGCore.skills.bard;
//
//import org.bukkit.Location;
//import org.bukkit.Sound;
//import org.bukkit.entity.Player;
//import org.bukkit.scheduler.BukkitRunnable;
//import vn.minecraft.rPGCore.RPGCore;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class MelodyPlayer {
//
//    private static final Map<String, Float> PITCH_MAP = new HashMap<>();
//    static {
//        // Octave 3 (F#3 -> B3)
//        PITCH_MAP.put("F#3", 0.5f); PITCH_MAP.put("G3", 0.5297f); PITCH_MAP.put("G#3", 0.5612f);
//        PITCH_MAP.put("A3", 0.5946f); PITCH_MAP.put("Bb3", 0.6299f); PITCH_MAP.put("B3", 0.6674f);
//
//        // Octave 4 (C4 -> B4)
//        PITCH_MAP.put("C4", 0.7071f); PITCH_MAP.put("C#4", 0.7491f); PITCH_MAP.put("D4", 0.7937f);
//        PITCH_MAP.put("Eb4", 0.8408f); PITCH_MAP.put("E4", 0.8908f); PITCH_MAP.put("F4", 0.9438f);
//        PITCH_MAP.put("F#4", 1.0f); PITCH_MAP.put("G4", 1.0594f); PITCH_MAP.put("G#4", 1.1224f);
//        PITCH_MAP.put("A4", 1.1892f); PITCH_MAP.put("Bb4", 1.2599f); PITCH_MAP.put("B4", 1.3348f);
//
//        // Octave 5 (C5 -> F#5)
//        PITCH_MAP.put("C5", 1.4142f); PITCH_MAP.put("C#5", 1.4983f); PITCH_MAP.put("D5", 1.5874f);
//        PITCH_MAP.put("Eb5", 1.6817f); PITCH_MAP.put("E5", 1.7817f); PITCH_MAP.put("F5", 1.8877f);
//        PITCH_MAP.put("F#5", 2.0f);
//    }
//
//    public static class NoteEvent {
//        public final int tick;
//        public final Sound instrument;
//        public final String note;
//        public final float volume;
//
//        public NoteEvent(int tick, Sound instrument, String note, float volume) {
//            this.tick = tick;
//            this.instrument = instrument;
//            this.note = note;
//            this.volume = volume;
//        }
//    }
//
//    // Danh sách toàn bộ 1800 ticks (90 giây) của bài TruE
//    private static final List<NoteEvent> TRUE_SONG_TRACK = new ArrayList<>();
//
//    static {
//        // ĐOẠN ĐIỆP KHÚC TRUE - HOYO-MiX (Bè Giai Điệu Chính & Bass Đệm)
//        // Dùng Harp/Chime cho tay phải, Guitar/Bass cho tay trái
//
//        // --- Nhịp lấy đà ---
//        addNote(0, Sound.BLOCK_NOTE_BLOCK_HARP, "Eb4", 1.5f);
//        addNote(8, Sound.BLOCK_NOTE_BLOCK_HARP, "Ab4", 1.5f);
//        addNote(16, Sound.BLOCK_NOTE_BLOCK_HARP, "Bb4", 1.5f);
//
//        // --- Ô nhịp 1 ---
//        addNote(24, Sound.BLOCK_NOTE_BLOCK_CHIME, "C5", 1.5f);
//        addNote(24, Sound.BLOCK_NOTE_BLOCK_GUITAR, "Ab3", 1.0f); // Bass
//        addNote(32, Sound.BLOCK_NOTE_BLOCK_HARP, "Bb4", 1.5f);
//        addNote(40, Sound.BLOCK_NOTE_BLOCK_CHIME, "Ab4", 1.5f);
//        addNote(40, Sound.BLOCK_NOTE_BLOCK_GUITAR, "Eb4", 0.8f); // Bass
//        addNote(48, Sound.BLOCK_NOTE_BLOCK_HARP, "Bb4", 1.5f);
//
//        // --- Ô nhịp 2 ---
//        addNote(56, Sound.BLOCK_NOTE_BLOCK_CHIME, "Eb5", 1.5f);
//        addNote(56, Sound.BLOCK_NOTE_BLOCK_GUITAR, "C4", 1.0f);  // Bass
//        addNote(64, Sound.BLOCK_NOTE_BLOCK_HARP, "C5", 1.5f);
//        addNote(72, Sound.BLOCK_NOTE_BLOCK_HARP, "Ab4", 1.5f);
//        addNote(72, Sound.BLOCK_NOTE_BLOCK_GUITAR, "Ab3", 0.8f); // Bass
//
//        // --- Ô nhịp 3 ---
//        addNote(88, Sound.BLOCK_NOTE_BLOCK_HARP, "F4", 1.5f);
//        addNote(96, Sound.BLOCK_NOTE_BLOCK_HARP, "Ab4", 1.5f);
//        addNote(96, Sound.BLOCK_NOTE_BLOCK_GUITAR, "Db3", 1.0f); // Bass
//        addNote(104, Sound.BLOCK_NOTE_BLOCK_CHIME, "C5", 1.5f);
//
//        // --- Ô nhịp 4 ---
//        addNote(112, Sound.BLOCK_NOTE_BLOCK_HARP, "Bb4", 1.5f);
//        addNote(112, Sound.BLOCK_NOTE_BLOCK_GUITAR, "F3", 1.0f); // Bass
//        addNote(120, Sound.BLOCK_NOTE_BLOCK_HARP, "Ab4", 1.5f);
//        addNote(128, Sound.BLOCK_NOTE_BLOCK_CHIME, "Bb4", 1.5f);
//        addNote(136, Sound.BLOCK_NOTE_BLOCK_CHIME, "C5", 1.5f);
//
//        // Vòng lặp test ngắn...
//    }
//
//    private static void addNote(int tick, Sound inst, String note, float vol) {
//        TRUE_SONG_TRACK.add(new NoteEvent(tick, inst, note, vol));
//    }
//
//    public static BukkitRunnable playTruESong(Player bard, int durationSeconds) {
//        return new BukkitRunnable() {
//            int currentTick = 0;
//            int eventIndex = 0;
//            final int maxDurationTicks = durationSeconds * 20; // Phát xuyên suốt thời gian Ulti
//
//            @Override
//            public void run() {
//                if (!bard.isOnline() || bard.isDead() || currentTick >= maxDurationTicks) {
//                    this.cancel();
//                    return;
//                }
//
//                Location loc = bard.getLocation();
//
//                // Duyệt qua tất cả các nốt thuộc tick hiện tại
//                while (eventIndex < TRUE_SONG_TRACK.size()) {
//                    NoteEvent event = TRUE_SONG_TRACK.get(eventIndex);
//
//                    // Nếu nốt nằm ở thời điểm hiện tại -> Phát nhạc
//                    if (event.tick == (currentTick % 1800)) { // 1800 ticks = 90 giây
//                        if (PITCH_MAP.containsKey(event.note)) {
//                            loc.getWorld().playSound(loc, event.instrument, event.volume, PITCH_MAP.get(event.note));
//                        }
//                        eventIndex++;
//                    } else if (event.tick < (currentTick % 1800)) {
//                        eventIndex++;
//                    } else {
//                        break; // Nốt thuộc tick tương lai -> Chờ lượt sau
//                    }
//                }
//
//                // Khi hết 90s (1800 ticks), reset index về 0 để lặp lại nếu Ulti còn thời gian
//                if ((currentTick % 1800) == 1799) {
//                    eventIndex = 0;
//                }
//
//                currentTick++;
//            }
//        };
//    }
//}