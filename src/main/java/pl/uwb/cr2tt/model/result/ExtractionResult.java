package pl.uwb.cr2tt.model.result;

import org.apache.jena.rdf.model.Model;
import pl.uwb.cr2tt.model.Cluster;

import java.util.List;

public class ExtractionResult {
    private final List<Cluster> clusters;
    private final Model gCore;

    public ExtractionResult(List<Cluster> clusters, Model gCore){
        this.clusters = clusters;
        this.gCore = gCore;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public Model getgCore() {
        return gCore;
    }
}
