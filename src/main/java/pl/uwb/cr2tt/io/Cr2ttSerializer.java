package pl.uwb.cr2tt.io;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.util.FmtUtils;

import pl.uwb.cr2tt.model.ConversionMode;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Cr2ttSerializer {

    public static void serialize(OutputStream out, Model model, ConversionMode mode) {
        switch (mode) {
            case REIFIED_TRIPLE:
                writeReifiedTriple(out, model);
                break;
            case REIFIED_TRIPLE_EXPANDED:
                writeReifiedTripleExplicit(out, model);
                break;
            case ANNOTATED_TRIPLE:
                writeAnnotatedTriple(out, model);
                break;
            case ANNOTATED_TRIPLE_EXPLICIT:
                writeAnnotatedTripleExplicit(out, model);
                break;
            case ANNOTATED_TRIPLE_EXPANDED:
                writeAnnotatedTripleExpanded(out, model);
                break;
            default:
                throw new IllegalArgumentException("Unsupported serialization mode: " + mode);
        }
    }

    private static void writeReifiedTriple(OutputStream out, Model model) {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));

        writer.println("VERSION \"1.2\"");

        for (Map.Entry<String, String> entry : model.getNsPrefixMap().entrySet()) {
            writer.println("PREFIX " + entry.getKey() + ": <" + entry.getValue() + ">");
        }
        writer.println();

        Property rdfReifies = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies");

        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            Resource s = subjects.next();
            List<Statement> stmts = s.listProperties().toList();

            if (stmts.isEmpty()) continue;

            Statement reifiesStmt = null;
            for (Statement stmt : stmts) {
                if (stmt.getPredicate().equals(rdfReifies)) {
                    reifiesStmt = stmt;
                    break;
                }
            }

            if (reifiesStmt != null) {
                Node tripleNode = reifiesStmt.getObject().asNode();

                if (tripleNode.isTripleTerm()) {
                    Triple t = tripleNode.getTriple();
                    String subjStr = FmtUtils.stringForNode(t.getSubject(), model);
                    String predStr = FmtUtils.stringForNode(t.getPredicate(), model);
                    String objStr = FmtUtils.stringForNode(t.getObject(), model);

                    writer.print("<< " + subjStr + " " + predStr + " " + objStr + " >>");
                } else {
                    writer.print(FmtUtils.stringForNode(s.asNode(), model));
                }
            } else {
                writer.print(FmtUtils.stringForNode(s.asNode(), model));
            }

            boolean first = true;
            for (Statement stmt : stmts) {
                if (stmt.equals(reifiesStmt)) continue;

                if (first) {
                    writer.print(" ");
                    first = false;
                } else {
                    writer.print(" ;\n    ");
                }

                String pStr = FmtUtils.stringForNode(stmt.getPredicate().asNode(), model);
                String oStr = FmtUtils.stringForNode(stmt.getObject().asNode(), model);
                writer.print(pStr + " " + oStr);
            }

            writer.println(" .");
            writer.println();
        }

        writer.flush();
    }

    private static void writeReifiedTripleExplicit(OutputStream out, Model model) {
        // TODO: Implement formatting for reified-triple-explicit
        throw new UnsupportedOperationException("REIFIED_TRIPLE_EXPANDED serializer is not yet implemented.");
    }

    private static void writeAnnotatedTriple(OutputStream out, Model model) {
        // TODO: Implement formatting for annotated-triple
        throw new UnsupportedOperationException("ANNOTATED_TRIPLE serializer is not yet implemented.");
    }

    private static void writeAnnotatedTripleExplicit(OutputStream out, Model model) {
        // TODO: Implement formatting for annotated-triple-explicit
        throw new UnsupportedOperationException("ANNOTATED_TRIPLE_EXPLICIT serializer is not yet implemented.");
    }

    private static void writeAnnotatedTripleExpanded(OutputStream out, Model model) {
        // TODO: Implement formatting for annotated-triple-expanded
        throw new UnsupportedOperationException("ANNOTATED_TRIPLE_EXPANDED serializer is not yet implemented.");
    }
}