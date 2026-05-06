package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.result.ExtractionResult;
import pl.uwb.cr2tt.model.result.SortResult;
import pl.uwb.cr2tt.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class ClusterExtractor {

    public List<Cluster> findValidClusters(Model inGraph) {
        Logger.info("starting extraction of classic reification clusters");
        // Clusters_raw <- Nodes in G_in with any rdf:{subject, predicate, object, Statement}
        Set<Resource> rawClusters = new HashSet<>();

        inGraph.listSubjectsWithProperty(RDF.subject).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.predicate).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.object).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.type, RDF.Statement).forEachRemaining(rawClusters::add);

        Logger.info("found " + rawClusters.size() + " potential reification nodes.");

        // Clusters_valid <- ∅
        List<Cluster> validClusters = new ArrayList<>();

        // foreach C in Clusters_raw do
        for (Resource c : rawClusters) {

            List<Statement> subjectStmts = c.listProperties(RDF.subject).toList();
            List<Statement> predicateStmts = c.listProperties(RDF.predicate).toList();
            List<Statement> objectStmts = c.listProperties(RDF.object).toList();
            List<Statement> typeStmts = inGraph.listStatements(c, RDF.type, RDF.Statement).toList();

            if (subjectStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:subject (found " + subjectStmts.size() + ")");
                continue;
            }

            if (predicateStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:predicate (found " + predicateStmts.size() + ")");
                continue;
            }

            if (objectStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:object (found " + objectStmts.size() + ")");
                continue;
            }

            if (typeStmts.size() > 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Max one optional rdf:type rdf:Statement (found " + typeStmts.size() + ")");
                continue;
            }

            RDFNode sNode = subjectStmts.getFirst().getObject();
            RDFNode pNode = predicateStmts.getFirst().getObject();
            RDFNode oNode = objectStmts.getFirst().getObject();

            if (!sNode.isResource()) {
                Logger.warn("node " + c.getLocalName() + " skipped: Invalid subject type (must be IRI or blank node)");
                continue;
            }

            if (!pNode.isURIResource()) {
                Logger.warn("node " + c.getLocalName() + " skipped: Invalid predicate type (must be IRI)");
                continue;
            }

            if(!oNode.isResource() && !oNode.isLiteral()) {
                Logger.warn("node " + c.getLocalName() + " skipped: Invalid object type (must be IRI or blank node or literal");
                continue;
            }

            Set<Statement> metadata = new HashSet<>();

            c.listProperties().forEachRemaining(stmt -> {
                Property p = stmt.getPredicate();

                if (p.equals(RDF.subject) || p.equals(RDF.predicate) || p.equals(RDF.object)) {
                    return;
                }

                if (p.equals(RDF.type) && stmt.getObject().equals(RDF.Statement)) {
                    return;
                }

                metadata.add(stmt);
            });

            Cluster cluster = new Cluster (
                c,
                sNode.asResource(),
                pNode.as(Property.class),
                oNode,
                metadata
            );

            validClusters.add(cluster);

        }
        Logger.info("extraction complete. Successfully found " + validClusters.size() + " valid classic reification clusters.");
        return validClusters;
    }

    public SortResult topologicalSort(List<Cluster> validClusters) {
        Logger.info("starting bottom-up topological sort for " + validClusters.size() + " clusters.");

        List<Cluster> sortedClusters = new ArrayList<>();
        List<Cluster> cycles = new ArrayList<>();

        Map<Resource, Cluster> clusterMap = new HashMap<>();

        for (Cluster c : validClusters) {
            clusterMap.put(c.getReifier(), c);
        }

        Logger.info("built index map for " + clusterMap.size() + " clusters. Starting dependency analysis.");

        Map<Cluster, Integer> states = new HashMap<>();

        for (Cluster c : validClusters) {
            if (!states.containsKey(c)) {
                dfs(c, states, clusterMap, sortedClusters, cycles);
            }
        }

        if (!cycles.isEmpty()) {
            Logger.warn("detected " + cycles.size() + " clusters involved in cycles. They will be skipped.");
        }
        Logger.info("topological sort complete. Successfully sorted " + sortedClusters.size() + " clusters.");

        return new SortResult(sortedClusters, cycles);
    }

    private boolean dfs(Cluster current, Map<Cluster, Integer> states,
                        Map<Resource, Cluster> clusterMap,
                        List<Cluster> sortedClusters, List<Cluster> cycles) {

        int state = states.getOrDefault(current, 0);

        if (state == 1) {
            if (!cycles.contains(current)) cycles.add(current);
            return false;
        }

        if (state == 2) {
            return true;
        }

        states.put(current, 1);

        boolean hasCycle = false;

        if (clusterMap.containsKey(current.getSubject())) {
            if (!dfs(clusterMap.get(current.getSubject()), states, clusterMap, sortedClusters, cycles))
                hasCycle = true;
        }

        if (current.getObject().isResource() && clusterMap.containsKey(current.getObject().asResource())) {
            if (!dfs(clusterMap.get(current.getObject().asResource()), states, clusterMap, sortedClusters, cycles))
                hasCycle = true;
        }

        for (Statement stmt : current.getMetadata()) {
            if (stmt.getObject().isResource() && clusterMap.containsKey(stmt.getObject().asResource())) {
                if (!dfs(clusterMap.get(stmt.getObject().asResource()), states, clusterMap, sortedClusters, cycles))
                    hasCycle = true;
            }
        }

        states.put(current, 2);

        if (hasCycle) {
            if (!cycles.contains(current)) cycles.add(current);
            return false;
        } else {
            sortedClusters.add(current);
            return true;
        }
    }

    public ExtractionResult extractClusters(Model inGraph) {
        Logger.info("starting phase 1: extraction and sorting");

        List<Cluster> validCluster = findValidClusters(inGraph);

        SortResult sortResult = topologicalSort(validCluster);
        List<Cluster> sortedCluster = sortResult.getSortedClusters();

        Logger.info("phase 1 complete.");

        return new ExtractionResult(sortedCluster, inGraph);
    }

}