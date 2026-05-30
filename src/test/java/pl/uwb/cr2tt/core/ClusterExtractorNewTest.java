package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.Cluster;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterExtractorNewTest {

    private static final String ex = "ex:";

    private Model inGraph;
    private ClusterExtractorNew extractor;
    private List<Cluster> extractedClusters;

    @BeforeEach
    public void setUp() {
        inGraph = ModelFactory.createDefaultModel();
        extractor = new ClusterExtractorNew();
        extractedClusters = new ArrayList<>();
    }

    private void runExtraction() {
        extractor.extractAndProcess(inGraph, cluster -> extractedClusters.add(cluster));
    }

    @Test
    public void shouldExtractSingleValidCluster() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));
        inGraph.add(st, RDF.type, RDF.Statement);

        runExtraction();

        assertEquals(1, extractedClusters.size());
        assertEquals(ex + "Alice", extractedClusters.getFirst().getSubjectNode().asResource().getURI());
    }

    @Test
    public void shouldExtractClusterEvenWithoutTypeStatement() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));

        runExtraction();

        assertEquals(1, extractedClusters.size());
    }

    @Test
    public void shouldIgnoreClusterWhenMultipleSubjectsArePresent() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Charlie"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));

        runExtraction();

        assertEquals(0, extractedClusters.size());
    }

    @Test
    public void shouldIgnoreClusterWhenMultiplePredicatesArePresent() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "loves"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));

        runExtraction();

        assertEquals(0, extractedClusters.size());
    }

    @Test
    public void shouldIgnoreClusterWhenMultipleObjectsArePresent() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "John"));

        runExtraction();

        assertEquals(0, extractedClusters.size());
    }

    @Test
    public void shouldCorrectlyCountNSpoWhenMultipleClustersReifySameTriple() {
        Resource st1 = inGraph.createResource(ex + "st1");
        Resource st2 = inGraph.createResource(ex + "st2");
        Resource s = inGraph.createResource(ex + "Alice");
        Property p = inGraph.createProperty(ex + "knows");
        Resource o = inGraph.createResource(ex + "Bob");

        inGraph.add(st1, RDF.subject, s).add(st1, RDF.predicate, p).add(st1, RDF.object, o);
        inGraph.add(st2, RDF.subject, s).add(st2, RDF.predicate, p).add(st2, RDF.object, o);

        runExtraction();

        assertEquals(2, extractedClusters.size());
    }

    @Test
    public void shouldIncludeMetadataWhenPresentInCluster() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));
        inGraph.add(st, inGraph.createProperty(ex + "certainty"), inGraph.createTypedLiteral(0.9));

        runExtraction();

        assertEquals(1, extractedClusters.size());
        assertEquals(1, extractedClusters.getFirst().getMetadata().size());
    }

    @Test
    public void shouldExtractClusterWhenMetadataIsAbsent() {
        Resource st = inGraph.createResource(ex + "st1");
        inGraph.add(st, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st, RDF.object, inGraph.createResource(ex + "Bob"));

        runExtraction();

        assertEquals(1, extractedClusters.size());
        assertTrue(extractedClusters.getFirst().getMetadata().isEmpty());
    }

    @Test
    public void shouldMarkNestedClusterAsNestedTarget() {
        Resource st1 = inGraph.createResource(ex + "st1");
        inGraph.add(st1, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st1, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st1, RDF.object, inGraph.createResource(ex + "Bob"));

        Resource st2 = inGraph.createResource(ex + "st2");
        inGraph.add(st2, RDF.subject, inGraph.createResource(ex + "Charlie"));
        inGraph.add(st2, RDF.predicate, inGraph.createProperty(ex + "claims"));
        inGraph.add(st2, RDF.object, st1);

        runExtraction();

        assertEquals(2, extractedClusters.size());
        Cluster childCluster = extractedClusters.stream()
                .filter(c -> c.getClusterNode().getURI().equals(ex + "st1"))
                .findFirst().orElseThrow();

        assertTrue(childCluster.isNestedTarget());
    }

    @Test
    public void shouldSkipClustersInCyclicDependency() {
        Resource st1 = inGraph.createResource(ex + "st1");
        Resource st2 = inGraph.createResource(ex + "st2");

        inGraph.add(st1, RDF.subject, inGraph.createResource(ex + "Alice"));
        inGraph.add(st1, RDF.predicate, inGraph.createProperty(ex + "knows"));
        inGraph.add(st1, RDF.object, st2);

        inGraph.add(st2, RDF.subject, inGraph.createResource(ex + "Charlie"));
        inGraph.add(st2, RDF.predicate, inGraph.createProperty(ex + "claims"));
        inGraph.add(st2, RDF.object, st1);

        runExtraction();

        assertEquals(0, extractedClusters.size());
    }
}