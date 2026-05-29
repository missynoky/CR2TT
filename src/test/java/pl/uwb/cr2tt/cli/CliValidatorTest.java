package pl.uwb.cr2tt.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.ConversionContext;
import pl.uwb.cr2tt.model.ConversionMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CliValidatorTest {

    @Nested
    class InputFileValidation {

        private ConversionContext createMockContext(File inputFile) {
            return new ConversionContext(
                    inputFile,
                    null,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.PRESERVE,
                    false,
                    false
            );
        }

        @Test
        void shouldPassWhenInputFileExistsAndIsAFile(@TempDir Path tempDir) throws IOException {
            Path inputPath = tempDir.resolve("input_test.ttl");
            File validFile = Files.createFile(inputPath).toFile();
            ConversionContext context = createMockContext(validFile);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertTrue(result, "Validator should return true for a valid, existing input file.");
        }

        @Test
        void shouldFailWhenInputFileDoesNotExist() {
            File nonExistentFile = new File("path/fake_input.ttl");
            ConversionContext context = createMockContext(nonExistentFile);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertFalse(result, "Validator should return false when the input file does not exist.");
        }

        @Test
        void shouldFailWhenInputPathIsADirectory(@TempDir Path tempDir) {
            File directoryAsFile = tempDir.toFile();
            ConversionContext context = createMockContext(directoryAsFile);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertFalse(result, "Validator should return false when the provided input path is a directory.");
        }
    }

    @Nested
    class OutputFileValidation {
        @TempDir
        Path tempDir;

        File validIn;

        @BeforeEach
        void setUp() throws IOException {
            Path inPath = tempDir.resolve("in.ttl");
            validIn = Files.createFile(inPath).toFile();
        }

        private ConversionContext createMockContext(File outputFile) {
            return new ConversionContext(
                    validIn,
                    outputFile,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.PRESERVE,
                    false,
                    false
            );
        }

        @Test
        void shouldPassWhenOutputIsAValidFilePath() {
            File validOut = tempDir.resolve("out.ttl").toFile();

            ConversionContext context = createMockContext(validOut);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertTrue(result, "Validator should return true for a valid output file path.");
        }

        @Test
        void shouldPassWhenOutputIsNullForStandardOutput() {
            File nullOutput = null;

            ConversionContext context = createMockContext(nullOutput);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertTrue(result, "Validator should return true when output file is null - standard output.");
        }

        @Test
        void shouldFailWhenOutputPathIsAnExistingDirectory() {
            File directoryAsOut = tempDir.toFile();

            ConversionContext context = createMockContext(directoryAsOut);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertFalse(result, "Validator should return false when output path is an existing directory.");
        }

        @Test
        void shouldFailWhenOutputParentDirectoryDoesNotExist() {
            File outWithFakeParent = new File("folder/out.ttl");

            ConversionContext context = createMockContext(outWithFakeParent);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertFalse(result, "Validator should return false when parent directory of output does not exist.");
        }
    }

    @Nested
    class SpecificFlagsLogic {
        @TempDir
        Path tempDir;

        File validIn;

        @BeforeEach
        void setUp() throws IOException {
            Path inPath = tempDir.resolve("in.ttl");
            validIn = Files.createFile(inPath).toFile();
        }

        private ConversionContext createMockContext(File outputFile, ConversionMode mode, boolean allowAssert, boolean validateOnly) {
            return new ConversionContext(
                    validIn,
                    outputFile,
                    mode,
                    BaseTriplePolicy.PRESERVE,
                    allowAssert,
                    validateOnly
            );
        }

        @Test
        void shouldPassAndIgnoreOutputWhenValidateOnlyIsActive() {
            File validOut = tempDir.resolve("out.ttl").toFile();
            ConversionContext context = createMockContext(validOut, ConversionMode.REIFIED_TRIPLE_EXPANDED, false, true);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertTrue(result, "Validator should return true and simply ignore the output file when --validate-only is used.");
        }

        @Test
        void shouldPassAndWarnWhenAllowAssertingIsUsedWithNonAnnotatedMode() {
            ConversionContext context = createMockContext(null, ConversionMode.REIFIED_TRIPLE, true, false);
            CliValidator validator = new CliValidator(context);

            boolean result = validator.initialValidator();

            assertTrue(result, "Validator should return true and log a warning when --allow-asserting-conversion is used without ANNOTATED mode.");
        }
    }

    @Nested
    class PicoCliIntegration {
        CommandLine cmd;

        @BeforeEach
        void setUp() {
            Main app = new Main();
            cmd = new CommandLine(app);
        }

        @Test
        void shouldRejectExecutionWhenInvalidModeIsProvided() {
            int exitCode = cmd.execute("--input", "out.ttl", "--mode", "FAKE_MODE");

            assertEquals(2, exitCode, "PicoCLI should return exit code 2 when an invalid conversion mode is provided.");
        }

        @Test
        void shouldRejectExecutionWhenInvalidBaseTriplePolicyIsProvided() {
            int exitCode = cmd.execute("--input", "out.ttl", "--base-triple-policy", "FAKE_POLICY");

            assertEquals(2, exitCode, "PicoCLI should return exit code 2 when an invalid base triple policy is provided.");
        }

        @Test
        void shouldRejectExecutionWhenInputFlagIsMissing() {
            int exitCode = cmd.execute("--mode", "REIFIED_TRIPLE");

            assertEquals(2, exitCode, "PicoCLI should return exit code 2 when required --input flag is missing.");
        }
    }

}