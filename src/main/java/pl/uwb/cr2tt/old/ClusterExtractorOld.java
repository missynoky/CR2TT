package pl.uwb.cr2tt.old;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.old.result.SortResultOld;
import pl.uwb.cr2tt.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class ClusterExtractorOld {

    public List<ClusterOld> findValidClusters(Model inGraph) {
        Logger.info("starting extraction of classic reification clusters");

        Set<Resource> rawClusters = new HashSet<>();

        inGraph.listSubjectsWithProperty(RDF.subject).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.predicate).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.object).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.type, RDF.Statement).forEachRemaining(rawClusters::add);

        Logger.info("found " + rawClusters.size() + " potential reification nodes.");

        List<ClusterOld> validClusterOlds = new ArrayList<>();

        for (Resource c : rawClusters) {

            List<Statement> subjectStmts = c.listProperties(RDF.subject).toList();
            List<Statement> predicateStmts = c.listProperties(RDF.predicate).toList();
            List<Statement> objectStmts = c.listProperties(RDF.object).toList();
            List<Statement> typeStmts = inGraph.listStatements(c, RDF.type, RDF.Statement).toList();

            if (subjectStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:subject");
                continue;
            }

            if (predicateStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:predicate");
                continue;
            }

            if (objectStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:object");
                continue;
            }

            if (typeStmts.size() > 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Max one optional rdf:type rdf:Statement");
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

            ClusterOld clusterOld = new ClusterOld(
                c,
                sNode.asResource(),
                pNode.as(Property.class),
                oNode,
                metadata
            );

            validClusterOlds.add(clusterOld);

        }
        Logger.info("extraction complete. Successfully found " + validClusterOlds.size() + " valid classic reification clusters.");
        return validClusterOlds;
    }

    public SortResultOld topologicalSort(List<ClusterOld> validClusterOlds) {
        Logger.info("starting bottom-up topological sort for " + validClusterOlds.size() + " clusters.");

        List<ClusterOld> sortedClusterOlds = new ArrayList<>();
        List<ClusterOld> cycles = new ArrayList<>();

        Map<Resource, ClusterOld> clusterMap = new HashMap<>();

        for (ClusterOld c : validClusterOlds) {
            clusterMap.put(c.getReifier(), c);
        }

        Logger.info("built index map for " + clusterMap.size() + " clusters. Starting dependency analysis.");

        Map<ClusterOld, Integer> states = new HashMap<>();

        for (ClusterOld c : validClusterOlds) {
            if (!states.containsKey(c)) {
                dfs(c, states, clusterMap, sortedClusterOlds, cycles);
            }
        }

        if (!cycles.isEmpty()) {
            Logger.warn("detected " + cycles.size() + " clusters involved in cycles. They will be skipped.");
        }
        Logger.info("topological sort complete. Successfully sorted " + sortedClusterOlds.size() + " clusters.");

        return new SortResultOld(sortedClusterOlds, cycles);
    }

    private boolean dfs(ClusterOld current, Map<ClusterOld, Integer> states,
                        Map<Resource, ClusterOld> clusterMap,
                        List<ClusterOld> sortedClusterOlds, List<ClusterOld> cycles) {

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
            if (!dfs(clusterMap.get(current.getSubject()), states, clusterMap, sortedClusterOlds, cycles))
                hasCycle = true;
        }

        if (current.getObject().isResource() && clusterMap.containsKey(current.getObject().asResource())) {
            if (!dfs(clusterMap.get(current.getObject().asResource()), states, clusterMap, sortedClusterOlds, cycles))
                hasCycle = true;
        }

        for (Statement stmt : current.getMetadata()) {
            if (stmt.getObject().isResource() && clusterMap.containsKey(stmt.getObject().asResource())) {
                if (!dfs(clusterMap.get(stmt.getObject().asResource()), states, clusterMap, sortedClusterOlds, cycles))
                    hasCycle = true;
            }
        }

        states.put(current, 2);

        if (hasCycle) {
            if (!cycles.contains(current)) cycles.add(current);
            return false;
        } else {
            sortedClusterOlds.add(current);
            return true;
        }
    }

    public List<ClusterOld> extractClusters(Model inGraph) {
        Logger.info("starting phase 1: extraction and sorting");
        List<ClusterOld> validClusterOld = findValidClusters(inGraph);
        SortResultOld sortResultOld = topologicalSort(validClusterOld);
        List<ClusterOld> sortedClusterOlds = sortResultOld.getSortedClusters();
        Logger.info("phase 1 complete.");

        return sortedClusterOlds;
    }

}