package com.rtm516.mcxboxbroadcast.core.webrtc.bedrock;

import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class is mostly copied from Geyser
 */
public class PaletteUtils {
    public static final NbtMap BIOMES_PALETTE;
    public static final byte[] EMPTY_LEVEL_CHUNK_DATA;

    private static final NbtMap EMPTY_TAG = NbtMap.EMPTY;

    static {
        /* Load biomes */
        // Build a fake plains biome entry
        NbtMapBuilder plainsBuilder = NbtMap.builder();
        plainsBuilder.putFloat("blue_spores", 0f);
        plainsBuilder.putFloat("white_ash", 0f);
        plainsBuilder.putFloat("ash", 0f);
        plainsBuilder.putFloat("temperature", 0f);
        plainsBuilder.putFloat("red_spores", 0f);
        plainsBuilder.putFloat("downfall", 0f);

        plainsBuilder.put("minecraft:overworld_generation_rules", NbtMap.EMPTY);
        plainsBuilder.put("minecraft:climate", NbtMap.EMPTY);
        plainsBuilder.put("tags", NbtList.EMPTY);

        // Add the fake plains to the map
        NbtMapBuilder biomesBuilder = NbtMap.builder();
        biomesBuilder.put("plains", plainsBuilder.build());

        // Build the biomes palette
        BIOMES_PALETTE = biomesBuilder.build();

        /* Create empty chunk data */
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(new byte[258]); // Biomes + Border Size + Extra Data Size

            try (NBTOutputStream nbtOutputStream = NbtUtils.createNetworkWriter(outputStream)) {
                nbtOutputStream.writeTag(EMPTY_TAG);
            }

            EMPTY_LEVEL_CHUNK_DATA = outputStream.toByteArray();
        } catch (IOException e) {
            throw new AssertionError("Unable to generate empty level chunk data");
        }
    }
}
