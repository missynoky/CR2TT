package pl.uwb.cr2tt.core;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.util.Map;

public class ClusterConverter {
    public void convertCluster(Cluster cluster, ConversionMode mode, Model outGraph,
                               Map<String, Resource> resolvedTerms, boolean keepStatementType) {
        Resource r = cluster.getClusterNode();
        Resource s = cluster.getSubjectNode();
        Property p = cluster.getPredicateNode();
        RDFNode o = cluster.getObjectNode();

        String rId = getNodeId(r);

        String sId = getNodeId(s);
        if (resolvedTerms.containsKey(sId)) {
            s = resolvedTerms.get(sId);
        }

        String oId = getNodeId(o);
        if (resolvedTerms.containsKey("tt_" + oId)) {
            o = resolvedTerms.get("tt_" + oId);
        } else if (resolvedTerms.containsKey(oId)) {
            o = resolvedTerms.get(oId);
        }

        Statement baseTriple = outGraph.createStatement(s, p, o);

        if (cluster.isNestedTarget()) {
            Triple rawTriple = baseTriple.asTriple();
            Node tripleNode = NodeFactory.createTripleTerm(rawTriple);
            Resource tripleTerm = outGraph.asRDFNode(tripleNode).asResource();

            resolvedTerms.put("tt_" + rId, tripleTerm);
        }
        Resource activeReifier = null;

        switch (mode) {
            case REIFIED_TRIPLE_EXPANDED:
                activeReifier = outGraph.createReifier(r, baseTriple);
                addStatementType(outGraph, r, keepStatementType);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(metaStmt);
                }
                break;

            case REIFIED_TRIPLE:
                activeReifier = outGraph.createReifier(baseTriple);
                addStatementType(outGraph, activeReifier, keepStatementType);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(activeReifier, metaStmt.getPredicate(), metaStmt.getObject());
                }
                break;

            case REIFIED_TRIPLE_EXPLICIT:
                activeReifier = outGraph.createReifier(r, baseTriple);
                addStatementType(outGraph, r, keepStatementType);

                if (!cluster.getMetadata().isEmpty()) {
                    for (Statement metaStmt : cluster.getMetadata()) {
                        outGraph.add(metaStmt);
                    }
                }
                break;

            case ANNOTATED_TRIPLE:
                outGraph.add(baseTriple);
                activeReifier = outGraph.createReifier(baseTriple);
                addStatementType(outGraph, activeReifier, keepStatementType);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(activeReifier, metaStmt.getPredicate(), metaStmt.getObject());
                }
                break;

            case ANNOTATED_TRIPLE_EXPLICIT:
                outGraph.add(baseTriple);
                activeReifier = outGraph.createReifier(r, baseTriple);
                addStatementType(outGraph, r, keepStatementType);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(metaStmt);
                }
                break;

            case ANNOTATED_TRIPLE_EXPANDED:
                outGraph.add(baseTriple);
                activeReifier = outGraph.createReifier(r, baseTriple);
                addStatementType(outGraph, r, keepStatementType);

                if (!cluster.getMetadata().isEmpty()) {
                    for (Statement metaStmt : cluster.getMetadata()) {
                        outGraph.add(metaStmt);
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported conversion mode: " + mode);
        }

        resolvedTerms.put(rId, activeReifier);
    }

    private void addStatementType(Model outGraph, Resource reifierNode, boolean keepStatementType) {
        if (keepStatementType && reifierNode != null) {
            outGraph.add(reifierNode, RDF.type, RDF.Statement);
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