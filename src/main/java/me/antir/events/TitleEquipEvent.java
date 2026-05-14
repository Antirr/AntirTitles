package me.antir.events;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import me.antir.data.ITitle;

import javax.annotation.Nonnull;

public class TitleEquipEvent extends CancellableEcsEvent
{
    @Nonnull
    public final ITitle title;
    public final TitleEquipEventType type;

    public TitleEquipEvent(@Nonnull ITitle title, TitleEquipEventType type)
    {
        this.title = title;
        this.type = type;
    }

    public enum TitleEquipEventType
    {
        EQUIP,
        UNEQUIP
    }
}
