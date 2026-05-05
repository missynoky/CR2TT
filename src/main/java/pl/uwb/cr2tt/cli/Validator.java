package pl.uwb.cr2tt.cli;

import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;

public class Validator {
    private final File inputFile;
    private final File outputFile;
    private final boolean validateOnly;
    private final boolean allowAssertingConversion;
    private final ConversionMode mode;

    public Validator(File inputFile, File outputFile, boolean validateOnly, boolean allowAssertingConversion, ConversionMode mode) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.validateOnly = validateOnly;
        this.allowAssertingConversion = allowAssertingConversion;
        this.mode = mode;
    }

    public boolean initialValidator() {
        if (!inputFile.exists()) {
            Logger.error("input file does not exist: " + inputFile.getAbsolutePath());
            return false;
        }

        if (!inputFile.isFile()) {
            Logger.error("provided input path is not a file: " + inputFile.getAbsolutePath());
            return false;
        }

        Logger.info("Input file path is valid: " + inputFile.getAbsolutePath());

        if (validateOnly) {
            Logger.info("validate-only mode active. Output destination: none.");

            if (outputFile != null) {
                Logger.warn("--output was provided but will be ignored due to --validate-only.");
            }
        } else {
            if (outputFile != null) {
                if (outputFile.exists() && outputFile.isDirectory()) {
                    Logger.error("provided output path is a directory, not a file: " + outputFile.getAbsolutePath());
                    return false;
                }

                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    Logger.error("parent directory for output file does not exist: " + parentDir.getAbsolutePath());
                    return false;
                }

                Logger.info("Output destination is a file: " + outputFile.getAbsolutePath());
            } else {
                Logger.info("Output destination is standard output.");
                // TODO: standard output
            }
        }

        if (allowAssertingConversion && !mode.name().startsWith("ANNOTATED")) {
            Logger.warn("--allow-asserting-conversion flag was provided, but it has no effect for mode: " + mode);
        }

        return true;
    }
}
