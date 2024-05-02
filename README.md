# SparkQueue

High-performance, multi-threaded priority queue and player matchmaking plugin for **Velocity** Minecraft proxy networks.

## Overview
Rebranded and modern Java 21 re-architecture of the open-source `ajQueue` concept, engineered specifically for high-concurrency Minecraft proxy setups and Folia backend servers.

### Features
* **Priority Matching:** Weighted multi-tier queues (`sparkqueue.priority.admin`, `sparkqueue.priority.donor`) with atomic FIFO ordering for equal weights.
* **Non-Blocking Health Pinging:** Prevents player drops by polling backend server capacity asynchronously.
* **Zero Main-Thread Overhead:** Asynchronous scheduled dispatcher thread prevents proxy latency spikes during peak player rushes.
* **Seamless Transfer:** Compatible with both Paper and Folia backend servers.

## Commands & Permissions
* `/queue <server>` - Enqueue for target backend server.
* `/queue leave` - Exit queue.
* `/sparkqueue` - Show current queue position and status.
* `sparkqueue.priority.admin` - Top priority bypass tier (weight: 100).
* `sparkqueue.priority.donor` - Donor priority tier (weight: 50).

## Build
```bash
mvn clean package
```
