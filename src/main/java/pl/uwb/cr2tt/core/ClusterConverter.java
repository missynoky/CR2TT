package pl.uwb.cr2tt.core;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;


public class ClusterConverter {
    public void convertCluster(Cluster cluster, ConversionMode mode, Model outGraph) {
        switch (mode) {
            case REIFIED_TRIPLE_EXPANDED:
                Node sNode = cluster.getSubject().asNode();
                Node pNode = cluster.getPredicate().asNode();
                Node oNode = cluster.getObject().asNode();

                Node tripleTermNode = NodeFactory.createTripleTerm(sNode, pNode, oNode);

                RDFNode tripleTerm = outGraph.asRDFNode(tripleTermNode);

                Property rdfReifies = RDF.reifies;

                outGraph.add(cluster.getReifier(), rdfReifies, tripleTerm);

                for (Statement metaStmt : cluster.getMetadata()) {
                    outGraph.add(metaStmt);
                }
                break;

            case REIFIED_TRIPLE:
                break;

            case REIFIED_TRIPLE_EXPLICIT:
                break;

            case ANNOTATED_TRIPLE:
                break;

            case ANNOTATED_TRIPLE_EXPLICIT:
                break;

            case ANNOTATED_TRIPLE_EXPANDED:
                break;

            default:
                throw new IllegalArgumentException("Unsupported conversion mode: " + mode);
        }
    }
}
