package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClusterValidatorTest {

    private ClusterValidator validator;

    private Set<Statement> createMetadata() {
        Statement stmt = ResourceFactory.createStatement(
                ResourceFactory.createResource(),
                ResourceFactory.createProperty("http://example.org/p"),
                ResourceFactory.createPlainLiteral("value")
        );
        return Collections.singleton(stmt);
    }

    @BeforeEach
    void setUp() {
        validator = new ClusterValidator();
    }

    @Nested
    class MultipleReificationsTests {

        private final String EXPECTED_ERROR = "Multiple reifications for same triple require explicit mode";

        @Test
        void shouldReturnErrorWhenMultipleReificationsAndReifiedTripleMode() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    2, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertEquals(EXPECTED_ERROR, result);
        }

        @Test
        void shouldReturnErrorWhenMultipleReificationsAndAnnotatedTripleMode() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    5, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertEquals(EXPECTED_ERROR, result);
        }

        @Test
        void shouldReturnNullWhenMultipleReificationsAndExplicitMode() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    2, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE_EXPLICIT,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertNull(result, "Expected no errors for explicit mode even with nSpo > 1");
        }
    }

    @Nested
    class BaseTriplePolicyTests {

        @Test
        void shouldReturnErrorWhenPolicyRequiresTripleAndItIsMissing() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, false, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.REQUIRE,
                    true
            );

            assertEquals("Missing base triple", result);
        }

        @Test
        void shouldReturnNullWhenPolicyRequiresTripleAndItExists() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.REQUIRE,
                    true
            );

            assertNull(result, "Expected success when required base triple is present");
        }

        @Test
        void shouldReturnErrorWhenPolicyForbidsExtraAssertionAndItExists() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.FORBID_EXTRA_ASSERTED,
                    true
            );

            assertEquals("Triple already asserted", result);
        }

        @Test
        void shouldReturnNullWhenPolicyForbidsExtraAssertionAndItIsMissing() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, false, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.FORBID_EXTRA_ASSERTED,
                    true
            );

            assertNull(result, "Expected success when extra asserted triple is correctly missing");
        }
    }

    @Nested
    class AllowAssertTests {

        @Test
        void shouldReturnErrorWhenAssertionRequiredButNotAllowed() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, false, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.PRESERVE,
                    false
            );

            assertEquals("Assertion not allowed", result);
        }

        @Test
        void shouldReturnNullWhenAssertionRequiredAndAllowed() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, false, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertNull(result, "Expected success when assertion is explicitly allowed");
        }

        @Test
        void shouldReturnNullWhenAssertionNotAllowedButTripleAlreadyExists() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.PRESERVE,
                    false
            );

            assertNull(result, "Expected success when asserting is false but triple is already in the graph");
        }
    }

    @Nested
    class BlankNodeRestrictionTests {
        private final String EXPECTED_ERROR = "Requires blank node, locality and metadata";

        @Test
        void shouldReturnErrorWhenNodeIsIriAndNotBlankNode() {
            Resource testNode = ResourceFactory.createResource("http://example.org/node");

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    createMetadata(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertEquals(EXPECTED_ERROR, result);
        }

        @Test
        void shouldReturnErrorWhenClusterIsNotLocal() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    createMetadata(),
                    1, true, false
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertEquals(EXPECTED_ERROR, result);
        }

        @Test
        void shouldReturnErrorWhenClusterHasNoMetadata() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertEquals(EXPECTED_ERROR, result);
        }

        @Test
        void shouldReturnNullWhenAllBlankNodeConditionsAreMet() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    createMetadata(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertNull(result, "Expected success when blank node, locality, and metadata conditions are strictly met");
        }
    }

    @Nested
    class MetadataDeclarationTests {

        @Test
        void shouldReturnErrorWhenExplicitModeHasNoMetadata() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertEquals("Requires metadata declaration", result);
        }

        @Test
        void shouldReturnNullWhenExplicitModeHasMetadata() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    createMetadata(),
                    1, true, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertNull(result, "Expected success when explicit mode has metadata declared");
        }
    }

    @Nested
    class ValidClusterTests {

        @Test
        void shouldReturnNullForValidReifiedTripleExpanded() {
            Resource testNode = ResourceFactory.createResource("http://example.org/explicitNode");

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    Collections.emptySet(),
                    1, false, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.REIFIED_TRIPLE_EXPANDED,
                    BaseTriplePolicy.PRESERVE,
                    false
            );

            assertNull(result, "Validation should pass");
        }

        @Test
        void shouldReturnNullForValidAnnotatedTriple() {
            Resource testNode = ResourceFactory.createResource();

            Cluster cluster = new Cluster(
                    testNode, null, null, null,
                    createMetadata(),
                    1, false, true
            );

            String result = validator.validateCluster(
                    cluster,
                    ConversionMode.ANNOTATED_TRIPLE,
                    BaseTriplePolicy.PRESERVE,
                    true
            );

            assertNull(result, "Validation should pass");
        }

    }
}