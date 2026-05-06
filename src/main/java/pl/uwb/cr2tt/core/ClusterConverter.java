package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;


public class ClusterConverter {
    public void convertCluster(Cluster cluster, ConversionMode mode, Model outGraph) {
        switch (mode) {
            case REIFIED_TRIPLE_EXPANDED:
                Statement baseStatement = outGraph.createStatement(
                        cluster.getSubject(),
                        cluster.getPredicate(),
                        cluster.getObject()
                );

                break;

            case REIFIED_TRIPLE:
                break;

            case REIFIED_TRIPLE_EXPLICIT:
                break;

            case ANNOTATED_TRIPLE:
                break;

            case ANNOTATED_TRIPLE_EXPLICIT:
                break;

            case ANNOTATED_TRIPLE_EXPANDED:
                break;

            default:
                throw new IllegalArgumentException("Unsupported conversion mode: " + mode);
        }
    }
}
