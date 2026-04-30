package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClusterExtractor {

    public List<Cluster> extractClusters(Model inGraph) {
        Logger.info("starting extraction of classic reification clusters");
        // Clusters_raw <- Nodes in G_in with any rdf:{subject, predicate, object, Statement}
        Set<Resource> rawClusters = new HashSet<>();

        inGraph.listSubjectsWithProperty(RDF.subject).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.predicate).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.object).forEachRemaining(rawClusters::add);
        inGraph.listSubjectsWithProperty(RDF.type, RDF.Statement).forEachRemaining(rawClusters::add);

        Logger.info("found " + rawClusters.size() + " potential reification nodes.");

        // Clusters_valid <- ∅
        List<Cluster> validClusters = new ArrayList<>();

        // foreach C in Clusters_raw do
        for (Resource c : rawClusters) {

            List<Statement> subjectStmts = c.listProperties(RDF.subject).toList();
            List<Statement> predicateStmts = c.listProperties(RDF.predicate).toList();
            List<Statement> objectStmts = c.listProperties(RDF.object).toList();
            List<Statement> typeStmts = inGraph.listStatements(c, RDF.type, RDF.Statement).toList();

            if (subjectStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:subject (found " + subjectStmts.size() + ")");
                continue;
            }

            if (predicateStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:predicate (found " + predicateStmts.size() + ")");
                continue;
            }

            if (objectStmts.size() != 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Must have exactly one rdf:object (found " + objectStmts.size() + ")");
                continue;
            }

            if (typeStmts.size() > 1) {
                Logger.warn("node " + c.getLocalName() + " skipped: Max one optional rdf:type rdf:Statement (found " + typeStmts.size() + ")");
                continue;
            }

            RDFNode sNode = subjectStmts.getFirst().getObject();
            RDFNode pNode = predicateStmts.getFirst().getObject();
            RDFNode oNode = objectStmts.getFirst().getObject();

            if (!sNode.isResource()) {
                Logger.warn("node " + c.getLocalName() + " skipped: Invalid subject type (must be IRI or blank node)");
                continue;
            }

            if (!pNode.isURIResource()) {
                Logger.warn("node " + c.getLocalName() + " skipped: Invalid predicate type (must be IRI)");
                continue;
            }

            if(!oNode.isResource() && !oNode.isLiteral()) {
                Logger.warn("node " + c.getLocalName() + " skipped: Invalid object type (must be IRI or blank node or literal");
                continue;
            }

            Set<Statement> metadata = new HashSet<>();

            c.listProperties().forEachRemaining(stmt -> {
                Property p = stmt.getPredicate();

                if (p.equals(RDF.subject) || p.equals(RDF.predicate) || p.equals(RDF.object)) {
                    return;
                }

                if (p.equals(RDF.type) && stmt.getObject().equals(RDF.Statement)) {
                    return;
                }

                metadata.add(stmt);
            });

            Cluster cluster = new Cluster (
                c,
                sNode.asResource(),
                pNode.as(Property.class),
                oNode,
                metadata
            );

            validClusters.add(cluster);

        }

        return validClusters;
    }
}