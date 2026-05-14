package me.antir.data.effect;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;

import javax.annotation.Nonnull;

public class GrantItemEffect implements ITitleEffect
{
    @Nonnull
    public static final BuilderCodec<GrantItemEffect> CODEC = BuilderCodec.builder(GrantItemEffect.class, GrantItemEffect::new)
            .append(new KeyedCodec<>("ItemId", Codec.STRING),
                    (effect, value) -> effect.itemId = value != null ? value : "", effect -> effect.itemId).add()
            .append(new KeyedCodec<>("Amount", Codec.INTEGER),
                    (effect, value) -> effect.amount = value != null ? value : 1, effect -> effect.amount).add()
            .build();

    private String itemId = "";
    private int amount = 1;

    @Override
    public String getUiColor() { return "#c8a050"; }

    @Override
    public String getDescription()
    {
        return "Grants " + amount + "x " + itemId + " on acquire";
    }

    public static GrantItemEffect create(@Nonnull String itemId, int amount)
    {
        GrantItemEffect effect = new GrantItemEffect();
        effect.itemId = itemId;
        effect.amount = amount;
        return effect;
    }

    @Override
    public void apply(TitleEffectContext ctx)
    {
        if (itemId.isEmpty()) return;
        CombinedItemContainer container = InventoryComponent.getCombined(ctx.commandBuffer, ctx.ref, InventoryComponent.HOTBAR_FIRST);
        container.addItemStack(new ItemStack(itemId, amount));
    }

    @Override
    public void remove(TitleEffectContext ctx)
    {
        // Item grants are not reversible
    }
}
