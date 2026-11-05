package com.example.auravisuals.command;

import com.example.auravisuals.client.gui.FriendManagerScreen;
import com.example.auravisuals.friend.FriendManager;
import net.minecraft.client.MinecraftClient;

public class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "Manage friends", ".friend [help | list | clear | add | remove]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new FriendManagerScreen()));
        } else {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "help":
                    CommandManager.addChatMessage("§dFriend System Help:");
                    CommandManager.addChatMessage("§7.friend §f- Open Friend GUI");
                    CommandManager.addChatMessage("§7.friend help §f- Show this help message");
                    CommandManager.addChatMessage("§7.friend list §f- List all friends");
                    CommandManager.addChatMessage("§7.friend add <name> §f- Add a friend");
                    CommandManager.addChatMessage("§7.friend remove <name> §f- Remove a friend");
                    CommandManager.addChatMessage("§7.friend clear §f- Remove all friends");
                    break;
                case "list":
                    if (FriendManager.INSTANCE.getFriends().isEmpty()) {
                        CommandManager.addChatMessage("You have no friends added.");
                    } else {
                        CommandManager.addChatMessage("§dFriends list (" + FriendManager.INSTANCE.getFriends().size() + "):");
                        CommandManager.addChatMessage("§7" + String.join(", ", FriendManager.INSTANCE.getFriends()));
                    }
                    break;
                case "clear":
                    FriendManager.INSTANCE.clearFriends();
                    CommandManager.addChatMessage("All friends removed.");
                    break;
                case "add":
                    if (args.length < 2) {
                        CommandManager.addChatMessage("Usage: .friend add <name>");
                        return;
                    }

                    FriendManager.INSTANCE.addFriend(args[1]);
                    CommandManager.addChatMessage("Added " + args[1] + " to friends.");
                    break;
                case "remove":
                    if (args.length < 2) {
                        CommandManager.addChatMessage("Usage: .friend remove <name>");
                        return;
                    }

                    FriendManager.INSTANCE.removeFriend(args[1]);
                    CommandManager.addChatMessage("Removed " + args[1] + " from friends.");
                    break;
                default:
                    CommandManager.addChatMessage("Unknown subcommand. Type .friend help for list.");
            }
        }
    }
}
