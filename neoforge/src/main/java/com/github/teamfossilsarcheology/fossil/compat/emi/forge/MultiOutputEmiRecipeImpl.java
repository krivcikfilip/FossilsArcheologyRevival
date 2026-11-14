package com.github.teamfossilsarcheology.fossil.compat.emi.forge;

import com.github.teamfossilsarcheology.fossil.compat.emi.MultiOutputEmiRecipe;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiTagKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Access to some internal code
 * @see MultiOutputEmiRecipe
 */
public class MultiOutputEmiRecipeImpl {
    public static EmiIngredient toIngredient(List<MultiOutputEmiRecipe.WeightedItem> list) {
        if (list.isEmpty()) {
            return EmiStack.EMPTY;
        } else if (list.size() == 1) {
            MultiOutputEmiRecipe.WeightedItem weightedItem = list.get(0);
            if (weightedItem.tagKey() == null) {
                if (weightedItem.stack().isEmpty()) {
                    //Air is a valid output
                    return new ItemEmiStack(weightedItem.stack(), 1).setChance((float) weightedItem.probability());
                }
                return EmiStack.of(weightedItem.stack()).setChance((float) weightedItem.probability());
            } else {
                //For custom recipe display. Probability is the probability of getting anything from the tag
                //PORT(1.21.1): EmiUtil.values(TagKey) was removed from EMI 1.1.x (1.21); EmiTagKey#stream streams the tag values
                //PORT(1.21.1, verified phase 2): EMI internal API, verify against the 1.21.1 jar -> compiles against emi-neoforge 1.1.24+1.21.1
                return new ListEmiIngredient(EmiTagKey.of(weightedItem.tagKey()).stream().map(item -> EmiStack.of(new ItemStack(item)).setChance((float) (weightedItem.probability()))).toList(), 1);
            }
        } else {
            return new ListEmiIngredient(toStack(list), 1);
        }
    }

    public static List<EmiIngredient> toStack(List<MultiOutputEmiRecipe.WeightedItem> list) {
        return list.stream().map(weightedItem -> {
            if (weightedItem.tagKey() == null) {
                return EmiStack.of(weightedItem.stack()).setChance((float) weightedItem.probability());
            } else {
                return new TagEmiIngredient(weightedItem.tagKey(), 1).setChance((float) weightedItem.probability());
            }
        }).toList();
    }

    public static List<EmiStack> values(TagKey<Item> key) {
        //PORT(1.21.1): EmiTags#getRawValues takes an EmiTagKey now
        return EmiTags.getRawValues(EmiTagKey.of(key));
    }
}
