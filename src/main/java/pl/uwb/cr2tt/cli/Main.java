package pl.uwb.cr2tt.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;
import java.util.concurrent.Callable;


@Command(
        name = "rdf12-reif",
        mixinStandardHelpOptions = true,
        version = "rdf12-reif 1.0",
        description = "Converts classic reification to triple terms representation."
)
public class Main implements Callable<Integer> {

    @Option(
            names = {"--input"},
            required = true,
            description = "Input file containing classic reification."
    )
    private File inputFile;

    @Option(
            names = {"--output"},
            description = "Output file path. If not provided, RDF 1.2 result is printed to standard output."
    )
    private File outputFile;

    @Option(
            names = {"--mode"},
            defaultValue = "reified_triple_expanded",
            description = "Selection of the target RDF 1.2 form. " +
                    "Available modes: ${COMPLETION-CANDIDATES}. " +
                    "Default: ${DEFAULT-VALUE}."
    )
    private ConversionMode mode;

    @Option(
            names = {"--base-triple-policy"},
            defaultValue = "preserve",
            description = "Controls if the reified triple (s p o) must already exist in the input as an assertion. " +
                    "Values: ${COMPLETION-CANDIDATES}. Default: ${DEFAULT-VALUE}."
    )
    private BaseTriplePolicy baseTriplePolicy;

    @Option(
            names = {"--allow-asserting-conversion"},
            description = "Explicitly allows modes that may add a regular assertion of the reified triple to the graph."
    )
    private boolean allowAssertingConversion;

    @Option(
            names = {"--validate-only"},
            description = "Parses input and checks if the selected conversion mode is valid, but does not save RDF output."
    )
    private boolean validateOnly;

    @Option(
            names = {"--verbose"},
            description = "Prints diagnostic messages about the conversion process."
    )
    private boolean verbose;

    @Override
    public Integer call() {
        Logger.init(verbose);
        Validator validator = new Validator(inputFile, outputFile, validateOnly, allowAssertingConversion, mode);
        Model inGraph;

        Logger.info("starting rdf12-reif conversion process.");

        boolean initialValidatorResult = validator.initialValidator();
        if (!initialValidatorResult) {
            Logger.error("Initial validation failed. Aborting.");
            return -1;
        }

        Logger.info("loading graph from input file.");
        try {
            inGraph = RDFDataMgr.loadModel(inputFile.getAbsolutePath());
        } catch (Exception e) {
            Logger.error("Failed to parse the input file as RDF: " + e.getMessage());
            return -1;
        }

        Logger.info("successfully loaded " + inGraph.size() + " triples into the model.");

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}