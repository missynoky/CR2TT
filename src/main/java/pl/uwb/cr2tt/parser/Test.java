package pl.uwb.cr2tt.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    static class Variant {
        String outputSuffix;
        String[] flags;

        Variant(String outputSuffix, String... flags) {
            this.outputSuffix = outputSuffix;
            this.flags = flags;
        }
    }

    public static void main(String[] args) {
        String baseDir = System.getProperty("user.dir");

        String jarPath = new File(baseDir, "target/rdf12-reif.jar").getAbsolutePath();
        String inputPath = new File(baseDir, "data/input/dataset_50000_iri.ttl").getAbsolutePath();

        int iterations = 10;
        System.out.println("Start tests. File: " + new File(inputPath).getName());

        Variant[] variants = new Variant[]{
                new Variant("dataset_50000_iri_reified_triple_expanded.ttl"),
                new Variant("dataset_50000_iri_reified_triple_expanded_ks.ttl", "--keep-statement-type"),
                new Variant("dataset_50000_iri_annotated_triple_expanded.ttl", "--mode", "ANNOTATED_TRIPLE_EXPANDED", "--allow-asserting-conversion"),
                new Variant("dataset_50000_iri_annotated_triple_expanded_ks.ttl", "--mode", "ANNOTATED_TRIPLE_EXPANDED", "--allow-asserting-conversion", "--keep-statement-type")
        };

        for (Variant variant : variants) {
            String outputPath = new File(baseDir, "data/output/" + variant.outputSuffix).getAbsolutePath();
            System.out.println("Testing variant: " + variant.outputSuffix);

            for (int i = 0; i <= iterations; i++) {

                String currentOutputPath = outputPath;

                if (i == 0) {
                    System.out.println("Warmup");
                    currentOutputPath = outputPath.replace(".ttl", "_warmup.ttl");
                } else {
                    System.out.println("Iteration: " + i);
                }

                File outFile = new File(currentOutputPath);

                try {
                    outFile.getParentFile().mkdirs();
                    outFile.createNewFile();
                } catch (IOException e) {
                    System.out.println("File error: " + e.getMessage());
                }

                List<String> command = new ArrayList<>();
                command.add("java");
                command.add("-Xmx8G");
                command.add("-jar");
                command.add(jarPath);
                command.add("--input");
                command.add(inputPath);
                command.add("--output");
                command.add(currentOutputPath);
                command.add("--verbose");

                command.addAll(Arrays.asList(variant.flags));

                try {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (i == 0) {
                    if (outFile.exists()) outFile.delete();

                    File warmupCsv = new File(currentOutputPath.replace(".ttl", "_metrics.csv"));
                    if (warmupCsv.exists()) warmupCsv.delete();

                    System.out.println("Warmup finished");

                } else if (i == 1) {
                    File savedFile = new File(currentOutputPath.replace(".ttl", "_saved.ttl"));
                    if (savedFile.exists()) {
                        savedFile.delete();
                    }
                    outFile.renameTo(savedFile);
                    System.out.println("Saved iteration 1 file as: " + savedFile.getName());
                } else {
                    if (outFile.exists()) {
                        outFile.delete();
                        System.out.println("Deleted iteration " + i);
                    }
                }
            }
        }

        System.out.println("End tests");
    }
}