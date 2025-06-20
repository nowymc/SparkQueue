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

// patch #2 [2024-05-30 15:41:22 +0300]

// patch #3 [2024-08-06 18:02:47 +0300]

// patch #4 [2024-08-15 15:07:18 +0300]

// patch #5 [2024-08-25 15:26:14 +0300]

// patch #6 [2024-10-07 15:38:28 +0300]

// patch #7 [2024-10-22 15:56:19 +0300]

// patch #8 [2024-12-02 11:07:45 +0300]

// patch #9 [2024-12-15 20:10:05 +0300]

// patch #10 [2025-02-08 13:07:12 +0300]

// patch #11 [2025-03-06 13:57:08 +0300]

// patch #12 [2025-04-04 21:43:54 +0300]

// patch #13 [2025-04-12 20:33:59 +0300]

// patch #14 [2025-05-29 11:18:24 +0300]

// patch #15 [2025-06-20 16:54:28 +0300]
