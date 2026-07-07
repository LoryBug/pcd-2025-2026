package pcd.ass02;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pcd.ass02.common.FSReport;
import pcd.ass02.eventloop.EventLoopFSStatLib;
import pcd.ass02.reactive.ReactiveFSStatLib;
import pcd.ass02.virtualthreads.VirtualThreadFSStatLib;

class FSStatLibTest {

    @TempDir
    Path tempDir;

    @Test
    void allVersionsComputeTheSameReport() throws IOException {
        createFile(tempDir.resolve("a.bin"), 0);
        createFile(tempDir.resolve("b.bin"), 10);
        createFile(tempDir.resolve("c.bin"), 25);
        createFile(tempDir.resolve("d.bin"), 50);
        createFile(tempDir.resolve("e.bin"), 100);
        Path sub = Files.createDirectory(tempDir.resolve("sub"));
        createFile(sub.resolve("f.bin"), 101);

        FSReport expected = new FSReport(6, List.of(2L, 1L, 1L, 1L, 1L));

        try (EventLoopFSStatLib eventLoop = new EventLoopFSStatLib()) {
            assertEquals(expected, eventLoop.getFSReport(tempDir, 100, 4).join());
        }
        try (ReactiveFSStatLib reactive = new ReactiveFSStatLib()) {
            assertEquals(expected, reactive.getFSReport(tempDir, 100, 4).blockingGet());
        }
        assertEquals(expected, new VirtualThreadFSStatLib().getFSReport(tempDir, 100, 4).join());
    }

    private void createFile(Path path, int size) throws IOException {
        Files.write(path, new byte[size]);
    }
}
