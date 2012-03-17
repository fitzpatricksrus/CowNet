package us.fitzpatricksr.cownet.plots;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Generator for a flat map with square plots separated by roads.
 */
public class PlotsChunkGenerator extends ChunkGenerator {
    private int plotSize;
    private int plotHeight;
    private Material bed = Material.BEDROCK;
    private Material base = Material.DIRT;
    private Material surface = Material.GRASS;
    private Material path = Material.DOUBLE_STEP;

    private byte bedId = (byte) bed.getId();
    private byte baseId = (byte) base.getId();
    private byte surfaceId = (byte) surface.getId();
    private byte pathId = (byte) path.getId();

    public PlotsChunkGenerator(int size, int height, Material base, Material surface, Material path) {
        this.plotSize = size + 7;
        this.plotHeight = height;

        this.surfaceId = (byte) surface.getId();
        this.pathId = (byte) path.getId();
    }

    public List<BlockPopulator> getDefaultPopulators(World world) {
        return new ArrayList<BlockPopulator>();
    }

    public Location getFixedSpawnLocation(World world, Random rand) {
        return new Location(world, 0, 18, 0);
    }

    public int coordsToByte(int x, int y, int z) {
        return (x * 16 + z) * 128 + y;
    }

    private boolean isPathBlock(int x, int z) {
        return (x % this.plotSize == 0) ||
                (z % this.plotSize == 0) ||
                ((x + 1) % this.plotSize == 0) ||
                ((z + 1) % this.plotSize == 0) ||
                ((x - 1) % this.plotSize == 0) ||
                ((z - 1) % this.plotSize == 0) ||
                ((x + 2) % this.plotSize == 0) ||
                ((z + 2) % this.plotSize == 0) ||
                ((x - 2) % this.plotSize == 0) ||
                ((z - 2) % this.plotSize == 0);
    }

    public byte[] generate(World world, Random random, int chunkX, int chunkZ) {
        byte[] blocks = new byte[32768];
        int x, y, z;

        int worldChunkX = chunkX << 4;
        int worldChunkZ = chunkZ << 4;

        for (x = 0; x < 16; ++x) {
            for (z = 0; z < 16; ++z) {
                blocks[this.coordsToByte(x, 0, z)] = this.bedId;

                for (y = 1; y < this.plotHeight; ++y) {
                    blocks[this.coordsToByte(x, y, z)] = this.baseId;
                }

                if (this.isPathBlock(worldChunkX + x, worldChunkZ + z)) {
                    blocks[this.coordsToByte(x, this.plotHeight, z)] = this.pathId;
                } else {
                    blocks[this.coordsToByte(x, this.plotHeight, z)] = this.surfaceId;
                }
            }
        }

        return blocks;
    }

    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        byte[][] result = new byte[16][];
        int worldChunkX = chunkX << 4;
        int worldChunkZ = chunkZ << 4;
        byte bedId = (byte) Material.BEDROCK.getId();
        byte baseId = (byte) Material.DIRT.getId();

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                setBlock(result, x, 0, z, bedId);

                for (int y = 1; y < this.plotHeight; ++y) {
                    setBlock(result, x, y, z, baseId);
                }
                if (this.isPathBlock(worldChunkX + x, worldChunkZ + z)) {
                    setBlock(result, x, this.plotHeight, z, this.pathId);
                } else {
                    setBlock(result, x, this.plotHeight, z, getBiomeSurfaceBlock(biomes.getBiome(x, z), random));
                    setBlock(result, x, this.plotHeight + 1, z, getBiomeAboveSurfaceBlock(biomes.getBiome(x, z), random));
                }
            }
        }
        return result;
    }

    void setBlock(byte[][] result, int x, int y, int z, Material m) {
        setBlock(result, x, y, z, (byte) m.getId());
    }

    void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
        if (blkid == 0) return;
        if (result[y >> 4] == null) {
            result[y >> 4] = new byte[4096];
        }
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }

    public Material getBiomeSurfaceBlock(Biome b, Random random) {
        BiomeContents contents = biomeContents.get(b);
        return (contents == null) ? Material.GRASS : contents.nextMaterial(random);
    }

    public Material getBiomeAboveSurfaceBlock(Biome b, Random random) {
        BiomeContents contents = biomeDecorations.get(b);
        return (contents == null) ? Material.AIR : contents.nextMaterial(random);
    }

    private static HashMap<Biome, BiomeContents> biomeContents = new HashMap<Biome, BiomeContents>();

    static {
        contentsFor(biomeContents, Material.GRASS,
                Biome.EXTREME_HILLS,
                Biome.FOREST_HILLS,
                Biome.TAIGA_HILLS,
                Biome.SMALL_MOUNTAINS)
                .add(Material.STONE, 2.0);
        contentsFor(biomeContents, Material.GRASS,
                Biome.PLAINS,
                Biome.SAVANNA,
                Biome.SEASONAL_FOREST,
                Biome.FOREST,
                Biome.TAIGA,
                Biome.SHRUBLAND,
                Biome.JUNGLE,
                Biome.JUNGLE_HILLS);
        contentsFor(biomeContents, Material.SAND,
                Biome.DESERT,
                Biome.BEACH)
                .add(Material.SANDSTONE, 10)
                .add(Material.GRAVEL, 2);
        contentsFor(biomeContents, Material.SNOW_BLOCK,
                Biome.ICE_DESERT,
                Biome.TUNDRA,
                Biome.ICE_PLAINS,
                Biome.ICE_MOUNTAINS);
        contentsFor(biomeContents, Material.WATER,
                Biome.OCEAN,
                Biome.RIVER,
                Biome.FROZEN_OCEAN,
                Biome.FROZEN_RIVER);
        contentsFor(biomeContents, Material.AIR,
                Biome.SKY);
        contentsFor(biomeContents, Material.NETHERRACK,
                Biome.HELL)
                .add(Material.SOUL_SAND, 20);
        contentsFor(biomeContents, Material.GRASS,
                Biome.SWAMPLAND,
                Biome.RAINFOREST,
                Biome.MUSHROOM_ISLAND,
                Biome.MUSHROOM_SHORE);
    }

    private static HashMap<Biome, BiomeContents> biomeDecorations = new HashMap<Biome, BiomeContents>();

    static {
        contentsFor(biomeDecorations, Material.AIR,
                Biome.RAINFOREST)
                .add(Material.SAPLING, 0.2)
                .add(Material.LEAVES, 0.2);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.SWAMPLAND,
                Biome.JUNGLE,
                Biome.JUNGLE_HILLS);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.SEASONAL_FOREST,
                Biome.FOREST,
                Biome.FOREST_HILLS)
                .add(Material.LEAVES, 0.2)
                .add(Material.LONG_GRASS, 0.2)
                .add(Material.SAPLING, 0.2)
                .add(Material.MELON_BLOCK, 0.1);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.TAIGA,
                Biome.TAIGA_HILLS,
                Biome.SAVANNA,
                Biome.EXTREME_HILLS,
                Biome.SMALL_MOUNTAINS)
                .add(Material.YELLOW_FLOWER, 0.1)
                .add(Material.RED_ROSE, 0.1);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.SHRUBLAND);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.DESERT,
                Biome.DESERT_HILLS)
                .add(Material.CACTUS, 0.1)
                .add(Material.DEAD_BUSH, 0.02);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.PLAINS)
                .add(Material.LONG_GRASS, 0.3)
                .add(Material.SAPLING, 0.1)
                .add(Material.CROPS, 0.1)
                .add(Material.SUGAR_CANE_BLOCK, 0.1);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.TUNDRA)
                .add(Material.SNOW, 20)
                .add(Material.ICE, 2);
        contentsFor(biomeDecorations, Material.ICE,
                Biome.FROZEN_OCEAN,
                Biome.FROZEN_RIVER)
                .add(Material.SNOW, 2)
                .add(Material.AIR, 2);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.ICE_DESERT,
                Biome.ICE_PLAINS,
                Biome.ICE_MOUNTAINS)
                .add(Material.SNOW, 50)
                .add(Material.ICE, 25);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.MUSHROOM_ISLAND,
                Biome.MUSHROOM_SHORE)
                .add(Material.RED_MUSHROOM, 0.2);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.RIVER)
                .add(Material.WATER_LILY, 1);
        contentsFor(biomeDecorations, Material.AIR,
                Biome.BEACH,
                Biome.HELL,
                Biome.SKY,
                Biome.OCEAN);
    }

    private static BiomeContents contentsFor(HashMap<Biome, BiomeContents> map, Material fill, Biome... biomes) {
        BiomeContents results = new BiomeContents(fill);
        for (Biome b : biomes) {
            map.put(b, results);
        }
        return results;
    }

    private static class BiomeContents {
        private Material[] contents = new Material[1000];
        private int nextFree = 0;

        public BiomeContents(Material fill) {
            for (int i = 0; i < contents.length; i++) contents[i] = fill;
        }

        public BiomeContents add(Material material, double dFrequency) {
            int frequency = (int) (dFrequency * 10);
            for (int i = nextFree; i < contents.length && i < nextFree + frequency; i++) {
                contents[i] = material;
            }
            nextFree += frequency;
            return this;
        }

        public Material nextMaterial(Random r) {
            return contents[Math.abs(r.nextInt()) % contents.length];
        }
    }
}