package pcd.ass02;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import pcd.ass02.common.FSReport;

class FSReportTest {

    @Test
    void mapsBoundariesCorrectly() {
        long maxFS = 100;
        int nb = 4;

        assertEquals(0, FSReport.bandOf(0, maxFS, nb));
        assertEquals(0, FSReport.bandOf(24, maxFS, nb));
        assertEquals(1, FSReport.bandOf(25, maxFS, nb));
        assertEquals(2, FSReport.bandOf(50, maxFS, nb));
        assertEquals(3, FSReport.bandOf(75, maxFS, nb));
        assertEquals(3, FSReport.bandOf(100, maxFS, nb));
        assertEquals(4, FSReport.bandOf(101, maxFS, nb));
    }

    @Test
    void mergesReports() {
        FSReport a = new FSReport(2, List.of(1L, 0L, 1L));
        FSReport b = new FSReport(3, List.of(0L, 2L, 1L));

        assertEquals(new FSReport(5, List.of(1L, 2L, 2L)), a.merge(b));
    }
}
