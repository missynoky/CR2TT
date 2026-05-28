package pl.uwb.cr2tt.io;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;

public class DatasetExporter {

    public void exportData(Model outGraph, File outputFile) {
        if (outputFile != null) {
            Logger.info("exporting data to RDF file: " + outputFile.getAbsolutePath());

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                RDFDataMgr.write(out, outGraph, RDFFormat.TURTLE_BLOCKS);

                Logger.info("export finished successfully.");

            } catch (Exception e) {
                throw new RuntimeException("Failed to export dataset to file: " + outputFile.getName(), e);
            }
        } else {
            Logger.info("exporting data to standard output.");

            try {
                RDFDataMgr.write(System.out, outGraph, RDFFormat.TURTLE_BLOCKS);
                System.out.flush();

                Logger.info("export finished successfully.");
            } catch (Exception e) {
                throw new RuntimeException("Failed to export dataset to standard output", e);
            }
        }
    }
}