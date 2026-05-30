package pl.uwb.cr2tt.core;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
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

    private ConversionContext createContext(File inputFile, File outputFile, ConversionMode mode, boolean validateOnly) {
        return new ConversionContext(
                inputFile,
                outputFile,
                mode,
                BaseTriplePolicy.PRESERVE,
                false,
                validateOnly
        );
    }

    @Test
    public void shouldPreserveNamedGraphExistenceDuringConversion() {
        File inputFile = new File(EXAMPLES_DIR + "input_named.trig");
        File outputFile = tempDir.resolve("output_existence.trig").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED, false);
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

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE_EXPANDED, false);
        ConversionEngine engine = new ConversionEngine(context);

        engine.run();

        Dataset resultDataset = DatasetFactory.create();
        RDFDataMgr.read(resultDataset, context.getOutputFile().getAbsolutePath());

        org.apache.jena.rdf.model.Model namedGraph = resultDataset.getNamedModel("ex:Graph1");
        assertFalse(namedGraph.isEmpty(), "The named graph should contain triples, but it is empty.");
    }

    @Test
    public void shouldReturnFalseAndSkipExportWhenValidateOnlyFails() {
        File inputFile = new File(EXAMPLES_DIR + "input_explicit.ttl");
        File outputFile = tempDir.resolve("output.ttl").toFile();

        ConversionContext context = createContext(inputFile, outputFile, ConversionMode.REIFIED_TRIPLE, true);
        ConversionEngine engine = new ConversionEngine(context);

        boolean isValidResult = engine.run();

        assertFalse(isValidResult, "Engine should return false.");
        assertFalse(context.getOutputFile().exists(), "Output file should not be created.");
    }
}