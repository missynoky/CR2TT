package pl.uwb.cr2tt.core;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
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
                outGraph.createReifier(r, baseTriple);

                if (!cluster.getMetadata().isEmpty()) {
                    for (Statement metaStmt : cluster.getMetadata()) {
                        outGraph.add(metaStmt);
                    }
                }
                break;

            case ANNOTATED_TRIPLE:
                outGraph.add(baseTriple);

                Resource anonReifier = outGraph.createReifier(baseTriple);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(anonReifier, metaStmt.getPredicate(), metaStmt.getObject());
                }
                break;

            case ANNOTATED_TRIPLE_EXPLICIT:
                outGraph.add(baseTriple);

                outGraph.createReifier(r, baseTriple);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(metaStmt);
                }
                break;

            case ANNOTATED_TRIPLE_EXPANDED:
                outGraph.add(baseTriple);

                outGraph.createReifier(r, baseTriple);

                if (!cluster.getMetadata().isEmpty()) {
                    for (Statement metaStmt : cluster.getMetadata()) {
                        outGraph.add(metaStmt);
                    }
                }
                break;

            case DIRECT_TRIPLE:
                Node tripleNode = NodeFactory.createTripleTerm(
                        s.asNode(),
                        p.asNode(),
                        o.asNode()
                );

                Resource tripleTermSubj = outGraph.asRDFNode(tripleNode).asResource();

                outGraph.add(tripleTermSubj, RDF.type, RDF.Statement);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(tripleTermSubj, metaStmt.getPredicate(), metaStmt.getObject());
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported conversion mode: " + mode);
        }
    }
}
