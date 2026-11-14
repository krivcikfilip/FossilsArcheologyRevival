package com.github.teamfossilsarcheology.fossil.block;

import com.github.teamfossilsarcheology.fossil.FossilMod;
import com.github.teamfossilsarcheology.fossil.world.feature.configuration.ModConfiguredFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

/**
 * PORT(1.21.1): AbstractTreeGrower was replaced by the final class {@link TreeGrower} in 1.20.3.
 * Replaces world/feature/tree/*TreeGrower (which always returned a single configured feature).
 */
public class ModTreeGrowers {
    public static final TreeGrower CALAMITES = create("calamites", ModConfiguredFeatures.CALAMITES_TREE_KEY);
    public static final TreeGrower CORDAITES = create("cordaites", ModConfiguredFeatures.CORDAITES_TREE_KEY);
    public static final TreeGrower MUTANT_TREE = create("mutant_tree", ModConfiguredFeatures.MUTANT_TREE_KEY);
    public static final TreeGrower PALM = create("palm", ModConfiguredFeatures.PALM_TREE_KEY);
    public static final TreeGrower SIGILLARIA = create("sigillaria", ModConfiguredFeatures.SIGILLARIA_TREE_KEY);
    public static final TreeGrower TEMPSKYA = create("tempskya", ModConfiguredFeatures.TEMPSKYA_TREE_KEY);

    private static TreeGrower create(String name, ResourceKey<ConfiguredFeature<?, ?>> tree) {
        return new TreeGrower(FossilMod.MOD_ID + ":" + name, Optional.empty(), Optional.of(tree), Optional.empty());
    }
}
