package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;


public class ClusterConverter {
    public Model convertCluster(Cluster cluster, ConversionMode mode) {
        Model outModel = ModelFactory.createDefaultModel();

        Statement baseStmt;
        StatementTerm tripleTerm;

        switch (mode) {
            case REIFIED_TRIPLE_EXPANDED, REIFIED_TRIPLE:
                baseStmt = outModel.createStatement(
                        cluster.getSubject(),
                        cluster.getPredicate(),
                        cluster.getObject()
                );

                tripleTerm = outModel.createStatementTerm(baseStmt);

                outModel.add(cluster.getReifier(), RDF.reifies, tripleTerm);

                cluster.getMetadata().forEach(outModel::add);
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
