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
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                        "SELECT ?cluster ?s ?p ?o ?metaPred ?metaObj \n" +
                        "WHERE { \n" +
                        "  { \n" +
                        "    SELECT ?cluster ?s ?p ?o \n" +
                        "    WHERE { \n" +
                        "      ?cluster rdf:subject ?s ; \n" +
                        "               rdf:predicate ?p ; \n" +
                        "               rdf:object ?o . \n" +
                        "      \n" +
                        "      FILTER NOT EXISTS { \n" +
                        "        ?cluster rdf:subject ?s2 . \n" +
                        "        FILTER (?s != ?s2) \n" +
                        "      } \n" +
                        "      \n" +
                        "      FILTER NOT EXISTS { \n" +
                        "        ?cluster rdf:predicate ?p2 . \n" +
                        "        FILTER (?p != ?p2) \n" +
                        "      } \n" +
                        "      \n" +
                        "      FILTER NOT EXISTS { \n" +
                        "        ?cluster rdf:object ?o2 . \n" +
                        "        FILTER (?o != ?o2) \n" +
                        "      } \n" +
                        "      \n" +
                        "      OPTIONAL { \n" +
                        "        ?cluster rdf:type ?type . \n" +
                        "        FILTER(?type = rdf:Statement) \n" +
                        "      } \n" +
                        "    } GROUP BY ?cluster ?s ?p ?o \n" +
                        "    HAVING (COUNT(?type) <= 1 && (isIRI(?s) || isBlank(?s)) && isIRI(?p) && (isIRI(?o) || isBlank(?o) || isLiteral(?o))) \n" +
                        "  } \n" +
                        "  OPTIONAL { \n" +
                        "    ?cluster ?metaPred ?metaObj . \n" +
                        "    FILTER (?metaPred != rdf:subject && ?metaPred != rdf:predicate && ?metaPred != rdf:object) \n" +
                        "    FILTER (!(?metaPred = rdf:type && ?metaObj = rdf:Statement)) \n" +
                        "  } \n" +
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
            long clusterCount = 0;

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
                    clusterCount++;
                    buildAndProcessCluster(inGraph, lastClusterNode, currentS, currentP, currentO, metadata, clusterProcessor);
                    metadata = new HashSet<>();
                }

                lastClusterNode = cNode;
                currentS = soln.getResource("s");
                currentP = ResourceFactory.createProperty(soln.getResource("p").getURI());
                currentO = soln.get("o");

                if (soln.contains("metaPred") && soln.contains("metaObj")) {
                    Property mPred = ResourceFactory.createProperty(soln.getResource("metaPred").getURI());
                    RDFNode mObj = soln.get("metaObj");
                    metadata.add(ResourceFactory.createStatement(cNode, mPred, mObj));
                }
            }

            if (lastClusterNode != null) {
                clusterCount++;
                buildAndProcessCluster(inGraph, lastClusterNode, currentS, currentP, currentO, metadata, clusterProcessor);
            }

            Logger.info("finished reading stream. Processed rows: " + rowCount + ", extracted clusters: " + clusterCount);

            if (!waitingRoom.isEmpty()) {
                int cyclicCount = waitingRoom.values().stream().mapToInt(List::size).sum();
                Logger.error("cyclic reification omitted due to loop detection. Skipped clusters: " + cyclicCount);
            }
        }
    }

    private void buildAndProcessCluster(Model inGraph, Resource cNode, Resource s, Property p, RDFNode o,
                                        Set<Statement> metadata, Consumer<Cluster> clusterProcessor) {
        int nSpo = calculateNSpo(inGraph, s, p, o);
        boolean isLocal = !inGraph.contains(null, null, cNode);
        boolean inGIn = inGraph.contains(s, p, o);
        boolean isNestedTarget = false;

        Cluster cluster = new Cluster(cNode, s, p, o, metadata, nSpo, inGIn, isLocal, isNestedTarget);
        evaluateDependencyAndProcess(cluster, inGraph, clusterProcessor);
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