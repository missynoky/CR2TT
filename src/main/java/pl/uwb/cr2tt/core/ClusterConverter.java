package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.util.Map;

public class ClusterConverter {
    public void convertCluster(Cluster cluster, ConversionMode mode, Model outGraph, Map<String, StatementTerm> resolvedTerms) {
        Resource r = cluster.getClusterNode();
        Resource s = cluster.getSubjectNode();
        Property p = cluster.getPredicateNode();
        RDFNode o = cluster.getObjectNode();

        String oId = getNodeId(o);
        if (resolvedTerms.containsKey(oId)) {
            o = resolvedTerms.get(oId);
        }

        Statement baseTriple = outGraph.createStatement(s, p, o);

        if (cluster.isNestedTarget()) {
            StatementTerm tripleTerm = outGraph.createStatementTerm(baseTriple);
            resolvedTerms.put(getNodeId(r), tripleTerm);
        }

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

            default:
                throw new IllegalArgumentException("Unsupported conversion mode: " + mode);
        }
    }
    private String getNodeId(RDFNode node) {
        if (node == null) return "";
        if (node.isAnon()) {
            return node.asResource().getId().toString();
        } else if (node.isResource()) {
            return node.asResource().getURI();
        }
        return "";
    }
}
