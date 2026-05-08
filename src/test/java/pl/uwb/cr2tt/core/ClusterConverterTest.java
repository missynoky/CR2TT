package pl.uwb.cr2tt.core;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterConverterTest {
    private final String EX_NS = "http://example.org/";

    @Nested
    class ExplicitReifierTests {
        private Cluster inputCluster;
        private final String EXPECTED_REIFIED_TRIPLE_EXPANDED =
                "PREFIX ex: <" + EX_NS + "> " +
                        "PREFIX rdf: <" + RDF.uri + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                        "ex:stmt1 ex:accordingTo ex:Source1 . " +
                        "ex:stmt1 ex:confidence 0.9 . " +
                        "ex:stmt1 rdf:reifies <<( ex:Jan ex:knows ex:Anna )>> .";

        @BeforeEach
        public void setUp() {
            Model tempModel = ModelFactory.createDefaultModel();

            Resource stmt1 = tempModel.createResource(EX_NS + "stmt1");
            Resource jan = tempModel.createResource(EX_NS + "Jan");
            Property knows = tempModel.createProperty(EX_NS + "knows");
            Resource anna = tempModel.createResource(EX_NS + "Anna");

            Property accordingTo = tempModel.createProperty(EX_NS + "accordingTo");
            Resource source1 = tempModel.createResource(EX_NS + "Source1");
            Property confidence = tempModel.createProperty(EX_NS + "confidence");
            Literal confValue = tempModel.createTypedLiteral("0.9", XSDDatatype.XSDdecimal);

            Statement meta1 = tempModel.createStatement(stmt1, accordingTo, source1);
            Statement meta2 = tempModel.createStatement(stmt1, confidence, confValue);

            inputCluster = new Cluster(stmt1, jan, knows, anna, Set.of(meta1, meta2));
        }

        @Test
        public void testConvertCluster_ReifiedTripleExpanded() {
            System.out.println("REIFIED_TRIPLE_EXPANDED");

            Model expectedModel = ModelFactory.createDefaultModel();
            expectedModel.read(new StringReader(EXPECTED_REIFIED_TRIPLE_EXPANDED), null, "TURTLE");

            ClusterConverter converter = new ClusterConverter();
            Model actualModel = converter.convertCluster(inputCluster, ConversionMode.REIFIED_TRIPLE_EXPANDED);

            boolean isCorrect = expectedModel.isIsomorphicWith(actualModel);

            if (isCorrect) {
                System.out.println("Graphs are isomorphic.\n");
            } else {
                System.out.println("Graphs differ from each other.\n");
            }

            System.out.println("Expected:");
            RDFDataMgr.write(System.out, expectedModel, RDFFormat.TURTLE_FLAT);

            System.out.println("\nAfter conversion:");
            actualModel.setNsPrefix("ex", EX_NS);
            actualModel.setNsPrefix("rdf", RDF.uri);
            actualModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
            RDFDataMgr.write(System.out, actualModel, RDFFormat.TURTLE_FLAT);

            assertTrue(isCorrect, "The generated RDF model does not match the Turtle pattern.");
        }
    }

    @Nested
    class BlankReifierTests {
        private Cluster inputCluster;
        private final String EXPECTED_REIFIED_TRIPLE =
                "PREFIX ex: <" + EX_NS + "> " +
                        "PREFIX rdf: <" + RDF.uri + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                        "<< ex:Jan ex:knows ex:Anna >> ex:accordingTo ex:Source1 ; " +
                        "                              ex:confidence 0.9 . ";

        @BeforeEach
        public void setUp() {
            Model tempModel = ModelFactory.createDefaultModel();

            Resource stmtBNode = tempModel.createResource();

            Resource jan = tempModel.createResource(EX_NS + "Jan");
            Property knows = tempModel.createProperty(EX_NS + "knows");
            Resource anna = tempModel.createResource(EX_NS + "Anna");

            Property accordingTo = tempModel.createProperty(EX_NS + "accordingTo");
            Resource source1 = tempModel.createResource(EX_NS + "Source1");
            Statement meta1 = tempModel.createStatement(stmtBNode, accordingTo, source1);

            Property confidence = tempModel.createProperty(EX_NS + "confidence");
            Literal confValue = tempModel.createTypedLiteral("0.9", XSDDatatype.XSDdecimal);
            Statement meta2 = tempModel.createStatement(stmtBNode, confidence, confValue);

            inputCluster = new Cluster(stmtBNode, jan, knows, anna, Set.of(meta1, meta2));
        }

        @Test
        public void testConvertCluster_ReifiedTriple() {
            System.out.println("\nREIFIED_TRIPLE");

            Model expectedModel = ModelFactory.createDefaultModel();
            expectedModel.read(new StringReader(EXPECTED_REIFIED_TRIPLE), null, "TURTLE");

            ClusterConverter converter = new ClusterConverter();
            Model actualModel = converter.convertCluster(inputCluster, ConversionMode.REIFIED_TRIPLE);

            boolean isCorrect = expectedModel.isIsomorphicWith(actualModel);

            if (isCorrect) {
                System.out.println("Graphs are isomorphic.\n");
            } else {
                System.out.println("Graphs differ from each other.\n");
            }

            System.out.println("Expected:");
            RDFDataMgr.write(System.out, expectedModel, RDFFormat.TURTLE);

            System.out.println("\nAfter conversion:");
            actualModel.setNsPrefix("ex", EX_NS);
            actualModel.setNsPrefix("rdf", RDF.uri);
            RDFDataMgr.write(System.out, actualModel, RDFFormat.TURTLE);

            assertTrue(isCorrect, "The generated RDF model does not match the Turtle pattern.");
        }
    }
}