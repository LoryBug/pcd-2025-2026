package pcd.ass02.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record FSReport(long totalFiles, List<Long> bandCounts) {

    public FSReport {
        if (totalFiles < 0) {
            throw new IllegalArgumentException("totalFiles must be non-negative");
        }
        Objects.requireNonNull(bandCounts, "bandCounts");
        if (bandCounts.isEmpty()) {
            throw new IllegalArgumentException("bandCounts must contain at least one band");
        }
        bandCounts = List.copyOf(bandCounts);
    }

    public static FSReport empty(int nb) {
        validateBands(nb);
        return new FSReport(0, Collections.nCopies(nb + 1, 0L));
    }

    public static FSReport single(long fileSize, long maxFS, int nb) {
        int band = bandOf(fileSize, maxFS, nb);
        List<Long> bands = new ArrayList<>(Collections.nCopies(nb + 1, 0L));
        bands.set(band, 1L);
        return new FSReport(1, bands);
    }

    public FSReport merge(FSReport other) {
        if (bandCounts.size() != other.bandCounts.size()) {
            throw new IllegalArgumentException("Cannot merge reports with different band counts");
        }
        List<Long> merged = new ArrayList<>(bandCounts.size());
        for (int i = 0; i < bandCounts.size(); i++) {
            merged.add(bandCounts.get(i) + other.bandCounts.get(i));
        }
        return new FSReport(totalFiles + other.totalFiles, merged);
    }

    public long overMaxCount() {
        return bandCounts.get(bandCounts.size() - 1);
    }

    public static int bandOf(long fileSize, long maxFS, int nb) {
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must be non-negative");
        }
        validate(maxFS, nb);
        if (fileSize > maxFS) {
            return nb;
        }
        double bandWidth = maxFS / (double) nb;
        return Math.min((int) (fileSize / bandWidth), nb - 1);
    }

    public static void validate(long maxFS, int nb) {
        if (maxFS <= 0) {
            throw new IllegalArgumentException("maxFS must be positive");
        }
        validateBands(nb);
    }

    private static void validateBands(int nb) {
        if (nb <= 0) {
            throw new IllegalArgumentException("nb must be positive");
        }
    }
}
