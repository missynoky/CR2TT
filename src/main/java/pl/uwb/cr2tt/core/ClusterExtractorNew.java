package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.utils.Logger;

import java.util.*;
import java.util.function.Consumer;

public class ClusterExtractorNew {
    private final Set<String> processedClusters = new HashSet<>();
    private final Set<String> emittedClusters = new HashSet<>();
    private final Map<String, List<Cluster>> waitingRoom = new HashMap<>();

    private long validClusterCount = 0;

    public int extractAndProcess(Model inGraph, Consumer<Cluster> clusterProcessor) {
        Logger.info("starting extraction of clusters.");

        long rowCount = 0;

        StmtIterator candidates = inGraph.listStatements(null, RDF.subject, (RDFNode) null);

        try {
            while (candidates.hasNext()) {
                Statement stmt = candidates.next();
                Resource cNode = stmt.getSubject();

                rowCount++;
                if (rowCount % 50000 == 0) {
                    Logger.info("scanned potential cluster nodes: " + rowCount);
                }

                String cNodeId = cNode.isAnon() ? cNode.getId().toString() : cNode.getURI();
                if (processedClusters.contains(cNodeId)) {
                    continue;
                }

                processedClusters.add(cNodeId);

                extractAndValidateSingleCluster(cNode, inGraph, clusterProcessor);
            }
        } finally {
            candidates.close();
        }

        Logger.info("finished reading stream. Valid extracted clusters: " + validClusterCount);

        int cyclicCount = 0;
        if (!waitingRoom.isEmpty()) {
            cyclicCount = waitingRoom.values().stream().mapToInt(List::size).sum();
            Logger.error("cyclic reification omitted due to loop detection. Skipped clusters: " + cyclicCount);
        }

        return cyclicCount;
    }

    private void extractAndValidateSingleCluster(Resource cNode, Model inGraph, Consumer<Cluster> clusterProcessor) {
        StmtIterator props = inGraph.listStatements(cNode, null, (RDFNode) null);

        int sCount = 0, pCount = 0, oCount = 0, stmtCount = 0;
        Resource s = null;
        Property p = null;
        RDFNode o = null;
        Set<Statement> metadata = new HashSet<>();

        try {
            while (props.hasNext()) {
                Statement pStmt = props.next();
                Property pred = pStmt.getPredicate();
                RDFNode obj = pStmt.getObject();

                if (pred.equals(RDF.subject)) {
                    sCount++;
                    if (obj.isResource()) s = obj.asResource();
                } else if (pred.equals(RDF.predicate)) {
                    pCount++;
                    if (obj.isResource() && obj.asResource().isURIResource()) {
                        p = ResourceFactory.createProperty(obj.asResource().getURI());
                    }
                } else if (pred.equals(RDF.object)) {
                    oCount++;
                    o = obj;
                } else if (pred.equals(RDF.type) && obj.equals(RDF.Statement)) {
                    stmtCount++;
                } else {
                    metadata.add(pStmt);
                }
            }
        } finally {
            props.close();
        }

        if (sCount != 1 || pCount != 1 || oCount != 1 || stmtCount > 1) {
            return;
        }

        if (s == null || p == null || o == null) {
            return;
        }

        if (!isValidSubject(s) || !isValidPredicate(p) || !isValidObject(o)) {
            return;
        }

        validClusterCount++;

        buildAndProcessCluster(inGraph, cNode, s, p, o, metadata, clusterProcessor);
    }

    private void buildAndProcessCluster(Model inGraph, Resource cNode, Resource s, Property p, RDFNode o,
                                        Set<Statement> metadata, Consumer<Cluster> clusterProcessor) {

        int nSpo = calculateNSpo(inGraph, cNode, s, p, o);

        boolean isLocal = !inGraph.contains(null, null, cNode);
        boolean inGIn = inGraph.contains(s, p, o);
        boolean isNestedTarget = inGraph.contains(null, null, cNode);

        Cluster cluster = new Cluster(cNode, s, p, o, metadata, nSpo, inGIn, isLocal, isNestedTarget);
        evaluateDependencyAndProcess(cluster, inGraph, clusterProcessor);
    }

    private void evaluateDependencyAndProcess(Cluster cluster, Model inGraph, Consumer<Cluster> clusterProcessor) {
        Resource s = cluster.getSubjectNode();
        RDFNode o = cluster.getObjectNode();

        boolean isSCluster = isValidCluster(inGraph, s);
        boolean isOCluster = o.isResource() && isValidCluster(inGraph, o.asResource());

        String sId = getNodeId(s);
        String oId = getNodeId(o);

        boolean waitingForS = isSCluster && !emittedClusters.contains(sId);
        boolean waitingForO = isOCluster && !emittedClusters.contains(oId);

        if (waitingForS) {
            waitingRoom.computeIfAbsent(sId, _ -> new ArrayList<>()).add(cluster);
        } else if (waitingForO) {
            waitingRoom.computeIfAbsent(oId, _ -> new ArrayList<>()).add(cluster);
        } else {
            processRecursively(cluster, inGraph, clusterProcessor);
        }
    }

    private void processRecursively(Cluster cluster, Model inGraph, Consumer<Cluster> clusterProcessor) {
        clusterProcessor.accept(cluster);

        Resource cNode = cluster.getClusterNode();
        String thisClusterId = getNodeId(cNode);

        emittedClusters.add(thisClusterId);

        List<Cluster> waitingParents = waitingRoom.remove(thisClusterId);

        if (waitingParents != null) {
            for (Cluster parent : waitingParents) {
                evaluateDependencyAndProcess(parent, inGraph, clusterProcessor);
            }
        }
    }

    private boolean isValidCluster(Model inGraph, Resource cNode) {
        if (cNode == null || !inGraph.contains(cNode, RDF.subject, (RDFNode) null)) return false;

        int sCount = 0, pCount = 0, oCount = 0, stmtCount = 0;
        Resource s = null; Property p = null; RDFNode o = null;

        StmtIterator props = inGraph.listStatements(cNode, null, (RDFNode) null);
        try {
            while (props.hasNext()) {
                Statement pStmt = props.next();
                Property pred = pStmt.getPredicate();
                RDFNode obj = pStmt.getObject();

                if (pred.equals(RDF.subject)) {
                    sCount++;
                    if (obj.isResource()) s = obj.asResource();
                } else if (pred.equals(RDF.predicate)) {
                    pCount++;
                    if (obj.isResource() && obj.asResource().isURIResource()) {
                        p = ResourceFactory.createProperty(obj.asResource().getURI());
                    }
                } else if (pred.equals(RDF.object)) {
                    oCount++;
                    o = obj;
                } else if (pred.equals(RDF.type) && obj.equals(RDF.Statement)) {
                    stmtCount++;
                }
            }
        } finally {
            props.close();
        }

        if (sCount != 1 || pCount != 1 || oCount != 1 || stmtCount > 1) return false;
        if (s == null || p == null || o == null) return false;
        if (!isValidSubject(s) || !isValidPredicate(p) || !isValidObject(o)) return false;

        return true;
    }

    private int calculateNSpo(Model inGraph, Resource currentCluster, Resource s, Property p, RDFNode o) {
        int count = 1;

        StmtIterator it = inGraph.listStatements(null, RDF.subject, s);
        try {
            while (it.hasNext()) {
                Resource potentialCluster = it.next().getSubject();

                if (!potentialCluster.equals(currentCluster)) {
                    if (inGraph.contains(potentialCluster, RDF.predicate, p) &&
                            inGraph.contains(potentialCluster, RDF.object, o)) {

                        count++;
                        if (count > 1) {
                            return count;
                        }
                    }
                }
            }
        } finally {
            it.close();
        }
        return count;
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

    private boolean isValidSubject(Resource s) {
        return s.isURIResource() || s.isAnon();
    }

    private boolean isValidPredicate(Property p) {
        return p.isURIResource();
    }

    private boolean isValidObject(RDFNode o) {
        return o.isURIResource() || o.isAnon() || o.isLiteral();
    }
}