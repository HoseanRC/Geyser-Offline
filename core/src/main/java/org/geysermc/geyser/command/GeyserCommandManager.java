/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.command;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandExecutor;
import org.geysermc.geyser.api.command.CommandManager;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.command.defaults.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
public abstract class GeyserCommandManager extends CommandManager {

    @Getter
    private final Map<String, Command> commands = new Object2ObjectOpenHashMap<>(12);
    private final Map<String, Command> extensionCommands = new Object2ObjectOpenHashMap<>(0);

    private final GeyserImpl geyser;

    public void init() {
        registerBuiltInCommand(new HelpCommand(geyser, "help", "geyser.commands.help.desc", "geyser.command.help", "geyser", commands));
        registerBuiltInCommand(new ListCommand(geyser, "list", "geyser.commands.list.desc", "geyser.command.list"));
        registerBuiltInCommand(new ReloadCommand(geyser, "reload", "geyser.commands.reload.desc", "geyser.command.reload"));
        registerBuiltInCommand(new OffhandCommand(geyser, "offhand", "geyser.commands.offhand.desc", "geyser.command.offhand"));
        registerBuiltInCommand(new DumpCommand(geyser, "dump", "geyser.commands.dump.desc", "geyser.command.dump"));
        registerBuiltInCommand(new VersionCommand(geyser, "version", "geyser.commands.version.desc", "geyser.command.version"));
        registerBuiltInCommand(new SettingsCommand(geyser, "settings", "geyser.commands.settings.desc", "geyser.command.settings"));
        registerBuiltInCommand(new StatisticsCommand(geyser, "statistics", "geyser.commands.statistics.desc", "geyser.command.statistics"));
        registerBuiltInCommand(new AdvancementsCommand("advancements", "geyser.commands.advancements.desc", "geyser.command.advancements"));
        registerBuiltInCommand(new AdvancedTooltipsCommand("tooltips", "geyser.commands.advancedtooltips.desc", "geyser.command.tooltips"));
        if (GeyserImpl.getInstance().getPlatformType() == PlatformType.STANDALONE) {
            registerBuiltInCommand(new StopCommand(geyser, "stop", "geyser.commands.stop.desc", "geyser.command.stop"));
        }

        register(new HelpCommand(geyser, "help", "geyser.commands.exthelp.desc", "geyser.command.exthelp", "geyserext", extensionCommands));
    }

    /**
     * For internal Geyser commands
     */
    public void registerBuiltInCommand(GeyserCommand command) {
        register(command, this.commands);
    }

    @Override
    public void register(@NonNull Command command) {
        register(command, this.extensionCommands);
    }

    private void register(Command command, Map<String, Command> commands) {
        commands.put(command.name(), command);
        geyser.getLogger().debug(GeyserLocale.getLocaleStringLog("geyser.commands.registered", command.name()));

        if (command.aliases().isEmpty()) {
            return;
        }

        for (String alias : command.aliases()) {
            commands.put(alias, command);
        }
    }

    @Override
    public void unregister(@NonNull Command command) {
        this.extensionCommands.remove(command.name(), command);

        if (command.aliases().isEmpty()) {
            return;
        }

        for (String alias : command.aliases()) {
            this.extensionCommands.remove(alias, command);
        }
    }

    @NotNull
    @Override
    public Map<String, Command> commands() {
        return Collections.unmodifiableMap(this.extensionCommands);
    }

    public boolean runCommand(GeyserCommandSource sender, String command) {
        boolean extensionCommand = command.startsWith("geyserext ");
        if (!command.startsWith("geyser ") && !extensionCommand) {
            return false;
        }

        command = command.trim().replace(extensionCommand ? "geyserext " : "geyser ", "");
        String label;
        String[] args;

        if (!command.contains(" ")) {
            label = command.toLowerCase(Locale.ROOT);
            args = new String[0];
        } else {
            label = command.substring(0, command.indexOf(" ")).toLowerCase(Locale.ROOT);
            String argLine = command.substring(command.indexOf(" ") + 1);
            args = argLine.contains(" ") ? argLine.split(" ") : new String[] { argLine };
        }

        Command cmd = (extensionCommand ? this.extensionCommands : this.commands).get(label);
        if (cmd == null) {
            geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.commands.invalid"));
            return false;
        }

        if (cmd instanceof GeyserCommand) {
            if (sender instanceof GeyserSession) {
                ((GeyserCommand) cmd).execute((GeyserSession) sender, sender, args);
            } else {
                if (!cmd.isBedrockOnly()) {
                    ((GeyserCommand) cmd).execute(null, sender, args);
                } else {
                    geyser.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.command.bedrock_only"));
                }
            }
        }

        return true;
    }

    /**
     * Returns the description of the given command
     *
     * @param command Command to get the description for
     * @return Command description
     */
    public abstract String description(String command);

    @RequiredArgsConstructor
    public static class CommandBuilder<T extends CommandSource> implements Command.Builder<T> {
        private final Class<T> sourceType;
        private String name;
        private String description = "";
        private String permission = "";
        private List<String> aliases;
        private boolean suggestedOpOnly = false;
        private boolean executableOnConsole = true;
        private List<String> subCommands;
        private boolean bedrockOnly;
        private CommandExecutor<T> executor;

        public CommandBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        public CommandBuilder<T> description(String description) {
            this.description = description;
            return this;
        }

        public CommandBuilder<T> permission(String permission) {
            this.permission = permission;
            return this;
        }

        public CommandBuilder<T> aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        @Override
        public Command.Builder<T> suggestedOpOnly(boolean suggestedOpOnly) {
            this.suggestedOpOnly = suggestedOpOnly;
            return this;
        }

        public CommandBuilder<T> executableOnConsole(boolean executableOnConsole) {
            this.executableOnConsole = executableOnConsole;
            return this;
        }

        public CommandBuilder<T> subCommands(List<String> subCommands) {
            this.subCommands = subCommands;
            return this;
        }

        public CommandBuilder<T> bedrockOnly(boolean bedrockOnly) {
            this.bedrockOnly = bedrockOnly;
            return this;
        }

        public CommandBuilder<T> executor(CommandExecutor<T> executor) {
            this.executor = executor;
            return this;
        }

        public GeyserCommand build() {
            if (this.name == null || this.name.isBlank()) {
                throw new IllegalArgumentException("Command cannot be null or blank!");
            }

            return new GeyserCommand(this.name, this.description, this.permission) {

                @SuppressWarnings("unchecked")
                @Override
                public void execute(@Nullable GeyserSession session, GeyserCommandSource sender, String[] args) {
                    Class<T> sourceType = CommandBuilder.this.sourceType;
                    CommandExecutor<T> executor = CommandBuilder.this.executor;
                    if (sourceType.isInstance(session)) {
                        executor.execute((T) session, this, args);
                        return;
                    }

                    if (sourceType.isInstance(sender)) {
                        executor.execute((T) sender, this, args);
                        return;
                    }

                    GeyserImpl.getInstance().getLogger().debug("Ignoring command " + this.name + " due to no suitable sender.");
                }

                @NonNull
                @Override
                public List<String> aliases() {
                    return CommandBuilder.this.aliases == null ? Collections.emptyList() : CommandBuilder.this.aliases;
                }

                @Override
                public boolean isSuggestedOpOnly() {
                    return CommandBuilder.this.suggestedOpOnly;
                }

                @NonNull
                @Override
                public List<String> subCommands() {
                    return CommandBuilder.this.subCommands == null ? Collections.emptyList() : CommandBuilder.this.subCommands;
                }

                @Override
                public boolean isBedrockOnly() {
                    return CommandBuilder.this.bedrockOnly;
                }

                @Override
                public boolean isExecutableOnConsole() {
                    return CommandBuilder.this.executableOnConsole;
                }
            };
        }
    }
}
