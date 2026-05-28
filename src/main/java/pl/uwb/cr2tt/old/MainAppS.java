package pl.uwb.cr2tt.old;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.core.ClusterConverter;
import pl.uwb.cr2tt.core.ClusterExtractor;
import pl.uwb.cr2tt.core.ClusterValidator;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.utils.Logger;
import pl.uwb.cr2tt.utils.TimeUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class MainAppS {

    public static void main(String[] args) {
        Instant startTime = Instant.now();
        Logger.init(true);

        String inDbPath = "C:\\Users\\magda\\Desktop\\studia\\Praca magisterska\\program\\data\\baza_tdb2";
        String outDbPath = "C:\\Users\\magda\\Desktop\\studia\\Praca magisterska\\program\\data\\out_baza_tdb2";

        Logger.info("connecting to input TDB2 database at: " + inDbPath);
        Dataset inDataset = TDB2Factory.connectDataset(inDbPath);

        Logger.info("connecting to output TDB2 database at: " + outDbPath);
        Dataset outDataset = TDB2Factory.connectDataset(outDbPath);

        inDataset.begin(ReadWrite.READ);
        outDataset.begin(ReadWrite.WRITE);

        try {
            Model inGraph = inDataset.getDefaultModel();
            Model outGraph = outDataset.getDefaultModel();

            outGraph.setNsPrefixes(inGraph.getNsPrefixMap());

            ClusterExtractor extractor = new ClusterExtractor();
            ClusterValidator validator = new ClusterValidator();
            ClusterConverter converter = new ClusterConverter();

            ConversionMode currentMode = ConversionMode.REIFIED_TRIPLE_EXPANDED;
            BaseTriplePolicy currentPolicy = BaseTriplePolicy.PRESERVE;
            boolean allowAssert = false;

            AtomicInteger validCounter = new AtomicInteger(0);

            Model tombstoneGraph = outDataset.getNamedModel("urn:cr2tt:temp:tombstones");

            Logger.info("starting extraction, validation and conversion process.");

            extractor.extractAndProcess(inGraph, cluster -> {
                boolean isValid = validator.validateCluster(cluster, currentMode, currentPolicy, allowAssert);

                if (isValid) {
                    validCounter.incrementAndGet();
                    converter.convertCluster(cluster, currentMode, outGraph);

                    tombstoneGraph.add(cluster.getClusterNode(), RDF.type, RDF.Statement);
                }
            });
            Logger.info("validation completed successfully for " + validCounter.get() + " clusters.");

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
                        Logger.info("copied " + copiedTriples + " regular triples to the output database.");
                    }
                }
            } finally {
                it.close();
            }
            Logger.info("finished copying. Total copied triples: " + copiedTriples);

            tombstoneGraph.removeAll();

            Logger.info("committing converted data to the output database.");
            outDataset.commit();
            Logger.info("transaction committed. Output database updated successfully.");

        } catch (RuntimeException ex) {
            Logger.error("validation aborted due to an error: " + ex.getMessage());

            if (outDataset.isInTransaction()) {
                outDataset.abort();
            }
            Logger.info("all output changes have been rolled back. Output database is clean.");

        } catch (Exception ex) {
            Logger.error("Error during processing: " + ex.getMessage());
            ex.printStackTrace();

            if (outDataset.isInTransaction()) {
                outDataset.abort();
            }
            Logger.info("all output changes have been rolled back. Output database is clean.");
        } finally {
            inDataset.end();
            inDataset.close();
            outDataset.end();
            outDataset.close();
            Logger.info("connections to all TDB2 databases closed safely.");

            Instant endTime = Instant.now();

            Logger.info("total execution time: " + TimeUtils.getExecutionTimeFormatted(startTime, endTime));
        }
    }
}