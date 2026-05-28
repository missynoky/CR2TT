package pl.uwb.cr2tt.old.result;

import pl.uwb.cr2tt.old.ClusterOld;

import java.util.List;

public class SortResultOld {
    private final List<ClusterOld> sortedClusterOlds;
    private final List<ClusterOld> cycles;

    public SortResultOld(List<ClusterOld> sortedClusterOlds, List<ClusterOld> cycles) {
        this.sortedClusterOlds = sortedClusterOlds;
        this.cycles = cycles;
    }

    public List<ClusterOld> getSortedClusters() {
        return sortedClusterOlds;
    }

    public List<ClusterOld> getCycles() {
        return cycles;
    }
}
