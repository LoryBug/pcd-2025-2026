# Assignment 04 - Distributed Programming

## Problem Analysis

This assignment moves from local concurrency to distributed programming. The main differences are partial failures, network latency, remote discovery, serialization, and the absence of shared memory.

Two mandatory systems were implemented:

- a distributed Smart Home Alarm based on Apache Pekko Cluster;
- a distributed Tic-Tac-Toe game based on Java RMI.
- a distributed critical-section middleware based on RabbitMQ.

## Exercise 1 - Distributed Smart Home Alarm

### Architecture

Package: `pcd.ass04.alarm`

The system is designed as a Pekko Cluster application. Different nodes can host different roles:

```text
alarm-cluster
  node 25520 role=control  -> ControlPanelEntity
  node 25521 role=keypad   -> DistributedKeypadActor
  node 25522 role=sensor   -> DistributedSensorActor
```

The actors communicate only through actor messages. The `ControlPanelEntity` registers itself through Pekko typed `Receptionist`; distributed keypad and sensor actors subscribe to the same service key and send commands/events to the discovered control panel actor reference.

Messages implement `JsonSerializable` and `application.conf` binds that marker to Pekko Jackson JSON serialization.

### State Machine And Safe Recovery

The alarm state machine supports:

- `RECOVERY`;
- `DISARMED`;
- `EXIT_DELAY`;
- `ARMED`;
- `ENTRY_DELAY`;
- `ALARM`.

Unlike the local version, the distributed control entity starts in `RECOVERY`. This is the required safe mode after restart or recreation: the entity does not assume that the home is safe (`DISARMED`) and does not assume that the system was armed. Sensor events and arm requests are ignored until a correct PIN is received through `Disarm`.

Normal transitions are:

```text
RECOVERY -- valid PIN --> DISARMED
DISARMED -- valid PIN arm --> EXIT_DELAY -- timeout --> ARMED
ARMED -- active-zone sensor --> ENTRY_DELAY -- timeout --> ALARM
ENTRY_DELAY -- valid PIN --> DISARMED
ALARM -- valid PIN --> DISARMED
```

Zone-based partial arming is supported. Sensor events from inactive zones are ignored.

### Cluster Configuration

The cluster configuration is in `src/main/resources/application.conf`.

It enables:

- `provider = cluster`;
- Artery remoting;
- three localhost seed nodes on ports `25520`, `25521`, `25522`;
- Split Brain Resolver with `keep-majority`.

### Running A Local Three-Node Demo

Run each command in a different terminal from `assignments/assignment-04`:

```bash
.\mvnw.cmd exec:java "-Dexec.args=control 25520"
.\mvnw.cmd exec:java "-Dexec.args=keypad 25521 demo"
.\mvnw.cmd exec:java "-Dexec.args=sensor 25522 front-door perimeter DOOR demo"
```

In demo mode, the keypad sends a valid PIN to leave recovery and then arms the perimeter zone. The sensor sends a trigger after the system has been armed.

## Exercise 2 - Distributed Tic-Tac-Toe With Java RMI

### Architecture

Package: `pcd.ass04.ttt`

The RMI system is based on three remote interfaces:

- `TicTacToeHub`: factory/discovery object registered in the RMI registry;
- `TicTacToeGame`: remote object representing a single match;
- `TicTacToeListener`: client callback for join, move, and game-over notifications.

The server stores game objects in `TicTacToeHubImpl`. Each game is represented by `TicTacToeGameImpl` and exported as a remote object before being returned to clients.

### Concurrency Discipline

Game state is centralized on the server. Methods that read or mutate the board are synchronized, so two remote clients cannot apply conflicting moves concurrently.

Callbacks are stored in a `CopyOnWriteArrayList`. If a callback throws `RemoteException`, it is removed, treating the client as disconnected.

### Game Rules

The game supports:

- named game creation;
- joining an existing game;
- X/O assignment;
- turn validation;
- occupied-cell validation;
- win and draw detection;
- immutable serializable snapshots returned to clients.

### Running The RMI Server And Client

Start the server:

```bash
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.ttt.TicTacToeServer" "-Dexec.args=1099"
```

Create a game as Alice:

```bash
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.ttt.TicTacToeClient" "-Dexec.args=localhost 1099 create game1 Alice"
```

Join as Bob:

```bash
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.ttt.TicTacToeClient" "-Dexec.args=localhost 1099 join game1 Bob"
```

The client also accepts optional row/column pairs after the player name to submit moves.

## Exercise 3 - Distributed Critical Sections With RabbitMQ

### Architecture

Package: `pcd.ass04.cs`

The middleware exposes a high-level `DistributedLock` abstraction. A process only needs a lock name and its own process id; it does not know the identity or number of the other processes.

RabbitMQ is used as the message-oriented middleware. For each logical lock, two queues are created:

- `pcd.ass04.cs.<lock>.request` for acquire requests;
- `pcd.ass04.cs.<lock>.release` for release notifications.

Each process also creates an exclusive reply queue. Requests include `replyTo` and `correlationId`, so the coordinator can grant the lock to the correct process.

```text
Process P1 -- acquire --> request queue
Process P2 -- acquire --> request queue
Coordinator -- grant --> process reply queue
Process P1 -- release --> release queue
```

### Coordination Algorithm

The coordinator keeps the current holder and a FIFO queue of pending requests:

```text
on request:
  if no current holder -> grant immediately
  else enqueue request

on release:
  if queue empty -> lock becomes free
  else dequeue first waiting process and grant it
```

This gives mutual exclusion and FIFO fairness with respect to the order in which the coordinator consumes RabbitMQ request messages.

### Running RabbitMQ And Demo

Start RabbitMQ:

```bash
docker compose up -d rabbitmq
```

Run a one-JVM demo with one coordinator and three logical processes:

```bash
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.cs.CriticalSectionDemo"
```

Observed output:

```text
p2:enter,p2:exit,p3:enter,p3:exit,p1:enter,p1:exit
```

The exact order depends on message arrival order, but entries and exits are serialized: no two processes are in the critical section at the same time.

Separate-process execution is also supported:

```bash
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.cs.CriticalSectionCoordinatorMain" "-Dexec.args=home-lock"
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.cs.CriticalSectionProcessMain" "-Dexec.args=p1 home-lock 1000"
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass04.cs.CriticalSectionProcessMain" "-Dexec.args=p2 home-lock 1000"
```

## Verification

Tests cover:

- control panel safe recovery;
- active-zone sensor triggering entry delay and alarm;
- inactive-zone sensor filtering;
- RMI hub game creation and join;
- legal turn validation and winner detection.
- RabbitMQ coordinator FIFO state;
- RabbitMQ integration with two competing lock clients.

Command:

```bash
.\mvnw.cmd test
```

Result:

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Packaging:

```bash
.\mvnw.cmd package
```

Result: `BUILD SUCCESS`.
