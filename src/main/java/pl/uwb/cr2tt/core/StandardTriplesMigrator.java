package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.utils.Logger;

public class StandardTriplesMigrator {

    public void migrate(Model inGraph, Model outGraph, Model tombstoneGraph) {
        Logger.info("starting to copy regular triples and invalid clusters.");
        long copiedTriples = 0;

        StmtIterator it = inGraph.listStatements();
        try {
            while (it.hasNext()) {
                Statement stmt = it.next();

                if (tombstoneGraph.contains(stmt.getSubject(), RDF.type, RDF.Statement)) {
                    continue;
                }

                outGraph.add(stmt);
                copiedTriples++;

                if (copiedTriples % 1000000 == 0) {
                    Logger.info("copied " + copiedTriples + " triples to the output database.");
                }
            }
        } finally {
            it.close();
        }

        Logger.info("finished copying. Total copied triples: " + copiedTriples);

        tombstoneGraph.removeAll();
        Logger.info("temporary tombstone graph cleared.");
    }
}