package pl.uwb.cr2tt.core;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import pl.uwb.cr2tt.db.DatasetManager;
import pl.uwb.cr2tt.utils.Logger;

import java.util.Iterator;

public class StandardTriplesMigrator {

    public void migrate(DatasetManager inDb, DatasetManager outDb, Model tombstoneGraph) {
        Logger.info("starting to copy regular triples and invalid clusters.");
        long copiedTriples = 0;

        Dataset inDataset = inDb.getDataset();
        Dataset outDataset = outDb.getDataset();

        copiedTriples = copyModel(inDataset.getDefaultModel(), outDataset.getDefaultModel(), tombstoneGraph, outDb, copiedTriples);

        Iterator<String> graphNames = inDataset.listNames();
        while (graphNames.hasNext()) {
            String graphName = graphNames.next();
            copiedTriples = copyModel(inDataset.getNamedModel(graphName), outDataset.getNamedModel(graphName), tombstoneGraph, outDb, copiedTriples);
        }

        Logger.info("finished copying. Total copied triples: " + copiedTriples);
        tombstoneGraph.removeAll();
        Logger.info("temporary tombstone graph cleared.");
    }

    private long copyModel(Model inGraph, Model outGraph, Model tombstoneGraph, DatasetManager outDb, long copiedTriples) {
        StmtIterator it = inGraph.listStatements();
        try {
            while (it.hasNext()) {
                Statement stmt = it.next();
                if (tombstoneGraph.contains(stmt)) continue;

                outGraph.add(stmt);
                copiedTriples++;

                if (copiedTriples % 1000000 == 0) {
                    Logger.info("copied " + copiedTriples + " triples to the output database.");
                    outDb.commit();
                    outDb.beginWrite();
                    outGraph = outDb.getDataset().getNamedModel(outGraph.toString());
                    if (outGraph == null) {
                        outGraph = outDb.getDefaultModel();
                    }
                    tombstoneGraph = outDb.getNamedModel("urn:cr2tt:temp:tombstones");
                }
            }
        } finally {
            it.close();
        }
        return copiedTriples;
    }
}