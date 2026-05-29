package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.ConversionMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterConverterTest {

    private void verifyConversion(String inputFilePath, String expectedFilePath, ConversionMode mode) {
        Model inModel = RDFDataMgr.loadModel(inputFilePath);

        Model actualModel = ModelFactory.createDefaultModel();

        ClusterExtractor extractor = new ClusterExtractor();
        ClusterConverter converter = new ClusterConverter();

        extractor.extractAndProcess(inModel, cluster -> {
            converter.convertCluster(cluster, mode, actualModel);
        });

        Model expectedModel = RDFDataMgr.loadModel(expectedFilePath);

        assertTrue(actualModel.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }

    @Nested
    class ExplicitReifierTests {

        private final String INPUT_PATH = "src/test/resources/examples/classic/input_explicit.ttl";
        private final String EXPECTED_DIR = "src/test/resources/examples/expected/";

        @Test
        void shouldConvertReifiedTripleExpanded() {
            verifyConversion(INPUT_PATH, EXPECTED_DIR + "expected_reified_triple_expanded.ttl", ConversionMode.REIFIED_TRIPLE_EXPANDED);
        }

        @Test
        void shouldConvertReifiedTripleExplicit() {
            verifyConversion(INPUT_PATH, EXPECTED_DIR + "expected_reified_triple_explicit.ttl", ConversionMode.REIFIED_TRIPLE_EXPLICIT);
        }

        @Test
        void shouldConvertAnnotatedTripleExplicit() {
            verifyConversion(INPUT_PATH, EXPECTED_DIR + "expected_annotated_triple_explicit.ttl", ConversionMode.ANNOTATED_TRIPLE_EXPLICIT);
        }

        @Test
        void shouldConvertAnnotatedTripleExpanded() {
            verifyConversion(INPUT_PATH, EXPECTED_DIR + "expected_annotated_triple_expanded.ttl", ConversionMode.ANNOTATED_TRIPLE_EXPANDED);
        }
    }

    @Nested
    class BlankNodeReifierTests {

        private final String INPUT_PATH = "src/test/resources/examples/classic/input_blank.ttl";
        private final String EXPECTED_DIR = "src/test/resources/examples/expected/";

        @Test
        void shouldConvertReifiedTriple() {
            verifyConversion(INPUT_PATH, EXPECTED_DIR + "expected_reified_triple.ttl", ConversionMode.REIFIED_TRIPLE);
        }

        @Test
        void shouldConvertAnnotatedTriple() {
            verifyConversion(INPUT_PATH, EXPECTED_DIR + "expected_annotated_triple.ttl", ConversionMode.ANNOTATED_TRIPLE);
        }
    }
}