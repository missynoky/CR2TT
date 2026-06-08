package pl.uwb.cr2tt.core;

import org.apache.jena.query.Dataset;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversionEngine {
    private final ConversionContext context;

    public ConversionEngine(ConversionContext context) {
        this.context = context;
    }

    public boolean run() {
        File inputFile = context.getInputFile();
        File outputFile = context.getOutputFile();
        boolean validateOnly = context.isValidateOnly();
        boolean allowAssertingConversion = context.isAllowAssertingConversion();
        ConversionMode mode = context.getMode();
        BaseTriplePolicy baseTriplePolicy = context.getBaseTriplePolicy();
        boolean keepStatementType = context.isKeepStatementType();

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

            Dataset inDataset = inDb.getDataset();
            Dataset outDataset = outDb.getDataset();

            validateOutputFormatForNamedGraphs(inDataset, inputFile, outputFile);

            Logger.info("copying namespace prefixes.");
            outDataset.getDefaultModel().setNsPrefixes(inDataset.getDefaultModel().getNsPrefixMap());

            StandardTriplesMigrator migrator = new StandardTriplesMigrator();

            AtomicInteger validCounter = new AtomicInteger(0);
            AtomicInteger invalidCounter = new AtomicInteger(0);
            Map<String, Integer> errorSummary = new ConcurrentHashMap<>();

            Logger.info("starting extraction, validation and conversion process.");

            String runId = UUID.randomUUID().toString();
            checkGraphNameCollisions(inDataset, runId);

            Logger.info("processing default graph.");
            Model defaultTombstone = outDb.getNamedModel("urn:cr2tt:temp:tombstones:" + runId + ":default");
            processGraph(inDataset.getDefaultModel(), outDataset.getDefaultModel(), mode, baseTriplePolicy,
                    allowAssertingConversion, validateOnly, validCounter, invalidCounter, errorSummary,
                    defaultTombstone, keepStatementType);

            Iterator<String> graphNames = inDataset.listNames();
            while (graphNames.hasNext()) {
                String graphName = graphNames.next();
                Logger.info("processing named graph: " + graphName);

                String namedTombstoneUri = graphName + "-tombstones-" + runId;
                Model namedTombstone = outDb.getNamedModel(namedTombstoneUri);

                processGraph(inDataset.getNamedModel(graphName), outDataset.getNamedModel(graphName),
                        mode, baseTriplePolicy, allowAssertingConversion, validateOnly, validCounter,
                        invalidCounter, errorSummary, namedTombstone, keepStatementType);
            }

            Logger.info("extraction, validation and conversion process finished.");

            Logger.info("valid clusters: " + validCounter.get());
            Logger.info("invalid clusters: " + invalidCounter.get());

            if (invalidCounter.get() > 0) {
                Logger.info("reasons for rejection:");
                errorSummary.forEach((reason, count) -> {
                    Logger.info(reason + ": " + count);
                });
            }

            if (!validateOnly) {
                Logger.info("saving converted clusters.");
                outDb.commit();
                Logger.info("clusters saved successfully.");

                outDb.beginWrite();

                migrator.migrate(inDb, outDb, runId);

                DatasetExporter exporter = new DatasetExporter();
                exporter.exportData(outDb.getDataset(), inputFile, outputFile);

                Logger.info("committing final dataset to database.");
                outDb.commit();
                Logger.info("transaction committed. Output database updated successfully.");

            } else {
                Logger.info("validate-only active. Skipping migration and export.");
                outDb.abort();
            }

            return invalidCounter.get() == 0;

        } catch (RuntimeException ex) {
            outDb.abort();
            throw ex;

        } catch (Exception ex) {
            outDb.abort();
            throw new RuntimeException("Unexpected error during processing: " + ex.getMessage(), ex);

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

    private void validateOutputFormatForNamedGraphs(Dataset inDataset, File inputFile, File outputFile) {
        File fileToCheck = (outputFile != null) ? outputFile : inputFile;
        String fileName = (fileToCheck != null) ? fileToCheck.getName().toLowerCase() : "";
        boolean isTrigOrNq = fileName.endsWith(".trig") || fileName.endsWith(".nq");

        if (inDataset.listNames().hasNext() && !isTrigOrNq) {
            throw new RuntimeException("The input dataset contains named graphs, " +
                    "but the requested output format only supports a single default graph. " +
                    "Please use .trig or .nq extension for the output file.");
        }
    }

    private void checkGraphNameCollisions(Dataset inDataset, String runId) {
        Iterator<String> graphNames = inDataset.listNames();
        while (graphNames.hasNext()) {
            if (graphNames.next().contains(runId)) {
                throw new RuntimeException("Input graph contains the generated UUID runId.");
            }
        }
    }

    private void processGraph(Model inGraph, Model outGraph, ConversionMode mode, BaseTriplePolicy baseTriplePolicy,
                              boolean allowAssertingConversion, boolean validateOnly,
                              AtomicInteger validCounter, AtomicInteger invalidCounter, Map<String, Integer> errorSummary,
                              Model tombstoneGraph, boolean keepStatementType) {
        ClusterExtractorNew extractor = new ClusterExtractorNew();
        ClusterValidator validator = new ClusterValidator();
        ClusterConverter converter = new ClusterConverter();
        Map<String, Resource> resolvedTripleTerms = new HashMap<>();

        int cyclicCount = extractor.extractAndProcess(inGraph, cluster -> {
            String errorReason = validator.validateCluster(cluster, mode, baseTriplePolicy, allowAssertingConversion);

            if (errorReason == null) {
                validCounter.incrementAndGet();

                if (!validateOnly) {
                    converter.convertCluster(cluster, mode, outGraph, resolvedTripleTerms, keepStatementType);
                    markClusterAsProcessed(cluster, tombstoneGraph);
                }
            } else {
                invalidCounter.incrementAndGet();
                errorSummary.merge(errorReason, 1, Integer::sum);
            }
        });

        if (cyclicCount > 0) {
            invalidCounter.addAndGet(cyclicCount);
            errorSummary.merge("Cyclic reification detected ", cyclicCount, Integer::sum);
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