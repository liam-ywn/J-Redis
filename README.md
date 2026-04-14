# J-Redis

`J-Redis` is a Redis-inspired server written in Java as a learning project for exploring:

- the RESP wire protocol
- Redis-style command processing
- concurrent in-memory data structures
- append-only persistence and recovery
- Java 21 virtual threads for client handling

It listens on port `6379`, accepts RESP requests over TCP, and persists write commands to `appendOnlyFile.aof`.

## What It Supports

### Core pieces

- Custom RESP parser and response types
- Virtual-thread-per-client server model
- In-memory key-value storage backed by concurrent collections
- AOF logging and startup recovery
- Background `BGREWRITEAOF`
- TTL support with periodic cleanup
- Pub/Sub channel messaging
- Sorted sets backed by a custom skip list

### Data types

- Strings
- Integers via `INCR`
- Lists
- Hashes
- Sets
- Sorted sets

### Implemented commands

| Category | Commands |
| :--- | :--- |
| Generic | `PING`, `DEL`, `EXISTS`, `KEYS`, `TTL`, `EXPIRE` |
| Strings | `SET`, `GET`, `INCR` |
| Lists | `LPUSH`, `RPUSH`, `LPOP`, `LLEN`, `LRANGE` |
| Hashes | `HSET`, `HGET`, `HDEL`, `HLEN`, `HGETALL` |
| Sets | `SADD`, `SREM`, `SMEMBERS`, `SISMEMBER`, `SCARD` |
| Sorted sets | `ZADD`, `ZRANGE`, `ZREM`, `ZSCORE`, `ZCARD` |
| Pub/Sub | `SUBSCRIBE`, `UNSUBSCRIBE`, `PUBLISH` |
| Persistence | `BGREWRITEAOF` |

## Requirements

- Java 21 or newer
- `./gradlew` wrapper included in the repo

Java 21 is required because the implementation uses virtual threads and Java 21 collection APIs.

## Running The Project

### 1. Run tests

```bash
./gradlew test
```

### 2. Compile the server

```bash
./gradlew classes
```

### 3. Start the server

```bash
java -cp build/classes/java/main dev.yewintnaing.RedisServer
```

When the server starts, it will:

- replay commands from `appendOnlyFile.aof` if the file exists
- start the TTL cleaner task
- listen on port `6379`

## Trying It With `redis-cli`

If you have Redis CLI installed locally, you can connect to the server with:

```bash
redis-cli -p 6379
```

Example session:

```text
127.0.0.1:6379> SET name yewint
OK
127.0.0.1:6379> GET name
"yewint"
127.0.0.1:6379> LPUSH tasks third
(integer) 1
127.0.0.1:6379> LPUSH tasks second
(integer) 2
127.0.0.1:6379> LPUSH tasks first
(integer) 3
127.0.0.1:6379> LRANGE tasks 0 -1
1) "first"
2) "second"
3) "third"
```

Sorted set example:

```text
127.0.0.1:6379> ZADD leaders 10 alice
(integer) 1
127.0.0.1:6379> ZADD leaders 15 bob
(integer) 1
127.0.0.1:6379> ZRANGE leaders 0 -1 WITHSCORES
1) "alice"
2) "10.0"
3) "bob"
4) "15.0"
```

## Persistence

Write commands are appended to `appendOnlyFile.aof` in RESP format.

On startup, the server replays that file to rebuild in-memory state. `BGREWRITEAOF` creates a compacted version of the AOF from the current snapshot, including expiry metadata when keys still have time remaining.

## Tests In This Repo

The test suite currently covers:

- generic commands
- string and increment behavior
- hash commands
- set commands
- sorted set behavior
- pub/sub flow
- skip list behavior
- AOF logging, rewrite, and recovery

## Current Scope

This project is intentionally focused on learning and does not aim to be a full Redis replacement. For example, it currently keeps all data in memory, uses a single fixed port, and implements a focused subset of Redis commands rather than the full command surface.
