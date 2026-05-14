package me.antir.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.components.TitleComponent;
import me.antir.data.ITitle;
import me.antir.data.effect.ITitleEffect;
import me.antir.data.effect.TitleEffectContext;
import me.antir.events.TitleEquipEvent;
import me.antir.events.TitleEquipEvent.TitleEquipEventType;
import me.antir.ui.TitleUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TitleEquippedHandler extends EntityEventSystem<EntityStore, TitleEquipEvent>
{
    public TitleEquippedHandler()
    {
        super(TitleEquipEvent.class);
    }

    @Override
    protected boolean shouldProcessEvent(@Nonnull TitleEquipEvent event)
    {
        return super.shouldProcessEvent(event) && event.type == TitleEquipEventType.EQUIP;
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
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (!comp.hasTitle(title.getId()))
        {
            if (playerRef != null) TitleUI.showError(playerRef, Message.translation("server.ui.titles.notLearned"));
            return;
        }
        if (comp.getEquippedTitleIds().contains(title.getId()))
        {
            if (playerRef != null) TitleUI.showError(playerRef, Message.translation("server.ui.titles.alreadyEquipped"));
            return;
        }
        if (comp.getEquippedTitleIds().size() >= comp.getMaxEquippedTitles())
        {
            if (playerRef != null) TitleUI.showError(playerRef, Message.translation("server.ui.titles.noSlot"));
            return;
        }

        comp.equipTitle(title.getId());

        TitleEffectContext ctx = new TitleEffectContext(ref, store, commandBuffer, title);
        for (ITitleEffect effect : title.getActiveEffects()) effect.apply(ctx);

        if (playerRef != null)
        {
            TitleUI.showEquipped(playerRef, title);
        }
    }
}
