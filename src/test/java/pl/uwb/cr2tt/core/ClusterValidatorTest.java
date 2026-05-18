package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterValidatorTest {
    private ClusterValidator validator;
    private Model inGraph;
    private static final String ns = "http://example.org/";

    private Resource s;
    private Property p;
    private RDFNode o;
    private Cluster baseCluster;

    @BeforeEach
    void setUp() {
        validator = new ClusterValidator();
        inGraph = ModelFactory.createDefaultModel();

        s = inGraph.createResource(ns + "Michael");
        p = inGraph.createProperty(ns + "likes");
        o = inGraph.createResource(ns + "dogs");

        baseCluster = createTestCluster(s, p, o);
        List<Cluster> singleClusterList = List.of(baseCluster);

        validator.initialize(singleClusterList);
    }

    private Cluster createTestCluster(Resource s, Property p, RDFNode o) {
        Resource bNodeReifier = inGraph.createResource();
        Property metaProp = inGraph.createProperty(ns + "meta");
        Statement metadata = inGraph.createStatement(bNodeReifier, metaProp, "test-meta");

        return new Cluster(bNodeReifier, s, p, o, Set.of(metadata));
    }

    @Nested
    class MultiplicityTests {

        @Test
        void shouldThrowExceptionWhenMultipleReificationsInImplicitMode() {
            Cluster c2 = createTestCluster(s, p, o);
            List<Cluster> doubleClusterList = List.of(baseCluster, c2);

            validator.initialize(doubleClusterList);

            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseCluster,
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
            Cluster c2 = createTestCluster(s, p, o);
            List<Cluster> doubleClusterList = List.of(baseCluster, c2);

            validator.initialize(doubleClusterList);

            assertDoesNotThrow(() -> validator.validateCluster(
                    baseCluster,
                    inGraph,
                    ConversionMode.REIFIED_TRIPLE_EXPLICIT,
                    null,
                    false
            ));
        }

        @Test
        void shouldPassWhenSingleReificationInImplicitMode() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseCluster,
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
            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseCluster, inGraph, ConversionMode.REIFIED_TRIPLE,
                            BaseTriplePolicy.REQUIRE, false
                    )
            );
            assertTrue(exception.getMessage().contains("Missing base triple"));
        }

        @Test
        void shouldPassWhenPolicyRequireAndTriplePresent() {
            inGraph.add(s, p, o);

            assertDoesNotThrow(() -> validator.validateCluster(
                    baseCluster, inGraph, ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.REQUIRE, false
            ));
        }

        @Test
        void shouldThrowExceptionWhenPolicyForbidExtraAssertedAndTriplePresent() {
            inGraph.add(s, p, o);

            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseCluster, inGraph, ConversionMode.REIFIED_TRIPLE,
                            BaseTriplePolicy.FORBID_EXTRA_ASSERTED, false
                    )
            );
            assertTrue(exception.getMessage().contains("Triple already asserted"));
        }

        @Test
        void shouldPassWhenPolicyForbidExtraAssertedAndTripleMissing() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseCluster, inGraph, ConversionMode.REIFIED_TRIPLE,
                    BaseTriplePolicy.FORBID_EXTRA_ASSERTED, false
            ));
        }
    }

    @Nested
    class AssertionPermissionTests {

        @Test
        void shouldThrowExceptionWhenAssertingModeAndTripleMissingAndNotAllowed() {
            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseCluster, inGraph,
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
                    baseCluster, inGraph,
                    ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                    null,
                    true
            ));
        }

        @Test
        void shouldPassWhenAssertingModeAndTripleAlreadyPresent() {
            inGraph.add(s, p, o);

            assertDoesNotThrow(() -> validator.validateCluster(
                    baseCluster, inGraph,
                    ConversionMode.ANNOTATED_TRIPLE_EXPANDED,
                    null,
                    false
            ));
        }

        @Test
        void shouldPassWhenNotAssertingMode() {
            assertDoesNotThrow(() -> validator.validateCluster(
                    baseCluster, inGraph,
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
            Cluster iriCluster = new Cluster(iriReifier, s, p, o, Set.of(meta));

            validator.initialize(List.of(iriCluster));

            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            iriCluster, inGraph,
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
            inGraph.add(externalDoc, mentions, baseCluster.getReifier());

            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            baseCluster, inGraph,
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
            Cluster noMetaCluster = new Cluster(bNodeReifier, s, p, o, Set.of());

            validator.initialize(List.of(noMetaCluster));

            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            noMetaCluster, inGraph,
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
            Cluster badCluster = new Cluster(iriReifier, s, p, o, Set.of());

            validator.initialize(List.of(badCluster));

            assertDoesNotThrow(() -> validator.validateCluster(
                    badCluster, inGraph,
                    ConversionMode.REIFIED_TRIPLE_EXPLICIT,
                    null,
                    false
            ));
        }

        @Test
        void shouldThrowExceptionWhenAnnotatedExplicitAndNoMetadata() {
            Resource bNodeReifier = inGraph.createResource();
            Cluster noMetaCluster = new Cluster(bNodeReifier, s, p, o, Set.of());

            validator.initialize(List.of(noMetaCluster));

            ClusterValidator.FatalValidationException exception = assertThrows(
                    ClusterValidator.FatalValidationException.class,
                    () -> validator.validateCluster(
                            noMetaCluster, inGraph,
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
                    baseCluster, inGraph,
                    ConversionMode.ANNOTATED_TRIPLE_EXPLICIT,
                    null,
                    true
            ));
        }
    }
}