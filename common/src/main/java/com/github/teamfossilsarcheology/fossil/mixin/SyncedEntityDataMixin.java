package com.github.teamfossilsarcheology.fossil.mixin;

import com.github.teamfossilsarcheology.fossil.network.SyncedEntityDataHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.locks.ReadWriteLock;

@Mixin(SynchedEntityData.class)
public abstract class SyncedEntityDataMixin implements SyncedEntityDataHelper {

    //PORT(1.21.1): since 1.20.5 itemsById is an array and the ReadWriteLock is gone
    @Shadow
    @Final
    private SynchedEntityData.DataItem<?>[] itemsById;

    @Shadow
    private boolean isDirty;

    @Override
    public void fossilsArcheologyRevival$markNonDefaultAsDirty() {
        for (SynchedEntityData.DataItem<?> item : itemsById) {
            if (!item.isSetToDefault()) {
                item.setDirty(true);
                isDirty = true;
            }
        }
    }

    @Override
    public void fossilsArcheologyRevival$markDirty(EntityDataAccessor<Integer> dataAccessor) {
        itemsById[dataAccessor.id()].setDirty(true); //PORT(1.21.1, verified phase 2): EntityDataAccessor is a record since 1.20.5 (getId() -> id()), verify -> compiles
        isDirty = true;
    }
}
