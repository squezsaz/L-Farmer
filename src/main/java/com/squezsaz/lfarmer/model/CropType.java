package com.squezsaz.lfarmer.model;

import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import java.util.EnumSet;
import java.util.Set;

public enum CropType {

    WHEAT(Material.WHEAT, Material.WHEAT, Material.WHEAT_SEEDS),
    CARROT(Material.CARROTS, Material.CARROT, Material.CARROT),
    POTATO(Material.POTATOES, Material.POTATO, Material.POTATO),
    BEETROOT(Material.BEETROOTS, Material.BEETROOT, Material.BEETROOT_SEEDS),
    NETHER_WART(Material.NETHER_WART, Material.NETHER_WART, Material.NETHER_WART),
    COCOA(Material.COCOA, Material.COCOA_BEANS, Material.COCOA_BEANS),
    SUGAR_CANE(Material.SUGAR_CANE, Material.SUGAR_CANE, Material.SUGAR_CANE),
    CACTUS(Material.CACTUS, Material.CACTUS, Material.CACTUS),
    MELON(Material.MELON, Material.MELON_SLICE, Material.MELON_SEEDS),
    PUMPKIN(Material.PUMPKIN, Material.PUMPKIN, Material.PUMPKIN_SEEDS),
    SWEET_BERRY(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES, Material.SWEET_BERRIES),
    BAMBOO(Material.BAMBOO, Material.BAMBOO, Material.BAMBOO);

    private final Material blockMaterial;
    private final Material dropMaterial;
    private final Material seedMaterial;

    private static final Set<Material> HARVESTABLE_MATERIALS = EnumSet.noneOf(Material.class);

    static {
        for (CropType type : values()) {
            HARVESTABLE_MATERIALS.add(type.blockMaterial);
        }
    }

    CropType(Material blockMaterial, Material dropMaterial, Material seedMaterial) {
        this.blockMaterial = blockMaterial;
        this.dropMaterial = dropMaterial;
        this.seedMaterial = seedMaterial;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public Material getDropMaterial() {
        return dropMaterial;
    }

    public Material getSeedMaterial() {
        return seedMaterial;
    }

    public static boolean isHarvestable(Material material) {
        return HARVESTABLE_MATERIALS.contains(material);
    }

    public static CropType fromMaterial(Material material) {
        for (CropType crop : values()) {
            if (crop.blockMaterial == material) {
                return crop;
            }
        }
        return null;
    }

    public static boolean isMature(BlockData blockData) {
        if (blockData instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return true;
    }
}
