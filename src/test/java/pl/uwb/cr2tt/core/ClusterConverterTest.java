package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.uwb.cr2tt.model.ConversionMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterConverterTest {

    private ClusterExtractor extractor;
    private ClusterConverter converter;

    @BeforeEach
    public void setUp() {
        extractor = new ClusterExtractor();
        converter = new ClusterConverter();
    }

    @Test
    public void testReifiedTripleExpandedConversion() {
        Model inputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(inputModel, "src/test/resources/examples/classic/multi-metadata.ttl", Lang.TURTLE);

        Model expectedModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(expectedModel, "src/test/resources/examples/expected/reified-triple-expanded-expected.ttl", Lang.TURTLE);

        Model outGraph = ModelFactory.createDefaultModel();

        extractor.extractAndProcess(inputModel, cluster -> {
            converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE_EXPANDED, outGraph);
        });

        assertTrue(outGraph.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }

    @Test
    public void testReifiedTripleConversion() {
        Model inputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(inputModel, "src/test/resources/examples/classic/blank-reifier.ttl", Lang.TURTLE);

        Model expectedModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(expectedModel, "src/test/resources/examples/expected/reified-triple-expected.ttl", Lang.TURTLE);

        Model outGraph = ModelFactory.createDefaultModel();

        extractor.extractAndProcess(inputModel, cluster -> {
            converter.convertCluster(cluster, ConversionMode.REIFIED_TRIPLE, outGraph);
        });

        assertTrue(outGraph.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }

    @Test
    public void testReiifedTripleExplicitConverion() {
        Model inputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(inputModel, "src/test/resources/examples/classic/multi-metadata.ttl", Lang.TURTLE);

        Model expectedModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(expectedModel, "src/test/resources/examples/expected/reified-triple-explicit-expected.ttl", Lang.TURTLE);

        Model outGraph = ModelFactory.createDefaultModel();

        extractor.extractAndProcess(inputModel, cluster -> {
            converter.convertCluster(cluster,ConversionMode.REIFIED_TRIPLE_EXPLICIT, outGraph);
        });

        assertTrue(outGraph.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }

    @Test
    public void testAnnotatedTripleConversion() {
        Model inputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(inputModel, "src/test/resources/examples/classic/blank-reifier.ttl", Lang.TURTLE);

        Model expectedModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(expectedModel, "src/test/resources/examples/expected/annotated-triple-expected.ttl", Lang.TURTLE);

        Model outGraph = ModelFactory.createDefaultModel();

        extractor.extractAndProcess(inputModel, cluster -> {
            converter.convertCluster(cluster,ConversionMode.ANNOTATED_TRIPLE, outGraph);
        });

        assertTrue(outGraph.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }

    @Test
    public void testAnnotatedTripleExplicitConversion() {
        Model inputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(inputModel, "src/test/resources/examples/classic/multi-metadata.ttl", Lang.TURTLE);

        Model expectedModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(expectedModel, "src/test/resources/examples/expected/annotated-triple-explicit-expected.ttl", Lang.TURTLE);

        Model outGraph = ModelFactory.createDefaultModel();

        extractor.extractAndProcess(inputModel, cluster -> {
            converter.convertCluster(cluster,ConversionMode.ANNOTATED_TRIPLE_EXPLICIT, outGraph);
        });

        assertTrue(outGraph.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }

    @Test
    public void testAnnotatedTripleExpandedConversion() {
        Model inputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(inputModel, "src/test/resources/examples/classic/multi-metadata.ttl", Lang.TURTLE);

        Model expectedModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(expectedModel, "src/test/resources/examples/expected/annotated-triple-expanded-expected.ttl", Lang.TURTLE);

        Model outGraph = ModelFactory.createDefaultModel();

        extractor.extractAndProcess(inputModel, cluster -> {
            converter.convertCluster(cluster,ConversionMode.ANNOTATED_TRIPLE_EXPANDED, outGraph);
        });

        assertTrue(outGraph.isIsomorphicWith(expectedModel), "Graphs are not isomorphic.");
    }
}