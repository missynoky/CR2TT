package pl.uwb.cr2tt.io;

import pl.uwb.cr2tt.utils.Logger;
import java.io.File;

public class DatasetImporter {

    public void importData(File dbDirectory, File inputFile) {
        Logger.info("starting import of RDF file into TDB2 database.");
        Logger.info("source file: " + inputFile.getAbsolutePath());
        Logger.info("target TDB2 location: " + dbDirectory.getAbsolutePath());

        try {
            tdb2.tdbloader.main(new String[]{
                    "--loc=" + dbDirectory.getAbsolutePath(),
                    inputFile.getAbsolutePath()
            });

            Logger.info("import finished successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to import dataset from file: " + inputFile.getName(), e);
        }
    }
}