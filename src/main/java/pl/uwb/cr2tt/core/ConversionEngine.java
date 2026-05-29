package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import pl.uwb.cr2tt.db.DatasetManager;
import pl.uwb.cr2tt.io.DatasetExporter;
import pl.uwb.cr2tt.io.DatasetImporter;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionContext;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;
import pl.uwb.cr2tt.utils.TimeUtils;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversionEngine {
    private final ConversionContext context;

    public ConversionEngine(ConversionContext context) {
        this.context = context;
    }

    public void run() {
        File inputFile = context.getInputFile();
        File outputFile = context.getOutputFile();
        boolean validateOnly = context.isValidateOnly();
        boolean allowAssertingConversion = context.isAllowAssertingConversion();
        ConversionMode mode = context.getMode();
        BaseTriplePolicy baseTriplePolicy = context.getBaseTriplePolicy();

        Instant startTime = Instant.now();
        Logger.info("initializing temporary databases.");

        File inDbDir = createTempDir("cr2tt_in_db");
        File outDbDir = createTempDir("cr2tt_out_db");

        DatasetManager inDb = new DatasetManager(inDbDir);
        DatasetManager outDb = new DatasetManager(outDbDir);

        try {
            DatasetImporter importer = new DatasetImporter();
            importer.importData(inDbDir, inputFile);

            inDb.beginRead();
            outDb.beginWrite();

            Model inGraph = inDb.getDefaultModel();
            Model outGraph = outDb.getDefaultModel();

            Logger.info("copying namespace prefixes.");
            outGraph.setNsPrefixes(inGraph.getNsPrefixMap());

            ClusterExtractor extractor = new ClusterExtractor();
            ClusterValidator validator = new ClusterValidator();
            ClusterConverter converter = new ClusterConverter();
            StandardTriplesMigrator migrator = new StandardTriplesMigrator();

            Model tombstoneGraph = outDb.getNamedModel("urn:cr2tt:temp:tombstones");
            AtomicInteger validCounter = new AtomicInteger(0);
            AtomicInteger invalidCounter = new AtomicInteger(0);
            Map<String, Integer> errorSummary = new ConcurrentHashMap<>();

            Logger.info("starting extraction, validation and conversion process.");

            extractor.extractAndProcess(inGraph, cluster -> {
                String errorReason = validator.validateCluster(cluster, mode, baseTriplePolicy, allowAssertingConversion);

                if (errorReason == null) {
                    validCounter.incrementAndGet();

                    if (!validateOnly) {
                        converter.convertCluster(cluster, mode, outGraph);
                        markClusterAsProcessed(cluster, tombstoneGraph);
                    }
                } else {
                    invalidCounter.incrementAndGet();
                    errorSummary.merge(errorReason, 1, Integer::sum);
                }
            });

            Logger.info("extraction, validation and conversion process finished.");

            Logger.info("valid clusters: " + validCounter.get());
            Logger.info("invalid clusters: " + invalidCounter.get());

            if (invalidCounter.get() > 0) {
                Logger.info("Reasons for rejection:");
                errorSummary.forEach((reason, count) -> {
                    Logger.info(reason + ": " + count);
                });
            }

            if (!validateOnly) {
                migrator.migrate(inGraph, outGraph, tombstoneGraph);

                DatasetExporter exporter = new DatasetExporter();
                exporter.exportData(outGraph, outputFile);

                Logger.info("committing converted data to the output database.");
                outDb.commit();
                Logger.info("transaction committed. Output database updated successfully.");

            } else {
                Logger.info("validate-only active. Skipping migration and export.");
                outDb.abort();
            }

        } catch (RuntimeException ex) {
            outDb.abort();
            throw ex;

        } catch (Exception ex) {
            outDb.abort();
            throw new RuntimeException("unexpected error during processing: " + ex.getMessage(), ex);

        } finally {
            inDb.close();
            outDb.close();

            deleteDir(inDbDir);
            deleteDir(outDbDir);
            Logger.info("temporary database files cleaned up successfully.");

            Instant endTime = Instant.now();
            Logger.info("total execution time: " + TimeUtils.getExecutionTimeFormatted(startTime, endTime));
        }
    }

    private void markClusterAsProcessed(Cluster cluster, Model tombstoneGraph) {
        Resource cNode = cluster.getClusterNode();
        tombstoneGraph.add(cNode, RDF.subject, cluster.getSubjectNode());
        tombstoneGraph.add(cNode, RDF.predicate, cluster.getPredicateNode());
        tombstoneGraph.add(cNode, RDF.object, cluster.getObjectNode());
        tombstoneGraph.add(cNode, RDF.type, RDF.Statement);

        for (Statement metaStmt : cluster.getMetadata()) {
            tombstoneGraph.add(metaStmt);
        }
    }

    private File createTempDir(String prefix) {
        try {
            File tempDir = Files.createTempDirectory(prefix).toFile();
            tempDir.deleteOnExit();
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary directory for database", e);
        }
    }

    private void deleteDir(File file) {
        if (file != null && file.exists()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteDir(f);
                }
            }
            file.delete();
        }
    }
}