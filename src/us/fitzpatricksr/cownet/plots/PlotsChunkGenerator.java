package us.fitzpatricksr.cownet.plots;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generator for a flat map with square plots separated by roads.
 */
public class PlotsChunkGenerator extends ChunkGenerator {
    private int plotSize;
    private int plotHeight;
    private byte bedId;
    private byte baseId;
    private byte surfaceId;
    private byte pathId;

    public PlotsChunkGenerator(int size, int height, Material base, Material surface, Material path) {
        this.plotSize = size + 7;
        this.plotHeight = height;

        this.bedId = (byte) Material.BEDROCK.getId();
        this.baseId = (byte) (base.equals(Material.GRASS) ? Material.DIRT : base).getId();
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

}