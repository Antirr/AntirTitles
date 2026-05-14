package me.antir.data.effect;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.data.ITitle;

import javax.annotation.Nonnull;

public final class TitleEffectContext
{
    @Nonnull public final Ref<EntityStore> ref;
    @Nonnull public final Store<EntityStore> store;
    @Nonnull public final CommandBuffer<EntityStore> commandBuffer;
    @Nonnull public final ITitle title;

    public TitleEffectContext(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer,
                              @Nonnull ITitle title)
    {
        this.ref = ref;
        this.store = store;
        this.commandBuffer = commandBuffer;
        this.title = title;
    }
}
