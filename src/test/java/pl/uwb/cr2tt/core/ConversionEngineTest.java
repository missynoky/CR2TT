package pl.uwb.cr2tt.core;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.ConversionContext;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConversionEngineTest {

    @TempDir
    Path tempDir;

    private static final String EXAMPLES_DIR = "src/test/resources/examples/classic/";

    @BeforeEach
    public void setUp() {
        Logger.init(false);
    }

    private ConversionContext createContext(File inputFile, File outputFile, ConversionMode mode,
                                            boolean validateOnly) {
        return new ConversionContext(
                inputFile,
                outputFile,
                mode,
                BaseTriplePolicy.PRESERVE,
                false,
                validateOnly,
                false
        );
    }

    @Test
    public void shouldPreserveNamedGraphExistenceDuringConversion() {
        File inputFile = new File(EXAMPLES_DIR + "input_named.trig");
        File outputFile = tempDir.resolve("output_existence.trig").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED,
                false);
        ConversionEngine engine = new ConversionEngine(context);

        engine.run();

        Dataset resultDataset = DatasetFactory.create();
        RDFDataMgr.read(resultDataset, context.getOutputFile().getAbsolutePath());

        assertTrue(resultDataset.containsNamedModel("ex:Graph1"),
                "Output file should preserve the named graph.");
    }

    @Test
    public void shouldPreserveNamedGraphContentDuringConversion() {
        File inputFile = new File(EXAMPLES_DIR + "input_named.trig");
        File outputFile = tempDir.resolve("output_content.trig").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED,
                false);
        ConversionEngine engine = new ConversionEngine(context);

        engine.run();

        Dataset resultDataset = DatasetFactory.create();
        RDFDataMgr.read(resultDataset, context.getOutputFile().getAbsolutePath());

        Model namedGraph = resultDataset.getNamedModel("ex:Graph1");
        assertFalse(namedGraph.isEmpty(), "The named graph should contain triples, but it is empty.");
    }

    @Test
    public void shouldPreserveMultipleNamedGraphsAndTheirContent() {
        File inputFile = new File(EXAMPLES_DIR + "input_named_multi.trig");
        File outputFile = tempDir.resolve("output_multi.trig").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED,
                false);
        ConversionEngine engine = new ConversionEngine(context);

        engine.run();

        Dataset resultDataset = DatasetFactory.create();
        RDFDataMgr.read(resultDataset, context.getOutputFile().getAbsolutePath());

        assertTrue(resultDataset.containsNamedModel("http://example.org/Graph1"), "Graph1 should exist.");
        assertTrue(resultDataset.containsNamedModel("http://example.org/Graph2"), "Graph2 should exist.");

        Model graph1 = resultDataset.getNamedModel("http://example.org/Graph1");
        assertFalse(graph1.isEmpty(), "Graph1 should not be empty.");

        Model graph2 = resultDataset.getNamedModel("http://example.org/Graph2");
        assertFalse(graph2.isEmpty(), "Graph2 should not be empty.");
    }

    @Test
    public void shouldReturnFalseAndSkipExportWhenValidateOnlyFails() {
        File inputFile = new File(EXAMPLES_DIR + "input_explicit.ttl");
        File outputFile = tempDir.resolve("output.ttl").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE,
                true);
        ConversionEngine engine = new ConversionEngine(context);

        boolean isValidResult = engine.run();

        assertFalse(isValidResult, "Engine should return false.");
        assertFalse(context.getOutputFile().exists(), "Output file should not be created.");
    }

    @Test
    public void shouldThrowExceptionWhenDatasetHasNamedGraphsButOutputFormatIsInvalid() {
        File inputFile = new File(EXAMPLES_DIR + "input_named.trig");
        File outputFile = tempDir.resolve("output.ttl").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED, false);
        ConversionEngine engine = new ConversionEngine(context);

        RuntimeException exception = assertThrows(RuntimeException.class, engine::run);

        assertTrue(exception.getMessage().contains("The input dataset contains named graphs"));
    }

    @Test
    public void shouldPassValidationWhenDatasetHasNamedGraphsAndOutputFormatIsNq() {
        File inputFile = new File(EXAMPLES_DIR + "input_named.trig");
        File outputFile = tempDir.resolve("output.nq").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED, false);
        ConversionEngine engine = new ConversionEngine(context);

        assertDoesNotThrow(engine::run, "Engine should pass validation when output format is .nq");
    }

    @Test
    public void shouldPassValidationWhenDatasetHasNoNamedGraphsAndOutputFormatIsTtl() {
        File inputFile = new File(EXAMPLES_DIR + "input_explicit.ttl");
        File outputFile = tempDir.resolve("output_explicit.ttl").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE, false);
        ConversionEngine engine = new ConversionEngine(context);

        assertDoesNotThrow(engine::run, "Engine should pass format validation for .ttl if no named graphs exist");
    }

    @Test
    public void shouldProperlyConvertNestedReificationAndPreserveDomainData() {
        File inputFile = new File(EXAMPLES_DIR + "input_engine_nested_explicit.ttl");
        File expectedFile = new File("src/test/resources/examples/expected/expected_nested.ttl");
        File outputFile = tempDir.resolve("output_nested_test.ttl").toFile();

        ConversionContext context = new ConversionContext(
                inputFile,
                outputFile,
                ConversionMode.REIFIED_TRIPLE_EXPANDED,
                BaseTriplePolicy.PRESERVE,
                false,
                false,
                false
        );

        ConversionEngine engine = new ConversionEngine(context);
        engine.run();

        Model actualModel = RDFDataMgr.loadModel(outputFile.getAbsolutePath());
        Model expectedModel = RDFDataMgr.loadModel(expectedFile.getAbsolutePath());

        // Opcjonalne drukowanie do konsoli (jeśli test nie przejdzie, od razu widać dlaczego)
        if (actualModel.isIsomorphicWith(expectedModel)) {
            actualModel.setNsPrefixes(expectedModel.getNsPrefixMap());
            System.out.println("Actual model");
            RDFDataMgr.write(System.out, actualModel, org.apache.jena.riot.Lang.TURTLE);

            System.out.println("\nExpected model");
            RDFDataMgr.write(System.out, expectedModel, org.apache.jena.riot.Lang.TURTLE);
        }

        assertTrue(actualModel.isIsomorphicWith(expectedModel),
                "Graphs are not isomorphic.");
    }
}