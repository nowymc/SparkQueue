package net.falsesharing.sparkqueue.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.falsesharing.sparkqueue.core.QueueEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class QueueCommand implements SimpleCommand {

    private final QueueEngine engine;

    public QueueCommand(QueueEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can join queues.", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            int pos = engine.getPosition(player.getUniqueId());
            if (pos > 0) {
                player.sendMessage(Component.text("Your position in queue: " + pos, NamedTextColor.GOLD));
            } else {
                player.sendMessage(Component.text("Usage: /queue <server> or /queue leave", NamedTextColor.YELLOW));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            if (engine.dequeue(player.getUniqueId())) {
                player.sendMessage(Component.text("Left queue successfully.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("You are not in any queue.", NamedTextColor.RED));
            }
            return;
        }

        String targetServer = args[0];
        int weight = 1;
        if (player.hasPermission("sparkqueue.priority.admin")) {
            weight = 100;
        } else if (player.hasPermission("sparkqueue.priority.donor")) {
            weight = 50;
        }

        engine.enqueue(player, targetServer, weight);
    }
}
