package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterConverterTest {

    private ClusterExtractor extractor;
    private ClusterConverter converter;

    @BeforeEach
    void setUp() {
        extractor = new ClusterExtractor();
        converter = new ClusterConverter();
    }


    @Test
    void testConvertCluster_ReifiedTripleExpanded() {
        System.out.println("Reified triple expanded");
        Model inGraph = RDFDataMgr.loadModel("src/test/resources/examples/converter/classic/multi-metadata.ttl");
        Model expectedModel = RDFDataMgr.loadModel("src/test/resources/examples/converter/expected/reified-triple-expanded-expected.ttl");

        List<Cluster> clusters = extractor.extractClusters(inGraph);

        Model actualModel = ModelFactory.createDefaultModel();
        for (Cluster cluster : clusters) {
            Model convertedClusterModel = converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE_EXPANDED);
            actualModel.add(convertedClusterModel);
        }

        actualModel.setNsPrefixes(inGraph);

        assertTrue(expectedModel.isIsomorphicWith(actualModel),
                "The generated model does not match the expected RDF 1.2 output.");
    }

    @Test
    void testConvertCluster_ReifiedTriple() {
        System.out.println("Reified triple");

        Model inGraph = RDFDataMgr.loadModel("src/test/resources/examples/converter/classic/blank-reifier.ttl");
        Model expectedModel = RDFDataMgr.loadModel("src/test/resources/examples/converter/expected/reified-triple-expected.ttl");
//        System.out.println("EXPECTED: ");
//        RDFDataMgr.write(System.out, expectedModel, RDFFormat.TURTLE_BLOCKS);

        List<Cluster> clusters = extractor.extractClusters(inGraph);
        Model actualModel = ModelFactory.createDefaultModel();
        for (Cluster cluster : clusters) {
            Model convertedClusterModel = converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE);
            actualModel.add(convertedClusterModel);
        }
        actualModel.setNsPrefixes(inGraph);

        assertTrue(expectedModel.isIsomorphicWith(actualModel),
                "The generated model does not match the expected RDF 1.2 output.");
    }
}