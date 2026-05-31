package pl.uwb.cr2tt.io;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import pl.uwb.cr2tt.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DatasetExporter {

    public void exportData(Dataset outDataset, File inputFile, File outputFile) {
        RDFFormat format = determineFormat(inputFile, outputFile);

        if (outputFile != null) {
            Logger.info("exporting data to RDF file: " + outputFile.getAbsolutePath());

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                writeBasedOnFormat(out, outDataset, format);
                Logger.info("export finished successfully.");

            } catch (Exception e) {
                throw new RuntimeException("Failed to export dataset to file: " + outputFile.getName(), e);
            }
        } else {
            Logger.info("exporting data to standard output.");

            try {
                writeBasedOnFormat(System.out, outDataset, format);
                System.out.flush();
                Logger.info("export finished successfully.");

            } catch (Exception e) {
                throw new RuntimeException("Failed to export dataset to standard output", e);
            }
        }
    }

    private void writeBasedOnFormat(OutputStream out, Dataset dataset, RDFFormat format) {
        if (format.getLang().equals(Lang.TURTLE) || format.getLang().equals(Lang.NTRIPLES)) {
            RDFDataMgr.write(out, dataset.getDefaultModel(), format);
        } else {
            RDFDataMgr.write(out, dataset, format);
        }
    }

    private RDFFormat determineFormat(File inputFile, File outputFile) {
        File fileToCheck = (outputFile != null) ? outputFile : inputFile;

        if (fileToCheck == null) {
            return RDFFormat.TURTLE_BLOCKS;
        }

        String fileName = fileToCheck.getName().toLowerCase();
        if (fileName.endsWith(".trig")) {
            return RDFFormat.TRIG_BLOCKS;
        } else if (fileName.endsWith(".nq")) {
            return RDFFormat.NQUADS;
        } else if (fileName.endsWith(".nt")) {
            return RDFFormat.NTRIPLES;
        }

        return RDFFormat.TURTLE_BLOCKS;
    }
}