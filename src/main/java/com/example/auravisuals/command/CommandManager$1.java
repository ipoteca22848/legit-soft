package com.example.auravisuals.command;

public class CommandManager$1 extends Command {
    CommandManager$1(CommandManager this$0, String name, String description, String syntax) {
        super(name, description, syntax);
    }

    @Override
    public void execute(String[] args) {
        new LiveCommand().execute(new String[]{"reload"});
    }
}
