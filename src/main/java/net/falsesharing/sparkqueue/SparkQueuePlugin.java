package net.falsesharing.sparkqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.falsesharing.sparkqueue.command.QueueCommand;
import net.falsesharing.sparkqueue.core.QueueEngine;
import org.slf4j.Logger;

@Plugin(
    id = "sparkqueue",
    name = "SparkQueue",
    version = "1.4.0",
    description = "Next-generation priority matchmaking queue for Velocity.",
    authors = {"FalseSharing"}
)
public final class SparkQueuePlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private QueueEngine queueEngine;

    @Inject
    public SparkQueuePlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Starting SparkQueue engine...");
        this.queueEngine = new QueueEngine(proxy, logger);
        this.queueEngine.start();

        proxy.getCommandManager().register("queue", new QueueCommand(queueEngine), "sparkqueue");
        logger.info("SparkQueue initialized with priority dispatching and health polling.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (queueEngine != null) {
            queueEngine.shutdown();
        }
        logger.info("SparkQueue shutdown complete.");
    }

    public QueueEngine getQueueEngine() {
        return queueEngine;
    }
}
