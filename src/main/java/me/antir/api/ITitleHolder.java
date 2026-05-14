package me.antir.api;

import java.util.List;

public interface ITitleHolder
{
    boolean hasTitle(String titleId);

    boolean addTitle(String titleId);

    boolean removeTitle(String titleId);

    List<String> getTitleIds();

    List<String> getEquippedTitleIds();

    boolean equipTitle(String titleId);

    boolean unequipTitle(String titleId);

    int getMaxEquippedTitles();
}
