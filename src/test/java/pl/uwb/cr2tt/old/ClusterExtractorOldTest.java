package pl.uwb.cr2tt.old;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.old.result.SortResultOld;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClusterExtractorOldTest {
    private ClusterExtractorOld clusterExtractorOld;
    private static final String ns = "http://example.org/";

    @BeforeEach
    void setUp() {
        clusterExtractorOld = new ClusterExtractorOld();
    }

    private Resource createBaseCluster(Model model) {
        Resource reifier = model.createResource(ns + "stmt1");
        model.add(reifier, RDF.subject, model.createResource(ns + "Michael"));
        model.add(reifier, RDF.predicate, model.createProperty(ns + "likes"));
        model.add(reifier, RDF.object, model.createResource(ns + "dogs"));
        model.add(reifier, RDF.type, RDF.Statement);
        return reifier;
    }

    @Nested
    class GraphScanningTests {

        @Test
        void shouldReturnEmptyListWhenGraphIsEmpty() {
            Model inGraph = ModelFactory.createDefaultModel();

            List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

            assertTrue(result.isEmpty(), "Should return an empty list when the input graph is empty");
        }

        @Test
        void shouldIgnoreUnrelatedTriples() {
            Model inGraph = ModelFactory.createDefaultModel();

            Resource michael = inGraph.createResource(ns + "Michael");
            Property likes = inGraph.createProperty(ns + "likes");
            Resource dogs = inGraph.createResource(ns + "dogs");

            inGraph.add(michael, likes, dogs);

            List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

            assertTrue(result.isEmpty(), "Should ignore triples that are not related to reification");
        }

    }

    @Nested
    class ValidationTests {
        Model inGraph;
        Resource reifier;

        @BeforeEach
        void setUpGraph() {
            inGraph = ModelFactory.createDefaultModel();
            reifier = createBaseCluster(inGraph);
        }

        @Nested
        class StatementCountValidationTests {

            @Test
            void shouldSuccessfullyExtractValidCluster() {
                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertEquals(1, result.size(), "Should extract exactly 1 valid cluster");
            }

            @Test
            void shouldSuccessfullyExtractClusterWithoutOptionalType() {
                inGraph.removeAll(reifier, RDF.type, RDF.Statement);

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertEquals(1, result.size(), "Should extract cluster even if rdf:type is missing");
            }

            @Test
            void shouldSkipClusterWithMissingSubject() {
                inGraph.removeAll(reifier, RDF.subject, null);

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster with 0 subjects");
            }

            @Test
            void shouldSkipClusterWithMultipleSubjects() {
                inGraph.add(reifier, RDF.subject, inGraph.createResource(ns + "Peter"));

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster with multiple subjects");
            }


            @Test
            void shouldSkipClusterWithMissingPredicate() {
                inGraph.removeAll(reifier, RDF.predicate, null);

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster with 0 predicates");
            }

            @Test
            void shouldSkipClusterWithMultiplePredicates() {
                inGraph.add(reifier, RDF.predicate, inGraph.createProperty(ns + "has"));

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster with multiple predicates");
            }


            @Test
            void shouldSkipClusterWithMissingObject() {
                inGraph.removeAll(reifier, RDF.object, null);

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster with 0 objects");
            }

            @Test
            void shouldSkipClusterWithMultipleObjects() {
                inGraph.add(reifier, RDF.object, inGraph.createResource(ns + "cats"));

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster with multiple objects");
            }
        }

        @Nested
        class NodeTypeValidationTests {

            @Test
            void shouldSkipClusterWhenSubjectIsLiteral() {
                inGraph.removeAll(reifier, RDF.subject, null);
                inGraph.add(reifier, RDF.subject, inGraph.createLiteral("Plain text"));

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster if rdf:subject is a Literal");
            }

            @Test
            void shouldSkipClusterWhenPredicateIsBlankNode() {
                inGraph.removeAll(reifier, RDF.predicate, null);
                inGraph.add(reifier, RDF.predicate, inGraph.createResource());

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster if rdf:predicate is a Blank Node");
            }

            @Test
            void shouldSkipClusterWhenPredicateIsLiteral() {
                inGraph.removeAll(reifier, RDF.predicate, null);
                inGraph.add(reifier, RDF.predicate, inGraph.createLiteral("likes"));

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertTrue(result.isEmpty(), "Should skip cluster if rdf:predicate is a Literal");
            }

            @Test
            void shouldAcceptClusterWhenObjectIsLiteral() {
                inGraph.removeAll(reifier, RDF.object, null);
                inGraph.add(reifier, RDF.object, inGraph.createTypedLiteral(30));

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertEquals(1, result.size(), "Should extract cluster when rdf:object is a Literal");
            }

            @Test
            void shouldAcceptClusterWhenObjectIsBlankNode() {
                inGraph.removeAll(reifier, RDF.object, null);
                inGraph.add(reifier, RDF.object, inGraph.createResource());

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertEquals(1, result.size(), "Should extract cluster when rdf:object is a Blank Node");
            }
        }

        @Nested
        class MetadataExtractionTests {

            @Test
            void shouldExtractAdditionalMetadata() {
                Property dateProp = inGraph.createProperty(ns + "createdDate");
                Property authorProp = inGraph.createProperty(ns + "author");

                inGraph.add(reifier, dateProp, "2023-10-25");
                inGraph.add(reifier, authorProp, "Admin");

                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertEquals(1, result.size());
                ClusterOld clusterOld = result.getFirst();

                assertEquals(2, clusterOld.getMetadata().size(), "Should extract exactly 2 metadata statements");
            }

            @Test
            void shouldHaveEmptyMetadataWhenOnlyStandardPropertiesExist() {
                List<ClusterOld> result = clusterExtractorOld.findValidClusters(inGraph);

                assertEquals(1, result.size());

                assertTrue(result.getFirst().getMetadata().isEmpty(),
                        "Metadata set should be empty when no extra properties are present");
            }
        }
    }

    @Nested
    class TopologicalSortTests {
        private Model model;

        @BeforeEach
        void setUpGraph() {
            model = ModelFactory.createDefaultModel();
        }

        @Test
        void shouldSortSimpleNestedClustersBottomUp() {
            Resource r1 = model.createResource(ns + "stmt1");
            ClusterOld c1 = new ClusterOld(r1, model.createResource(ns + "Michael"),
                    model.createProperty(ns + "likes"), model.createResource(ns + "dogs"), new HashSet<>());

            Resource r2 = model.createResource(ns + "stmt2");
            ClusterOld c2 = new ClusterOld(r2, model.createResource(ns + "Peter"),
                    model.createProperty(ns + "says"), r1, new HashSet<>());

            SortResultOld result = clusterExtractorOld.topologicalSort(List.of(c1, c2));

            assertEquals(2, result.getSortedClusters().size());
            assertEquals(c1, result.getSortedClusters().get(0), "C1 (deepest) should be first");
            assertEquals(c2, result.getSortedClusters().get(1), "C2 (parent) should be second");
            assertTrue(result.getCycles().isEmpty());
        }

        @Test
        void shouldHandleMetadataDependencies() {
            Resource r1 = model.createResource(ns + "stmt1");
            Resource r2 = model.createResource(ns + "stmt2");
            Property source = model.createProperty(ns + "source");

            ClusterOld c2 = new ClusterOld(r2, model.createResource(ns + "X"),
                    model.createProperty(ns + "p"), model.createResource(ns + "Y"), new HashSet<>());

            Set<Statement> meta = new HashSet<>();
            meta.add(model.createStatement(r1, source, r2));

            ClusterOld c1 = new ClusterOld(r1, model.createResource(ns + "A"),
                    model.createProperty(ns + "b"), model.createResource(ns + "C"), meta);

            SortResultOld result = clusterExtractorOld.topologicalSort(List.of(c1, c2));

            assertEquals(c2, result.getSortedClusters().get(0), "Dependency in metadata should force C2 to be first");
            assertEquals(c1, result.getSortedClusters().get(1));
        }

        @Test
        void shouldDetectAndSkipSimpleCycle() {
            Resource r1 = model.createResource(ns + "stmt1");
            Resource r2 = model.createResource(ns + "stmt2");

            ClusterOld c1 = new ClusterOld(r1, model.createResource(ns + "Michael"),
                    model.createProperty(ns + "p"), r2, new HashSet<>());

            ClusterOld c2 = new ClusterOld(r2, model.createResource(ns + "Peter"),
                    model.createProperty(ns + "p"), r1, new HashSet<>());

            SortResultOld result = clusterExtractorOld.topologicalSort(List.of(c1, c2));

            assertTrue(result.getSortedClusters().isEmpty(), "No clusters should be sorted if they are in a cycle");
            assertEquals(2, result.getCycles().size(), "Both clusters should be identified as part of a cycle");
        }

        @Test
        void shouldKeepValidClustersWhenSeparateCycleExists() {
            Resource r1 = model.createResource(ns + "stmt1");
            Resource r2 = model.createResource(ns + "stmt2");
            Resource r3 = model.createResource(ns + "stmt3");

            ClusterOld c1 = new ClusterOld(r1, model.createResource(ns + "A"),
                    model.createProperty(ns + "p"), model.createResource(ns + "B"), new HashSet<>());

            ClusterOld c2 = new ClusterOld(r2, r3, model.createProperty(ns + "p"),
                    model.createResource(ns + "C"), new HashSet<>());

            ClusterOld c3 = new ClusterOld(r3, r2, model.createProperty(ns + "p"),
                    model.createResource(ns + "D"), new HashSet<>());

            SortResultOld result = clusterExtractorOld.topologicalSort(List.of(c1, c2, c3));

            assertEquals(1, result.getSortedClusters().size());
            assertEquals(c1, result.getSortedClusters().getFirst(), "C1 should be processed normally");
            assertEquals(2, result.getCycles().size(), "C2 and C3 should be caught in cycles list");
        }
    }

}
