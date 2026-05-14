package me.antir.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.antir.api.TitleAPI;
import me.antir.components.TitleComponent;
import me.antir.data.Title;
import me.antir.ui.TitleBrowserPage;
import me.antir.ui.admin.AdminTitleBrowserPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

public class TitleCommands extends AbstractCommandCollection
{
    public TitleCommands()
    {
        super("Title", "Antir's Title Commands");
        this.addSubCommand(new GrantTitleCommand());
        this.addSubCommand(new RemoveTitleCommand());
        this.addSubCommand(new EquipTitleCommand());
        this.addSubCommand(new UnequipTitleCommand());
        this.addSubCommand(new TitleInfoCommand());
        this.addSubCommand(new TitleListCommand());
        this.addSubCommand(new TitleBrowseCommand());
        this.addSubCommand(new TitleAdminCommand());
        this.addSubCommand(new SetSlotsCommand());
    }

    public static class GrantTitleCommand extends AbstractPlayerCommand
    {
        @Nonnull private final RequiredArg<String> titleArg;

        public GrantTitleCommand()
        {
            super("grant", "Grant yourself a title");
            this.titleArg = this.withRequiredArg("titleId", "Title ID (e.g. WolfSlayer)", ArgTypes.STRING)
                    .withSuggestionOverride(new TitleIdArgType());
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            String id = titleArg.get(context);
            Title title = Title.getAssetMap().getAsset(id);
            if (title == null)
            {
                playerRef.sendMessage(Message.raw("Unknown title: " + id).color(Color.RED));
                listAvailable(playerRef);
                return;
            }
            TitleAPI.grantTitle(ref, store, title);
        }
    }

    public static class RemoveTitleCommand extends AbstractPlayerCommand
    {
        @Nonnull private final RequiredArg<String> titleArg;

        public RemoveTitleCommand()
        {
            super("remove", "Remove a title from yourself");
            this.titleArg = this.withRequiredArg("titleId", "Title ID (e.g. WolfSlayer)", ArgTypes.STRING)
                    .withSuggestionOverride(new TitleIdArgType());
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            String id = titleArg.get(context);
            Title title = Title.getAssetMap().getAsset(id);
            if (title == null)
            {
                playerRef.sendMessage(Message.raw("Unknown title: " + id).color(Color.RED));
                listAvailable(playerRef);
                return;
            }
            TitleAPI.revokeTitle(ref, store, title);
        }
    }

    public static class EquipTitleCommand extends AbstractPlayerCommand
    {
        @Nonnull private final RequiredArg<String> titleArg;

        public EquipTitleCommand()
        {
            super("equip", "Equip a title you own");
            this.titleArg = this.withRequiredArg("titleId", "Title ID (e.g. WolfSlayer)", ArgTypes.STRING)
                    .withSuggestionOverride(new TitleIdArgType());
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            String id = titleArg.get(context);
            Title title = Title.getAssetMap().getAsset(id);
            if (title == null)
            {
                playerRef.sendMessage(Message.raw("Unknown title: " + id).color(Color.RED));
                return;
            }
            TitleAPI.equipTitle(ref, store, title);
        }
    }

    public static class UnequipTitleCommand extends AbstractPlayerCommand
    {
        @Nonnull private final RequiredArg<String> titleArg;

        public UnequipTitleCommand()
        {
            super("unequip", "Unequip a title");
            this.titleArg = this.withRequiredArg("titleId", "Title ID (e.g. WolfSlayer)", ArgTypes.STRING)
                    .withSuggestionOverride(new TitleIdArgType());
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            String id = titleArg.get(context);
            Title title = Title.getAssetMap().getAsset(id);
            if (title == null)
            {
                playerRef.sendMessage(Message.raw("Unknown title: " + id).color(Color.RED));
                return;
            }
            TitleAPI.unequipTitle(ref, store, title);
        }
    }

    public static class TitleInfoCommand extends AbstractPlayerCommand
    {
        public TitleInfoCommand()
        {
            super("info", "Show your titles");
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            TitleComponent comp = store.ensureAndGetComponent(ref, TitleComponent.getComponentType());
            if (comp == null)
            {
                playerRef.sendMessage(Message.raw("No title data found.").color(Color.RED));
                return;
            }

            List<String> learned = comp.getTitleIds();
            List<String> equipped = comp.getEquippedTitleIds();

            if (learned.isEmpty())
            {
                playerRef.sendMessage(Message.raw("You have no titles.").color(Color.GRAY));
                return;
            }

            playerRef.sendMessage(Message.raw("Titles (" + learned.size() + " learned, "
                    + equipped.size() + "/" + comp.getMaxEquippedTitles() + " equipped):").color(Color.WHITE));

            for (String id : learned)
            {
                Title title = Title.getAssetMap().getAsset(id);
                String name = title != null ? title.getDisplayName() : id;
                boolean isEquipped = equipped.contains(id);
                String line = (isEquipped ? "[Equipped] " : "  ") + name + " (" + id + ")";
                playerRef.sendMessage(Message.raw(line).color(isEquipped ? Color.GREEN : Color.GRAY));
            }
        }
    }

    public static class TitleListCommand extends AbstractPlayerCommand
    {
        public TitleListCommand()
        {
            super("list", "List all available titles");
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            var allTitles = Title.getAssetMap().getAssetMap();
            if (allTitles.isEmpty())
            {
                playerRef.sendMessage(Message.raw("No titles loaded.").color(Color.GRAY));
                return;
            }

            playerRef.sendMessage(Message.raw("Titles (" + allTitles.size() + " loaded):").color(Color.WHITE));
            for (Title title : allTitles.values())
            {
                var quality = title.getRarity();
                String rarityLabel = quality != null ? quality.getId() : "None";
                String line = "  " + title.getDisplayName()
                        + " (" + title.getId() + ")"
                        + " [" + rarityLabel + "]";
                if (title.getRequirements().length > 0)
                {
                    line += " - " + title.getRequirements().length + " requirement(s)";
                }
                playerRef.sendMessage(Message.raw(line).color(Color.GRAY));
            }
        }
    }

    public static class TitleBrowseCommand extends AbstractPlayerCommand
    {
        public TitleBrowseCommand()
        {
            super("browse", "Open the title browser panel");
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new TitleBrowserPage(playerRef));
        }
    }

    public static class TitleAdminCommand extends AbstractPlayerCommand
    {
        public TitleAdminCommand()
        {
            super("admin", "Open the admin title manager (requires antirtitles.admin)");
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            if (!playerRef.hasPermission("antirtitles.admin"))
            {
                playerRef.sendMessage(Message.raw("You do not have permission to use this command.").color(Color.RED));
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new AdminTitleBrowserPage(playerRef));
        }
    }

    public static class SetSlotsCommand extends AbstractPlayerCommand
    {
        @Nonnull private final RequiredArg<String> countArg;

        public SetSlotsCommand()
        {
            super("setslots", "Set your equipped title slot count (requires antirtitles.admin)");
            this.countArg = this.withRequiredArg("count", "Number of title slots (1–10)", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world)
        {
            if (!playerRef.hasPermission("antirtitles.admin"))
            {
                playerRef.sendMessage(Message.raw("You do not have permission to use this command.").color(Color.RED));
                return;
            }
            int count;
            try
            {
                count = Integer.parseInt(countArg.get(context));
            }
            catch (NumberFormatException ignored)
            {
                playerRef.sendMessage(Message.raw("Invalid number.").color(Color.RED));
                return;
            }
            if (count < 1 || count > 10)
            {
                playerRef.sendMessage(Message.raw("Slot count must be between 1 and 10.").color(Color.RED));
                return;
            }
            TitleComponent comp = store.ensureAndGetComponent(ref, TitleComponent.getComponentType());
            comp.setMaxEquippedTitles(count);
            playerRef.sendMessage(Message.raw("Equipped title slots set to " + count + ".").color(Color.WHITE));
        }
    }

    private static void listAvailable(PlayerRef playerRef)
    {
        java.util.Set<String> keys = Title.getAssetMap().getAssetMap().keySet();
        if (keys.isEmpty())
        {
            playerRef.sendMessage(Message.raw("No titles available.").color(Color.GRAY));
            return;
        }
        playerRef.sendMessage(Message.raw("Available: " + String.join(", ", keys)).color(Color.GRAY));
    }

    /** Argument type that provides loaded title IDs as tab-completion suggestions. */
    private static class TitleIdArgType extends SingleArgumentType<String>
    {
        TitleIdArgType()
        {
            super("titleId", "<title-id>");
        }

        @Nullable
        @Override
        public String parse(@Nonnull String input, @Nonnull ParseResult parseResult)
        {
            return input;
        }

        @Override
        public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered,
                            int numParametersTyped, @Nonnull SuggestionResult result)
        {
            for (String id : Title.getAssetMap().getAssetMap().keySet())
            {
                result.suggest(id);
            }
        }
    }
}
