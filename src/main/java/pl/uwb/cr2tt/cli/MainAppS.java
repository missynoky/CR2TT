package pl.uwb.cr2tt.cli;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb2.TDB2Factory;
import pl.uwb.cr2tt.core.ClusterConverter;
import pl.uwb.cr2tt.core.ClusterExtractor;
import pl.uwb.cr2tt.core.ClusterValidator;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.utils.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class MainAppS {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
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

            ClusterExtractor extractor = new ClusterExtractor();
            ClusterValidator validator = new ClusterValidator();
            ClusterConverter converter = new ClusterConverter();

            ConversionMode currentMode = ConversionMode.REIFIED_TRIPLE;
            BaseTriplePolicy currentPolicy = BaseTriplePolicy.PRESERVE;
            boolean allowAssert = false;

            AtomicInteger validCounter = new AtomicInteger(0);

            Logger.info("starting extraction, validation and conversion process.");

            extractor.extractAndProcess(inGraph, cluster -> {
                boolean isValid = validator.validateCluster(cluster, currentMode, currentPolicy, allowAssert);

                if (isValid) {
                    validCounter.incrementAndGet();
                    converter.convertCluster(cluster, currentMode, outGraph);
                }
            });
            Logger.info("validation completed successfully for " + validCounter.get() + " clusters.");
            Logger.info("committing converted data to the output database.");
            outDataset.commit();
            Logger.info("transaction committed. Output database updated successfully.");

        } catch (RuntimeException ex) {
            Logger.error("validation aborted due to an error: " + ex.getMessage());
            outDataset.abort();
            Logger.info("all output changes have been rolled back. Output database is clean.");

        } catch (Exception e) {
            Logger.error("unexpected error during processing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            inDataset.end();
            inDataset.close();
            outDataset.end();
            outDataset.close();
            Logger.info("connections to all TDB2 databases closed safely.");

            long endTime = System.currentTimeMillis();
            long totalTimeMs = endTime - startTime;
            long totalTimeSec = totalTimeMs / 1000;
            long totalTimeMin = totalTimeSec / 60;
            long remainingSec = totalTimeSec % 60;

            Logger.info("total execution time: " + totalTimeMin + " min " + remainingSec + " sec");
        }
    }
}