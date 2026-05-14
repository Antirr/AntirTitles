package me.antir.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.TitlesModule;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists per-entity progress toward title requirements (e.g. kill counts, quest steps).
 * Keys are {@code "titleId:requirementIndex"} and values are accumulated integer counts.
 */
public class RequirementProgressComponent implements Component<EntityStore>
{
    public static final BuilderCodec<RequirementProgressComponent> CODEC;

    private Map<String, Integer> progress = new HashMap<>();

    public static ComponentType<EntityStore, RequirementProgressComponent> getComponentType()
    {
        return TitlesModule.get().getRequirementProgressComponentType();
    }

    public int getProgress(String key)
    {
        return progress.getOrDefault(key, 0);
    }

    public int incrementProgress(String key, int amount)
    {
        int next = progress.getOrDefault(key, 0) + amount;
        progress.put(key, next);
        return next;
    }

    public void setProgress(String key, int value)
    {
        progress.put(key, Math.max(0, value));
    }

    @Nullable
    @Override
    public Component<EntityStore> clone()
    {
        RequirementProgressComponent copy = new RequirementProgressComponent();
        copy.progress = new HashMap<>(this.progress);
        return copy;
    }

    static
    {
        CODEC = BuilderCodec.builder(RequirementProgressComponent.class, RequirementProgressComponent::new)
                .append(new KeyedCodec<>("Progress", Codec.STRING_ARRAY),
                        (comp, entries) -> {
                            comp.progress = new HashMap<>();
                            if (entries != null)
                            {
                                for (String entry : entries)
                                {
                                    int eq = entry.indexOf('=');
                                    if (eq > 0)
                                    {
                                        try
                                        {
                                            comp.progress.put(entry.substring(0, eq),
                                                    Integer.parseInt(entry.substring(eq + 1)));
                                        }
                                        catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                        },
                        comp -> comp.progress.entrySet().stream()
                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                                .toArray(String[]::new)).add()
                .build();
    }
}
