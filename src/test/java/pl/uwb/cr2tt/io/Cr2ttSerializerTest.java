package pl.uwb.cr2tt.io;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.core.ClusterConverter;
import pl.uwb.cr2tt.core.ClusterExtractor;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Cr2ttSerializerTest {
    private ClusterExtractor extractor;
    private ClusterConverter converter;

    @BeforeEach
    void setUp() {
        extractor = new ClusterExtractor();
        converter = new ClusterConverter();
    }

    @Test
    void testSerialize_ReifiedTriple_MultipleClusters() {
        Model inGraph = RDFDataMgr.loadModel("src/test/resources/examples/serializer/classic/blank-reifier-serializer.ttl");
        Model expectedModel = RDFDataMgr.loadModel("src/test/resources/examples/serializer/expected/reified-triple-expected-serializer.ttl");

        List<Cluster> clusters = extractor.extractClusters(inGraph);
        Model actualModel = ModelFactory.createDefaultModel();
        for (Cluster cluster : clusters) {
            actualModel.add(converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE));
        }
        actualModel.setNsPrefixes(inGraph);

        String outputPath = "src/test/resources/examples/serializer/triple-term/reified-triple-output.ttl";
        try (OutputStream out = new FileOutputStream(outputPath)) {
            Cr2ttSerializer.serialize(out, actualModel, ConversionMode.REIFIED_TRIPLE);
        } catch (IOException e) {
            fail("Failed to save the output file: " + e.getMessage());
        }

        assertTrue(expectedModel.isIsomorphicWith(actualModel), "The generated model does not match the expected RDF 1.2 output.");

        try {
            String actualText = Files.readString(Path.of(outputPath));

            assertTrue(actualText.contains("<< ex:Jan ex:knows ex:Anna >>"), "Missing expected syntax for the Jan cluster.");
            assertTrue(actualText.contains("<< ex:Piotr ex:worksAt ex:TechCorp >>"), "Missing expected syntax for the Piotr cluster.");
            assertTrue(actualText.contains("<< ex:Mary ex:loves ex:Coffee >>"), "Missing expected syntax for the Mary cluster.");

            assertFalse(actualText.contains("a rdf:Statement"), "The output file still contains old rdf:Statement declarations.");
            assertFalse(actualText.contains("rdf:subject"), "The output file still contains old rdf:subject declarations.");
            assertFalse(actualText.contains("rdf:reifies"), "The output file uses rdf:reifies instead of the << >> brackets.");
        } catch (IOException e) {
            fail("Failed to read files for text comparison.");
        }
    }

    @Test
    void testSerialize_ReifiedTriple_SingleCluster() {
        Model inGraph = RDFDataMgr.loadModel("src/test/resources/examples/serializer/classic/blank-reifier-single.ttl");
        Model expectedModel = RDFDataMgr.loadModel("src/test/resources/examples/serializer/expected/reified-triple-single-expected.ttl");

        List<Cluster> clusters = extractor.extractClusters(inGraph);
        Model actualModel = ModelFactory.createDefaultModel();
        for (Cluster cluster : clusters) {
            actualModel.add(converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE));
        }
        actualModel.setNsPrefixes(inGraph);

        String outputPath = "src/test/resources/examples/serializer/triple-term/reified-triple-single-output.ttl";
        try (OutputStream out = new FileOutputStream(outputPath)) {
            Cr2ttSerializer.serialize(out, actualModel, ConversionMode.REIFIED_TRIPLE);
        } catch (IOException e) {
            fail("Failed to save the output file: " + e.getMessage());
        }

        assertTrue(expectedModel.isIsomorphicWith(actualModel), "Conceptual models differ.");

        try {
            String actualText = Files.readString(Path.of(outputPath));
            assertTrue(actualText.contains("<< ex:Jan ex:knows ex:Anna >>"), "Missing expected syntax for the single cluster.");
            assertFalse(actualText.contains("rdf:reifies"), "rdf:reifies found instead of << >>.");
        } catch (IOException e) {
            fail("Failed to read files for text comparison.");
        }
    }

    @Test
    void testSerialize_ReifiedTriple_MixedData() {
        Model inGraph = RDFDataMgr.loadModel("src/test/resources/examples/serializer/classic/blank-reifier-mixed.ttl");
        Model expectedModel = RDFDataMgr.loadModel("src/test/resources/examples/serializer/expected/reified-triple-mixed-expected.ttl");

        List<Cluster> clusters = extractor.extractClusters(inGraph);

        Model actualModel = ModelFactory.createDefaultModel().add(inGraph);

        for (Cluster cluster : clusters) {
            actualModel.removeAll(cluster.getReifier(), null, null);
            actualModel.add(converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE));
        }

        String outputPath = "src/test/resources/examples/serializer/triple-term/reified-triple-mixed-output.ttl";
        try (OutputStream out = new FileOutputStream(outputPath)) {
            Cr2ttSerializer.serialize(out, actualModel, ConversionMode.REIFIED_TRIPLE);
        } catch (IOException e) {
            fail("Failed to save the output file: " + e.getMessage());
        }

        assertTrue(expectedModel.isIsomorphicWith(actualModel), "Conceptual models for mixed data differ.");

        try {
            String actualText = Files.readString(Path.of(outputPath));

            assertTrue(actualText.contains("<< ex:Jan ex:knows ex:Anna >>"), "Missing converted syntax for the target cluster.");
            assertFalse(actualText.contains("a rdf:Statement"), "Old reification structures were not fully removed.");

            assertTrue(actualText.contains("ex:CompanyA ex:location ex:Warsaw"), "Standard IRI triples were corrupted or lost.");
            assertTrue(actualText.contains("ex:name \"Piotr\""), "Standard Blank Node properties were corrupted or lost.");
            assertTrue(actualText.contains("ex:Warsaw rdf:type ex:City"), "Standard type definitions were corrupted or lost.");

        } catch (IOException e) {
            fail("Failed to read files for text comparison.");
        }
    }
}