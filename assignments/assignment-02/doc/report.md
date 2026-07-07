# Assignment 02 - FSStat

## Problem Analysis

The assignment requires an asynchronous library method `getFSReport(D, MaxFS, NB)` that recursively scans a directory tree and computes:

- the total number of regular files contained in the tree;
- a distribution of file sizes over `NB + 1` bands;
- the first `NB` bands partition the closed interval `[0, MaxFS]`;
- the last band counts only files whose size is strictly greater than `MaxFS`.

The main concurrent aspects are directory traversal, file metadata access, result aggregation, and error isolation. Directory listing and file size queries are I/O-bound operations, so they can benefit from concurrency when several subtrees or files are inspected independently. Aggregation must avoid shared mutable counters, otherwise concurrent updates would require synchronization and would make the three required programming styles less clear.

The implementation uses an immutable `FSReport` record. A single file produces a one-file report, and larger reports are obtained by functional merging. This keeps the aggregation deterministic and thread-safe in all versions.

Unreadable files or directories are treated as empty contributions. This prevents one inaccessible path from failing the whole asynchronous computation.

## Adopted Strategy

The project contains three independent implementations, one for each programming discipline requested by the assignment.

### Event-Loop / Asynchronous Version

Class: `pcd.ass02.eventloop.EventLoopFSStatLib`

This version exposes `CompletableFuture<FSReport>`. Directory listing and file size operations are submitted to an executor, while composition is expressed through `thenCompose`, `allOf`, and `thenApply`. The caller receives a future immediately and is not blocked by the traversal.

Each directory asynchronously lists its children. Subdirectories recursively produce futures, regular files produce futures containing one-file reports, and `mergeAll` joins the completed child reports into one immutable result.

### Reactive Version

Class: `pcd.ass02.reactive.ReactiveFSStatLib`

This version exposes `Single<FSReport>` and uses RxJava `Flowable` streams. The recursive walk is represented as a stream of paths. Regular files are mapped to one-file reports and the final report is produced with a reactive `reduce`.

The Rx version uses a custom `Scheduler` backed by a `ScheduledExecutorService`. This keeps the scheduler lifecycle explicit and avoids leaking RxJava global scheduler threads when the example program is executed through Maven.

### Virtual-Thread Version

Class: `pcd.ass02.virtualthreads.VirtualThreadFSStatLib`

This version exposes `CompletableFuture<FSReport>` and starts the computation from a virtual thread. Recursive subdirectories are submitted to a virtual-thread-per-task executor. The code is intentionally written in a direct blocking style inside virtual threads: each subtree can wait for child futures without occupying platform threads.

Regular files are processed locally in the current virtual thread, while subdirectories are explored concurrently. The executor is scoped to one report computation and closed when the computation ends.

## Public Model

Class: `pcd.ass02.common.FSReport`

`FSReport` is immutable and stores:

- `long totalFiles`;
- `List<Long> bandCounts` with size `NB + 1`.

The method `bandOf(fileSize, maxFS, nb)` maps files with `fileSize > maxFS` to the extra band. A file whose size is exactly `MaxFS` belongs to the last ordinary band, not to the extra band.

## Example Program

Class: `pcd.ass02.examples.FSStatExample`

Usage:

```bash
.\mvnw.cmd exec:java "-Dexec.args=eventloop . 1048576 8"
.\mvnw.cmd exec:java "-Dexec.args=reactive . 1048576 8"
.\mvnw.cmd exec:java "-Dexec.args=vt . 1048576 8"
```

The program prints the selected version, root directory, total number of files, band distribution, and elapsed time.

## Benchmark

Class: `pcd.ass02.benchmark.FSStatBenchmark`

The benchmark creates a temporary deterministic dataset with 1200 files distributed across nested directories. It executes one warm-up run and then 5 measured runs for each implementation. All three reports are compared for equality before the program exits.

Command:

```bash
.\mvnw.cmd exec:java "-Dexec.mainClass=pcd.ass02.benchmark.FSStatBenchmark"
```

Measured on the development machine with `MaxFS = 32768` and `NB = 8`:

| Version | Files | Over MaxFS | Avg ms | Min ms | Bands |
| --- | ---: | ---: | ---: | ---: | --- |
| eventloop | 1200 | 599 | 67.93 | 54.63 | `[146, 5, 145, 5, 145, 5, 145, 5, 599]` |
| reactive | 1200 | 599 | 116.04 | 100.96 | `[146, 5, 145, 5, 145, 5, 145, 5, 599]` |
| virtual threads | 1200 | 599 | 55.12 | 47.53 | `[146, 5, 145, 5, 145, 5, 145, 5, 599]` |

The benchmark is intentionally small and mostly validates behaviour and overhead. The virtual-thread version is fastest in this run because it expresses recursive blocking operations directly with cheap tasks. The reactive version has higher overhead on this dataset due to stream and scheduler machinery, but it keeps a clear reactive pipeline and explicit resource management.

## Testing

Tests are implemented with JUnit 5:

- `FSReportTest` checks band mapping and immutable merge semantics;
- `FSStatLibTest` builds a temporary directory tree and verifies that all three implementations return the same expected report.

Verification command:

```bash
.\mvnw.cmd test
```

Result on the development machine:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Packaging was also verified with:

```bash
.\mvnw.cmd package
```

The shaded jar is generated successfully. Maven Shade reports only the expected manifest overlap warning from bundled dependencies.
