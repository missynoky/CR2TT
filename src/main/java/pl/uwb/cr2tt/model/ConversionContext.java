package pl.uwb.cr2tt.model;

import java.io.File;

public class ConversionContext {
    private final File inputFile;
    private final File outputFile;
    private final ConversionMode mode;
    private final BaseTriplePolicy baseTriplePolicy;
    private final boolean allowAssertingConversion;
    private final boolean validateOnly;
    private final boolean keepStatementType;

    public ConversionContext(File inputFile, File outputFile, ConversionMode mode,
                             BaseTriplePolicy baseTriplePolicy, boolean allowAssertingConversion,
                             boolean validateOnly, boolean keepStatementType) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.mode = mode;
        this.baseTriplePolicy = baseTriplePolicy;
        this.allowAssertingConversion = allowAssertingConversion;
        this.validateOnly = validateOnly;
        this.keepStatementType = keepStatementType;
    }

    public File getInputFile() {
        return inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public ConversionMode getMode() {
        return mode;
    }

    public BaseTriplePolicy getBaseTriplePolicy() {
        return baseTriplePolicy;
    }

    public boolean isAllowAssertingConversion() {
        return allowAssertingConversion;
    }

    public boolean isValidateOnly() {
        return validateOnly;
    }

    public boolean isKeepStatementType() {
        return keepStatementType;
    }
}