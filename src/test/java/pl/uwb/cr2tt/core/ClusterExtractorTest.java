package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.Cluster;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterExtractorTest {
    private static final String EX = "http://example.org/";

    private Model model;
    private ClusterExtractor extractor;
    private List<Cluster> capturedClusters;

    @BeforeEach
    public void setUp() {
        model = ModelFactory.createDefaultModel();
        extractor = new ClusterExtractor();
        capturedClusters = new ArrayList<>();
    }

    private Resource createCluster(String clusterUri, String pUri, String oUri, String metaValue) {
        Resource clusterNode = model.createResource(clusterUri);
        clusterNode.addProperty(RDF.subject, model.createResource(EX + "s"))
                .addProperty(RDF.predicate, model.createProperty(pUri))
                .addProperty(RDF.object, model.createResource(oUri))
                .addProperty(model.createProperty(EX + "meta"), metaValue);
        return clusterNode;
    }

    @Test
    public void testFlatClusterExtraction() {
        createCluster(EX + "Cluster1", EX + "p", EX + "o", "value1");

        extractor.extractAndProcess(model, cluster -> capturedClusters.add(cluster));

        assertEquals(1, capturedClusters.size(), "Exactly one cluster should be extracted.");
        assertEquals(1, capturedClusters.getFirst().getNSpo(), "n_spo should be 1.");
    }

    @Test
    public void testNSpoCalculation() {
        createCluster(EX + "Cluster1", EX + "p", EX + "o", "meta1");
        createCluster(EX + "Cluster2", EX + "p", EX + "o", "meta2");

        extractor.extractAndProcess(model, cluster -> capturedClusters.add(cluster));

        assertEquals(2, capturedClusters.size(), "Both clusters should be extracted.");
        assertEquals(2, capturedClusters.getFirst().getNSpo(), "n_spo should be 2 for the duplicate base triples.");
    }

    @Test
    public void testCyclicReification() {
        Resource clusterA = createCluster(EX + "ClusterA", EX + "p1", EX + "o1", "metaA");
        Resource clusterB = createCluster(EX + "ClusterB", EX + "p2", EX + "o2", "metaB");

        clusterA.removeAll(RDF.object).addProperty(RDF.object, clusterB);
        clusterB.removeAll(RDF.object).addProperty(RDF.object, clusterA);

        extractor.extractAndProcess(model, cluster -> capturedClusters.add(cluster));

        assertEquals(0, capturedClusters.size(), "Cyclic clusters should be blocked and not processed.");
    }

    @Test
    public void testExtractionIgnoresNormalTriples() {
        createCluster(EX + "Cluster1", EX + "p", EX + "o", "meta1");

        Resource jan = model.createResource(EX + "Jan");
        Property likes = model.createProperty(EX + "likes");
        Resource dogs = model.createResource(EX + "Dogs");

        jan.addProperty(likes, dogs);

        extractor.extractAndProcess(model, cluster -> capturedClusters.add(cluster));

        assertEquals(1, capturedClusters.size(), "Normal triples must be ignored by the extractor.");

        assertEquals(EX + "Cluster1", capturedClusters.getFirst().getClusterNode().getURI(), "The extracted cluster URI does not match.");
    }
}