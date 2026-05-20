package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

public class ClusterConverter {
    public void convertCluster(Cluster cluster, ConversionMode mode, Model outGraph) {
        Resource r = cluster.getClusterNode();
        Resource s = cluster.getSubjectNode();
        Property p = cluster.getPredicateNode();
        RDFNode o = cluster.getObjectNode();

        Statement baseTriple = outGraph.createStatement(s, p, o);

        switch (mode) {
            case REIFIED_TRIPLE_EXPANDED:
                outGraph.createReifier(r, baseTriple);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(metaStmt);
                }
                break;

            case REIFIED_TRIPLE:
                Resource anonymousReifier = outGraph.createReifier(baseTriple);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(anonymousReifier, metaStmt.getPredicate(), metaStmt.getObject());
                }
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
