package me.antir.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.antir.ui.TitleUI;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.components.TitleComponent;
import me.antir.data.ITitle;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.effect.TitleEffectContext;
import me.antir.events.TitleEquipEvent;
import me.antir.events.TitleEquipEvent.TitleEquipEventType;
import me.antir.events.TitleGrantEvent;
import me.antir.events.TitleGrantEvent.TitleGrantEventType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TitleRemovedHandler extends EntityEventSystem<EntityStore, TitleGrantEvent>
{
    public TitleRemovedHandler()
    {
        super(TitleGrantEvent.class);
    }

    @Override
    protected boolean shouldProcessEvent(@Nonnull TitleGrantEvent event)
    {
        return super.shouldProcessEvent(event) && event.type == TitleGrantEventType.REMOVE;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery()
    {
        return TitleComponent.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull TitleGrantEvent event)
    {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        TitleComponent comp = chunk.getComponent(index, TitleComponent.getComponentType());
        if (comp == null) return;

        ITitle title = event.title;
        if (!comp.hasTitle(title.getId())) return;

        // Auto-unequip first so equip effects and synergies are cleaned up
        if (comp.getEquippedTitleIds().contains(title.getId()))
        {
            store.invoke(ref, new TitleEquipEvent(title, TitleEquipEventType.UNEQUIP));
        }

        comp.removeTitle(title.getId());

        TitleEffectContext ctx = new TitleEffectContext(ref, store, commandBuffer, title);
        for (ITitleEffect effect : title.getPassiveEffects()) effect.remove(ctx);

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null)
        {
            TitleUI.showRemoved(playerRef, title);
        }
    }
}
