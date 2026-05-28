package pl.uwb.cr2tt.cli;

import pl.uwb.cr2tt.model.ConversionContext;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;

public class CliValidator {
    private final ConversionContext context;

    public CliValidator(ConversionContext context) {
        this.context = context;
    }

    public boolean initialValidator() {
        File inputFile = context.getInputFile();
        File outputFile = context.getOutputFile();
        boolean validateOnly = context.isValidateOnly();
        boolean allowAssertingConversion = context.isAllowAssertingConversion();
        ConversionMode mode = context.getMode();

        if (!inputFile.exists()) {
            Logger.error("input file does not exist: " + inputFile.getAbsolutePath());
            return false;
        }

        if (!inputFile.isFile()) {
            Logger.error("provided input path is not a file: " + inputFile.getAbsolutePath());
            return false;
        }

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
            }
        }

        if (allowAssertingConversion && !mode.name().startsWith("ANNOTATED")) {
            Logger.warn("--allow-asserting-conversion flag was provided, but it has no effect for mode: " + mode);
        }

        return true;
    }
}
