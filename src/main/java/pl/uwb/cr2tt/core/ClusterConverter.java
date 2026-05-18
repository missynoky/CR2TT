package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;


public class ClusterConverter {
    public Model convertCluster(Cluster cluster, ConversionMode mode) {
        Model outModel = ModelFactory.createDefaultModel();

        Statement baseStmt;

        switch (mode) {
            case REIFIED_TRIPLE_EXPANDED:
                baseStmt = outModel.createStatement(
                        cluster.getSubject(),
                        cluster.getPredicate(),
                        cluster.getObject()
                );

                outModel.createReifier(cluster.getReifier(), baseStmt);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outModel.add(metaStmt);
                }
                break;

            case REIFIED_TRIPLE:
                baseStmt = outModel.createStatement(
                        cluster.getSubject(),
                        cluster.getPredicate(),
                        cluster.getObject()
                );

                Resource anonReifier = outModel.createReifier(baseStmt);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outModel.add(
                            outModel.createStatement(
                                    anonReifier,
                                    metaStmt.getPredicate(),
                                    metaStmt.getObject()
                            )
                    );
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
        return outModel;
    }
}
