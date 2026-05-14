package me.antir.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.TitlesModule;
import me.antir.api.ITitleHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TitleComponent implements Component<EntityStore>, ITitleHolder
{
    public static final BuilderCodec<TitleComponent> CODEC;

    private List<String> learnedTitleIds = new ArrayList<>();
    private String[] equippedTitleSlots = new String[1];
    private int maxEquippedTitles = 1;
    private Set<String> activeSynergies = new HashSet<>();

    public static ComponentType<EntityStore, TitleComponent> getComponentType()
    {
        return TitlesModule.get().getTitleComponentType();
    }

    @Override
    public boolean hasTitle(String titleId)
    {
        return learnedTitleIds.contains(titleId);
    }

    @Override
    public boolean addTitle(String titleId)
    {
        if (learnedTitleIds.contains(titleId)) return false;
        learnedTitleIds.add(titleId);
        return true;
    }

    @Override
    public boolean removeTitle(String titleId)
    {
        return learnedTitleIds.remove(titleId);
    }

    @Override
    public List<String> getTitleIds()
    {
        return Collections.unmodifiableList(learnedTitleIds);
    }

    @Override
    public List<String> getEquippedTitleIds()
    {
        List<String> result = new ArrayList<>();
        for (String slot : equippedTitleSlots)
        {
            if (slot != null) result.add(slot);
        }
        return Collections.unmodifiableList(result);
    }

    /** Full slot array — null entries are empty slots. Used for UI slot display. */
    public String[] getEquippedTitleSlots()
    {
        return Arrays.copyOf(equippedTitleSlots, equippedTitleSlots.length);
    }

    @Override
    public boolean equipTitle(String titleId)
    {
        if (!learnedTitleIds.contains(titleId)) return false;
        for (String slot : equippedTitleSlots)
        {
            if (titleId.equals(slot)) return false;
        }
        for (int i = 0; i < equippedTitleSlots.length; i++)
        {
            if (equippedTitleSlots[i] == null)
            {
                equippedTitleSlots[i] = titleId;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean unequipTitle(String titleId)
    {
        for (int i = 0; i < equippedTitleSlots.length; i++)
        {
            if (titleId.equals(equippedTitleSlots[i]))
            {
                equippedTitleSlots[i] = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public int getMaxEquippedTitles()
    {
        return maxEquippedTitles;
    }

    public void setMaxEquippedTitles(int max)
    {
        int newMax = Math.max(1, max);
        if (newMax == this.maxEquippedTitles) return;
        equippedTitleSlots = Arrays.copyOf(equippedTitleSlots, newMax);
        this.maxEquippedTitles = newMax;
    }

    public Set<String> getActiveSynergies()
    {
        return activeSynergies;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone()
    {
        TitleComponent copy = new TitleComponent();
        copy.learnedTitleIds = new ArrayList<>(this.learnedTitleIds);
        copy.maxEquippedTitles = this.maxEquippedTitles;
        copy.equippedTitleSlots = Arrays.copyOf(this.equippedTitleSlots, this.equippedTitleSlots.length);
        copy.activeSynergies = new HashSet<>(this.activeSynergies);
        return copy;
    }

    static
    {
        CODEC = BuilderCodec.builder(TitleComponent.class, TitleComponent::new)
                .append(new KeyedCodec<>("LearnedTitles", Codec.STRING_ARRAY),
                        (comp, value) -> comp.learnedTitleIds = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
                        comp -> comp.learnedTitleIds.toArray(new String[0])).add()
                // MaxEquippedTitles must be decoded before EquippedTitles so the slot array
                // is correctly sized before titles are filled into it.
                .append(new KeyedCodec<>("MaxEquippedTitles", Codec.INTEGER),
                        (comp, value) -> {
                            comp.maxEquippedTitles = value != null ? Math.max(1, value) : 1;
                            comp.equippedTitleSlots = new String[comp.maxEquippedTitles];
                        },
                        comp -> comp.maxEquippedTitles).add()
                .append(new KeyedCodec<>("EquippedTitles", Codec.STRING_ARRAY),
                        (comp, value) -> {
                            if (value == null) return;
                            for (int i = 0; i < Math.min(value.length, comp.equippedTitleSlots.length); i++)
                            {
                                comp.equippedTitleSlots[i] = value[i].isEmpty() ? null : value[i];
                            }
                        },
                        comp -> {
                            List<String> equipped = new ArrayList<>();
                            for (String slot : comp.equippedTitleSlots)
                            {
                                if (slot != null) equipped.add(slot);
                            }
                            return equipped.toArray(new String[0]);
                        }).add()
                .append(new KeyedCodec<>("ActiveSynergies", Codec.STRING_ARRAY),
                        (comp, value) -> comp.activeSynergies = value != null ? new HashSet<>(Arrays.asList(value)) : new HashSet<>(),
                        comp -> comp.activeSynergies.toArray(new String[0])).add()
                .build();
    }
}
