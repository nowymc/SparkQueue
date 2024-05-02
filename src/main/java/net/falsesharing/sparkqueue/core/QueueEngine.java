package net.falsesharing.sparkqueue.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public final class QueueEngine {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Map<String, PriorityBlockingQueue<QueuedPlayer>> queues = new ConcurrentHashMap<>();
    private final Map<UUID, String> activePlayerQueues = new ConcurrentHashMap<>();
    private ScheduledExecutorService worker;

    public QueueEngine(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    public void start() {
        this.worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SparkQueue-DispatchThread");
            t.setDaemon(true);
            return t;
        });
        this.worker.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized boolean enqueue(Player player, String serverName, int weight) {
        Optional<RegisteredServer> optServer = proxy.getServer(serverName);
        if (optServer.isEmpty()) {
            player.sendMessage(Component.text("Server " + serverName + " does not exist.", NamedTextColor.RED));
            return false;
        }

        dequeue(player.getUniqueId());

        QueuedPlayer qp = new QueuedPlayer(player, weight);
        queues.computeIfAbsent(serverName.toLowerCase(), k -> new PriorityBlockingQueue<>()).offer(qp);
        activePlayerQueues.put(player.getUniqueId(), serverName.toLowerCase());

        player.sendMessage(Component.text("Joined queue for " + serverName + " (Priority weight: " + weight + ")", NamedTextColor.GREEN));
        return true;
    }

    public synchronized boolean dequeue(UUID uuid) {
        String s = activePlayerQueues.remove(uuid);
        if (s != null) {
            PriorityBlockingQueue<QueuedPlayer> q = queues.get(s);
            if (q != null) {
                q.removeIf(p -> p.uuid().equals(uuid));
                return true;
            }
        }
        return false;
    }

    public int getPosition(UUID uuid) {
        String s = activePlayerQueues.get(uuid);
        if (s == null) return -1;
        PriorityBlockingQueue<QueuedPlayer> q = queues.get(s);
        if (q == null) return -1;

        List<QueuedPlayer> list = new ArrayList<>(q);
        Collections.sort(list);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    private void tick() {
        for (Map.Entry<String, PriorityBlockingQueue<QueuedPlayer>> entry : queues.entrySet()) {
            String serverName = entry.getKey();
            PriorityBlockingQueue<QueuedPlayer> queue = entry.getValue();

            if (queue.isEmpty()) continue;

            Optional<RegisteredServer> optServer = proxy.getServer(serverName);
            if (optServer.isEmpty()) continue;
            RegisteredServer target = optServer.get();

            target.ping().thenAccept(ping -> {
                int max = ping.asBuilder().getMaximumPlayers().orElse(100);
                int online = ping.asBuilder().getOnlinePlayers().orElse(0);

                if (online < max) {
                    QueuedPlayer next = queue.poll();
                    if (next != null) {
                        activePlayerQueues.remove(next.uuid());
                        Optional<Player> p = proxy.getPlayer(next.uuid());
                        p.ifPresent(player -> {
                            player.sendMessage(Component.text("Connecting to " + serverName + "...", NamedTextColor.GREEN));
                            player.createConnectionRequest(target).fireAndForget();
                        });
                    }
                }
            }).exceptionally(ex -> null);
        }
    }

    public void shutdown() {
        if (worker != null) {
            worker.shutdownNow();
        }
        queues.clear();
        activePlayerQueues.clear();
    }
}
