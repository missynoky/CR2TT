package pl.uwb.cr2tt.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
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
        Logger.info("starting validation for cluster: " + reifierName);

        // N(s,p,o) ← |{C ∈ Clusters | C has base triple (s, p, o)}|
        long n = allClusters.stream()
                .filter(cPrime -> cPrime.getSubject().equals(c.getSubject()) &&
                        cPrime.getPredicate().equals(c.getPredicate()) &&
                        cPrime.getObject().equals(c.getObject())
        ).count();

        Logger.info(String.format("calculated N(s,p,o) = %d for triple: (%s, %s, %s)",
                n,
                c.getSubject().getLocalName(),
                c.getPredicate().getLocalName(),
                c.getObject().toString()));

        if (n > 1 && (mode == ConversionMode.REIFIED_TRIPLE || mode == ConversionMode.ANNOTATED_TRIPLE)) {
            throw new FatalValidationException("multiple reifications for same triple require explicit mode");
        }
        Logger.info("cluster " + reifierName + " multiplicity check passed.");


        boolean baseTripleInGraph = inGraph.contains(c.getSubject(), c.getPredicate(), c.getObject());
        Logger.info("base triple (s,p,o) presence in input graph (G_in): " + baseTripleInGraph);

        if (policy == BaseTriplePolicy.REQUIRE && !baseTripleInGraph) {
            throw new FatalValidationException("Missing base triple");
        }

        if (policy == BaseTriplePolicy.FORBID_EXTRA_ASSERTED && baseTripleInGraph) {
            throw new FatalValidationException("Triple already asserted");
        }
        Logger.info("cluster " + reifierName + " base triple policy check passed.");


        boolean isAssert = (mode == ConversionMode.ANNOTATED_TRIPLE ||
                mode == ConversionMode.ANNOTATED_TRIPLE_EXPLICIT ||
                mode == ConversionMode.ANNOTATED_TRIPLE_EXPANDED);

        if (isAssert && !baseTripleInGraph && !allowAssertingConversion) {
            throw new FatalValidationException("Assertion not allowed");
        }
        Logger.info("cluster " + reifierName + " assertion permission check passed.");


        boolean isBNode = c.getReifier().isAnon();
        boolean isLocal = isLocalReifier(c.getReifier(), inGraph);
        boolean hasMetadata = !c.getMetadata().isEmpty();

        boolean okBNode = isBNode && isLocal && hasMetadata;

        if ((mode == ConversionMode.REIFIED_TRIPLE || mode == ConversionMode.ANNOTATED_TRIPLE) && !okBNode) {
            throw new FatalValidationException("Requires blank node, locality and metadata");
        }

        if (mode == ConversionMode.ANNOTATED_TRIPLE_EXPLICIT && !hasMetadata) {
            throw new FatalValidationException("Requires metadata declaration");
        }

        Logger.info("cluster " + reifierName + " validation completed successfully.");

        return true;
    }

    private boolean isLocalReifier(Resource reifier, Model inGraph) {
        return !inGraph.contains(null, null, reifier);
    }
}
