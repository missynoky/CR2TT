package pl.uwb.cr2tt.old;

import org.apache.jena.rdf.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.ConversionMode;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterCliValidatorOldTest {
    private ClusterValidatorOld validator;
    private Model inGraph;
    private static final String ns = "http://example.org/";

    private Resource s;
    private Property p;
    private RDFNode o;
    private ClusterOld baseClusterOld;

    @BeforeEach
    void setUp() {
        validator = new ClusterValidatorOld();
        inGraph = ModelFactory.createDefaultModel();

        s = inGraph.createResource(ns + "Michael");
        p = inGraph.createProperty(ns + "likes");
        o = inGraph.createResource(ns + "dogs");

        baseClusterOld = createTestCluster(s, p, o);
        List<ClusterOld> singleClusterOldList = List.of(baseClusterOld);

        validator.initialize(singleClusterOldList);
    }

    private ClusterOld createTestCluster(Resource s, Property p, RDFNode o) {
        Resource bNodeReifier = inGraph.createResource();
        Property metaProp = inGraph.createProperty(ns + "meta");
        Statement metadata = inGraph.createStatement(bNodeReifier, metaProp, "test-meta");

        return new ClusterOld(bNodeReifier, s, p, o, Set.of(metadata));
    }

    @Nested
    class MultiplicityTests {

        @Test
        void shouldThrowExceptionWhenMultipleReificationsInImplicitMode() {
            ClusterOld c2 = createTestCluster(s, p, o);
            List<ClusterOld> doubleClusterOldList = List.of(baseClusterOld, c2);

            validator.initialize(doubleClusterOldList);

            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseClusterOld,
                            inGraph,
                            ConversionMode.REIFIED_TRIPLE,
                            null,
                            false
                    )
            );
            assertTrue(exception.getMessage().contains("Multiple reifications"));
        }

        @Test
        void shouldPassWhenMultipleReificationsInExplicitMode() {
            ClusterOld c2 = createTestCluster(s, p, o);
            List<ClusterOld> doubleClusterOldList = List.of(baseClusterOld, c2);

            validator.initialize(doubleClusterOldList);

            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld,
                    inGraph,
                    ConversionMode.REIFIED_TRIPLE_EXPLICIT,
                    null,
                    false
            ));
        }

        @Test
        void shouldPassWhenSingleReificationInImplicitMode() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld,
                    inGraph,
                    ConversionMode.REIFIED_TRIPLE,
                    null,
                    false
            ));
        }
    }

    @Nested
    class BaseTriplePolicyTests {

        @Test
        void shouldThrowExceptionWhenPolicyRequireAndTripleMissing() {
            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseClusterOld, inGraph, ConversionMode.REIFIED_TRIPLE,
                            BaseTriplePolicy.REQUIRE, false
                    )
            );
            assertTrue(exception.getMessage().contains("Missing base triple"));
        }

        @Test
        void shouldPassWhenPolicyRequireAndTriplePresent() {
            inGraph.add(s, p, o);

            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld, inGraph, ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.REQUIRE, false
            ));
        }

        @Test
        void shouldThrowExceptionWhenPolicyForbidExtraAssertedAndTriplePresent() {
            inGraph.add(s, p, o);

            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseClusterOld, inGraph, ConversionMode.REIFIED_TRIPLE,
                            BaseTriplePolicy.FORBID_EXTRA_ASSERTED, false
                    )
            );
            assertTrue(exception.getMessage().contains("Triple already asserted"));
        }

        @Test
        void shouldPassWhenPolicyForbidExtraAssertedAndTripleMissing() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld, inGraph, ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.FORBID_EXTRA_ASSERTED, false
            ));
        }
    }

    @Nested
    class AssertionPermissionTests {

        @Test
        void shouldThrowExceptionWhenAssertingModeAndTripleMissingAndNotAllowed() {
            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseClusterOld, inGraph,
                            ConversionMode.ANNOTATED_TRIPLE,
                            null,
                            false
                    )
            );
            assertTrue(exception.getMessage().contains("Assertion not allowed"));
        }

        @Test
        void shouldPassWhenAssertingModeAndTripleMissingButAllowedByFlag() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld, inGraph,
                    ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                    null,
                    true
            ));
        }

        @Test
        void shouldPassWhenAssertingModeAndTripleAlreadyPresent() {
            inGraph.add(s, p, o);

            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld, inGraph,
                    ConversionMode.ANNOTATED_TRIPLE_EXPANDED,
                    null,
                    false
            ));
        }

        @Test
        void shouldPassWhenNotAssertingMode() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld, inGraph,
                    ConversionMode.REIFIED_TRIPLE,
                    null,
                    false
            ));
        }
    }

    @Nested
    class NodeAndMetadataTests {

        @Test
        void shouldThrowExceptionWhenImplicitModeAndReifierIsIri() {
            Resource iriReifier = inGraph.createResource(ns + "stmt1");
            Property metaProp = inGraph.createProperty(ns + "meta");
            Statement meta = inGraph.createStatement(iriReifier, metaProp, "val");
            ClusterOld iriClusterOld = new ClusterOld(iriReifier, s, p, o, Set.of(meta));

            validator.initialize(List.of(iriClusterOld));

            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            iriClusterOld, inGraph,
                            ConversionMode.REIFIED_TRIPLE,
                            null,
                            false
                    )
            );
            assertTrue(exception.getMessage().contains("Requires blank node, locality and metadata"));
        }

        @Test
        void shouldThrowExceptionWhenImplicitModeAndReifierIsNotLocal() {
            Resource externalDoc = inGraph.createResource(ns + "ExternalDoc");
            Property mentions = inGraph.createProperty(ns + "mentions");
            inGraph.add(externalDoc, mentions, baseClusterOld.getReifier());

            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseClusterOld, inGraph,
                            ConversionMode.ANNOTATED_TRIPLE,
                            null,
                            true
                    )
            );
            assertTrue(exception.getMessage().contains("Requires blank node, locality and metadata"));
        }

        @Test
        void shouldThrowExceptionWhenImplicitModeAndNoMetadata() {
            Resource bNodeReifier = inGraph.createResource();
            ClusterOld noMetaClusterOld = new ClusterOld(bNodeReifier, s, p, o, Set.of());

            validator.initialize(List.of(noMetaClusterOld));

            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            noMetaClusterOld, inGraph,
                            ConversionMode.REIFIED_TRIPLE,
                            null,
                            false
                    )
            );
            assertTrue(exception.getMessage().contains("Requires blank node, locality and metadata"));
        }

        @Test
        void shouldPassWhenExplicitModeEvenIfNotOkBNode() {
            Resource iriReifier = inGraph.createResource(ns + "stmt1");
            ClusterOld badClusterOld = new ClusterOld(iriReifier, s, p, o, Set.of());

            validator.initialize(List.of(badClusterOld));

            assertDoesNotThrow(() -> validator.validateCluster(
                    badClusterOld, inGraph,
                    ConversionMode.REIFIED_TRIPLE_EXPLICIT,
                    null,
                    false
            ));
        }

        @Test
        void shouldThrowExceptionWhenAnnotatedExplicitAndNoMetadata() {
            Resource bNodeReifier = inGraph.createResource();
            ClusterOld noMetaClusterOld = new ClusterOld(bNodeReifier, s, p, o, Set.of());

            validator.initialize(List.of(noMetaClusterOld));

            ClusterValidatorOld.FatalValidationException exception = assertThrows(
                    ClusterValidatorOld.FatalValidationException.class,
                    () -> validator.validateCluster(
                            noMetaClusterOld, inGraph,
                            ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                            null,
                            true
                    )
            );
            assertTrue(exception.getMessage().contains("Requires metadata declaration"));
        }

        @Test
        void shouldPassWhenAnnotatedExplicitAndHasMetadata() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseClusterOld, inGraph,
                    ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                    null,
                    true
            ));
        }
    }
}