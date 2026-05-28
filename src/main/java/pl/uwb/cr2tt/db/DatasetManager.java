package pl.uwb.cr2tt.db;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb2.TDB2Factory;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;

public class DatasetManager {
    private final File dbDirectory;
    private Dataset dataset;

    public DatasetManager(File dbDirectory) {
        this.dbDirectory = dbDirectory;
    }

    public void beginRead() {
        Logger.info("connecting to TDB2 database at: " + dbDirectory.getAbsolutePath());
        dataset = TDB2Factory.connectDataset(dbDirectory.getAbsolutePath());
        dataset.begin(ReadWrite.READ);
    }


    public void beginWrite() {
        Logger.info("connecting to TDB2 database at: " + dbDirectory.getAbsolutePath());
        dataset = TDB2Factory.connectDataset(dbDirectory.getAbsolutePath());
        dataset.begin(ReadWrite.WRITE);
    }

    public Model getDefaultModel() {
        if (dataset == null) {
            throw new IllegalStateException("Dataset is not connected. Call beginRead() or beginWrite() first.");
        }
        return dataset.getDefaultModel();
    }

    public Model getNamedModel(String graphUri) {
        if (dataset == null) {
            throw new IllegalStateException("Dataset is not connected. Call beginRead() or beginWrite() first.");
        }
        return dataset.getNamedModel(graphUri);
    }

    public void commit() {
        if (dataset != null && dataset.isInTransaction()) {
            Logger.info("committing transaction for database: " + dbDirectory.getName());
            dataset.commit();
        }
    }

    public void abort() {
        if (dataset != null && dataset.isInTransaction()) {
            Logger.warn("aborting transaction. Rolling back changes for database: " + dbDirectory.getName());
            dataset.abort();
        }
    }

    public void close() {
        if (dataset != null) {
            dataset.end();
            dataset.close();
            Logger.info("database connection closed safely: " + dbDirectory.getName());
            dataset = null;
        }
    }

    public File getDbDirectory() {
        return dbDirectory;
    }
}