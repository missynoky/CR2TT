package pl.uwb.cr2tt.model.result;

import pl.uwb.cr2tt.model.Cluster;

import java.util.List;

public class SortResult {
    private final List<Cluster> sortedClusters;
    private final List<Cluster> cycles;

    public SortResult(List<Cluster> sortedClusters, List<Cluster> cycles) {
        this.sortedClusters = sortedClusters;
        this.cycles = cycles;
    }

    public List<Cluster> getSortedClusters() {
        return sortedClusters;
    }

    public List<Cluster> getCycles() {
        return cycles;
    }
}
