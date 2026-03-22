package vn.minecraft.rPGCore.skills.summoner.pets;

import java.util.HashMap;
import java.util.Map;

public class SummonPetManager {
    private static final Map<String, IPetProfile> profiles = new HashMap<>();

    public static void register(IPetProfile profile) {
        profiles.put(profile.getId(), profile);
    }

    public static IPetProfile getProfile(String id) {
        return profiles.get(id);
    }
}