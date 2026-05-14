package me.antir.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import me.antir.ui.TitleUI;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.components.TitleComponent;
import me.antir.data.ITitle;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.effect.TitleEffectContext;
import me.antir.events.TitleEquipEvent;
import me.antir.events.TitleEquipEvent.TitleEquipEventType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TitleUnequippedHandler extends EntityEventSystem<EntityStore, TitleEquipEvent>
{
    public TitleUnequippedHandler()
    {
        super(TitleEquipEvent.class);
    }

    @Override
    protected boolean shouldProcessEvent(@Nonnull TitleEquipEvent event)
    {
        return super.shouldProcessEvent(event) && event.type == TitleEquipEventType.UNEQUIP;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery()
    {
        return TitleComponent.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull TitleEquipEvent event)
    {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        TitleComponent comp = chunk.getComponent(index, TitleComponent.getComponentType());
        if (comp == null) return;

        ITitle title = event.title;
        if (!comp.getEquippedTitleIds().contains(title.getId())) return;

        TitleEffectContext ctx = new TitleEffectContext(ref, store, commandBuffer, title);

        comp.unequipTitle(title.getId());

        for (ITitleEffect effect : title.getActiveEffects()) effect.remove(ctx);

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null)
        {
            TitleUI.showUnequipped(playerRef, title);
        }
    }
}
