package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;

import java.util.List;

public class ClusterValidator {

    public static class FatalValidationException extends RuntimeException {
        public FatalValidationException(String message) {
            super(message);
        }
    }

    public boolean validateCluster(Cluster c, List<Cluster> allClusters, Model inGraph, ConversionMode mode,
                                   BaseTriplePolicy policy, boolean allowAssertingConversion) {
        String reifierName = c.getReifier().isAnon() ? "blank node" : c.getReifier().getLocalName();
        Logger.info("starting validation for cluster: " + c.getReifier().getLocalName());

        // N(s,p,o) ← |{C ∈ Clusters | C has base triple (s, p, o)}|
        long n = allClusters.stream()
                .filter(cPrime -> cPrime.getSubject().equals(c.getSubject()) &&
                        cPrime.getPredicate().equals(c.getPredicate()) &&
                        cPrime.getObject().equals(c.getObject())
        ).count();

        Logger.info(String.format("Calculated N(s,p,o) = %d for triple: (%s, %s, %s)",
                n,
                c.getSubject().getLocalName(),
                c.getPredicate().getLocalName(),
                c.getObject().toString()));

        if (n > 1 && (mode == ConversionMode.REIFIED_TRIPLE || mode == ConversionMode.ANNOTATED_TRIPLE)) {
            throw new FatalValidationException("Multiple reifications for same triple require explicit mode");
        }
        Logger.info("Cluster " + reifierName + " multiplicity check passed.");

        return true;
    }
}
