package me.antir.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.data.Title;
import me.antir.events.TitleGrantEvent;
import me.antir.events.TitleGrantEvent.TitleGrantEventType;

import javax.annotation.Nonnull;
import java.awt.*;

public class GrantTitleInteraction extends SimpleInteraction
{
    @Nonnull
    public static final BuilderCodec<GrantTitleInteraction> CODEC;

    private String titleToGrant = "";

    public GrantTitleInteraction(@Nonnull String id)
    {
        super(id);
    }

    protected GrantTitleInteraction()
    {
        super("Grant Title");
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
                         @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler)
    {
        if (!firstRun)
        {
            context.getState().state = InteractionState.Finished;
            return;
        }

        Ref<EntityStore> owningEntity = context.getOwningEntity();
        Store<EntityStore> store = owningEntity.getStore();
        PlayerRef player = store.getComponent(owningEntity, PlayerRef.getComponentType());

        Title title = Title.getAssetMap().getAsset(titleToGrant);
        if (title == null)
        {
            if (player != null) player.sendMessage(Message.raw("Invalid title: " + titleToGrant).color(Color.RED));
            context.getState().state = InteractionState.Finished;
            return;
        }

        store.invoke(owningEntity, new TitleGrantEvent(title, TitleGrantEventType.GRANT));

        InventoryComponent.Hotbar hotbar = store.getComponent(owningEntity, InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null)
        {
            hotbar.getInventory().removeItemStackFromSlot(context.getHeldItemSlot(), 1, true, false);
        }

        context.getState().state = InteractionState.Finished;
    }

    @Nonnull
    @Override
    public String toString()
    {
        return "GrantTitleInteraction{titleToGrant='" + titleToGrant + "'} " + super.toString();
    }

    static
    {
        CODEC = BuilderCodec.builder(GrantTitleInteraction.class, GrantTitleInteraction::new, SimpleInteraction.CODEC)
                .append(new KeyedCodec<>("TitleToGrant", Codec.STRING),
                        (comp, value, info) -> comp.titleToGrant = value != null ? value : "",
                        (comp, info) -> comp.titleToGrant).add()
                .build();
    }
}
