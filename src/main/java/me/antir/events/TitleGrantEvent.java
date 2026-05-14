package me.antir.events;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import me.antir.data.ITitle;

import javax.annotation.Nonnull;

public class TitleGrantEvent extends CancellableEcsEvent
{
    @Nonnull
    public final ITitle title;
    public final TitleGrantEventType type;

    public TitleGrantEvent(@Nonnull ITitle title)
    {
        this(title, TitleGrantEventType.GRANT);
    }

    public TitleGrantEvent(@Nonnull ITitle title, TitleGrantEventType type)
    {
        this.title = title;
        this.type = type;
    }

    public enum TitleGrantEventType
    {
        GRANT,
        REMOVE
    }
}
