package pl.uwb.cr2tt.core;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.utils.Logger;

import java.util.*;
import java.util.function.Consumer;

public class ClusterExtractor {
    private final Set<String> processedClusters = new HashSet<>();
    private final Map<String, List<Cluster>> waitingRoom = new HashMap<>();

    public void extractAndProcess(Model inGraph, Consumer<Cluster> clusterProcessor) {
        Logger.info("starting extraction of clusters.");

        String sparqlString =
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "SELECT ?cluster ?s ?p ?o ?metaPred ?metaObj " +
                        "WHERE { " +
                        "  { " +
                        "    SELECT ?cluster ?s ?p ?o " +
                        "    WHERE { " +
                        "      ?cluster rdf:subject ?s ; " +
                        "               rdf:predicate ?p ; " +
                        "               rdf:object ?o . " +
                        "      OPTIONAL { ?cluster rdf:type ?type } " +
                        "    } GROUP BY ?cluster ?s ?p ?o " +
                        "    HAVING (COUNT(?type) <= 1 && (isIRI(?s) || isBlank(?s)) && isIRI(?p)) " +
                        "  } " +
                        "  ?cluster ?metaPred ?metaObj . " +
                        "  FILTER (?metaPred != rdf:subject && ?metaPred != rdf:predicate && ?metaPred != rdf:object) " +
                        "  FILTER (!(?metaPred = rdf:type && ?metaObj = rdf:Statement)) " +
                        "} ORDER BY ?cluster";

        Logger.info("compiling SPARQL query.");
        try (QueryExecution qexec = QueryExecution.model(inGraph).query(QueryFactory.create(sparqlString)).build()) {

            Logger.info("executing query.");
            ResultSet results = qexec.execSelect();
            Logger.info("query executed successfully.");

            Resource lastClusterNode = null;
            Resource currentS = null;
            Property currentP = null;
            RDFNode currentO = null;
            Set<Statement> metadata = new HashSet<>();
            long rowCount = 0;

            while (results.hasNext()) {
                rowCount++;
                if (rowCount == 1) {
                    Logger.info("global sort finished.");
                }

                if (rowCount % 100000 == 0) {
                    Logger.info("processed row: " + rowCount);
                }

                QuerySolution soln = results.nextSolution();
                Resource cNode = soln.getResource("cluster");

                if (lastClusterNode != null && !cNode.equals(lastClusterNode)) {
                    int nSpo = calculateNSpo(inGraph, currentS, currentP, currentO);
                    boolean isLocal = !inGraph.contains(null, null, lastClusterNode);
                    boolean inGIn = inGraph.contains(currentS, currentP, currentO);

                    Cluster cluster = new Cluster(lastClusterNode, currentS, currentP, currentO, metadata,
                            nSpo, inGIn, isLocal);
                    evaluateDependencyAndProcess(cluster, inGraph, clusterProcessor);
                    metadata = new HashSet<>();
                }

                lastClusterNode = cNode;
                currentS = soln.getResource("s");
                currentP = ResourceFactory.createProperty(soln.getResource("p").getURI());
                currentO = soln.get("o");

                Property mPred = ResourceFactory.createProperty(soln.getResource("metaPred").getURI());
                RDFNode mObj = soln.get("metaObj");
                metadata.add(ResourceFactory.createStatement(cNode, mPred, mObj));
            }

            if (lastClusterNode != null) {
                int nSpo = calculateNSpo(inGraph, currentS, currentP, currentO);
                boolean isLocal = !inGraph.contains(null, null, lastClusterNode);
                boolean inGIn = inGraph.contains(currentS, currentP, currentO);

                Cluster cluster = new Cluster(lastClusterNode, currentS, currentP, currentO, metadata,
                        nSpo, inGIn, isLocal);
                evaluateDependencyAndProcess(cluster, inGraph, clusterProcessor);
            }

            Logger.info("finished reading stream. Total rows: " + rowCount);

            if (!waitingRoom.isEmpty()) {
                int cyclicCount = waitingRoom.values().stream().mapToInt(List::size).sum();
                Logger.error("cyclic reification omitted due to loop detection. Skipped clusters: " + cyclicCount);
            }
        }
    }

    private void evaluateDependencyAndProcess(Cluster cluster, Model inGraph, Consumer<Cluster> clusterProcessor) {
        Resource s = cluster.getSubjectNode();
        RDFNode o = cluster.getObjectNode();

        boolean isSCluster = inGraph.contains(s, RDF.subject, (RDFNode) null);
        boolean isOCluster = o.isResource() && inGraph.contains(o.asResource(), RDF.subject, (RDFNode) null);

        boolean waitingForS = isSCluster && !processedClusters.contains(s.toString());
        boolean waitingForO = isOCluster && !processedClusters.contains(o.toString());

        if (waitingForS) {
            waitingRoom.computeIfAbsent(s.toString(), _ -> new ArrayList<>()).add(cluster);
        } else if (waitingForO) {
            waitingRoom.computeIfAbsent(o.toString(), _ -> new ArrayList<>()).add(cluster);
        } else {
            processRecursively(cluster, inGraph, clusterProcessor);
        }
    }

    private void processRecursively(Cluster cluster, Model inGraph, Consumer<Cluster> clusterProcessor) {
        clusterProcessor.accept(cluster);

        String thisClusterId = cluster.getClusterNode().toString();
        processedClusters.add(thisClusterId);

        List<Cluster> waitingParents = waitingRoom.remove(thisClusterId);

        if (waitingParents != null) {
            for (Cluster parent : waitingParents) {
                evaluateDependencyAndProcess(parent, inGraph, clusterProcessor);
            }
        }
    }

    private int calculateNSpo(Model inGraph, Resource s, Property p, RDFNode o) {
        int count = 0;
        StmtIterator it = inGraph.listStatements(null, RDF.subject, s);

        while (it.hasNext()) {
            Resource potentialCluster = it.next().getSubject();
            if (inGraph.contains(potentialCluster, RDF.predicate, p) &&
                    inGraph.contains(potentialCluster, RDF.object, o)) {
                count++;
            }
        }
        return count;
    }
}