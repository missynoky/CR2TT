package pl.uwb.cr2tt.old;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import pl.uwb.cr2tt.model.BaseTriplePolicy;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.utils.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClusterValidatorOld {
    private Map<BaseFactOld, Long> multiplicityMap;

    public boolean validateCluster(ClusterOld c, Model inGraph, ConversionMode mode,
                                   BaseTriplePolicy policy, boolean allowAssertingConversion) {
        if (multiplicityMap == null) {
            throw new IllegalStateException("Validator was not initialized. Call initialize() first.");
        }

        BaseFactOld fact = new BaseFactOld(c.getSubject(), c.getPredicate(), c.getObject());
        long n = multiplicityMap.getOrDefault(fact, 0L);

        if (n > 1 && (mode == ConversionMode.REIFIED_TRIPLE || mode == ConversionMode.ANNOTATED_TRIPLE)) {
            throw new FatalValidationException("Multiple reifications for same triple require explicit mode.");
        }


        boolean baseTripleInGraph = inGraph.contains(c.getSubject(), c.getPredicate(), c.getObject());

        if (policy == BaseTriplePolicy.REQUIRE && !baseTripleInGraph) {
            throw new FatalValidationException("Missing base triple.");
        }

        if (policy == BaseTriplePolicy.FORBID_EXTRA_ASSERTED && baseTripleInGraph) {
            throw new FatalValidationException("Triple already asserted.");
        }


        boolean isAssert = (mode == ConversionMode.ANNOTATED_TRIPLE ||
                mode == ConversionMode.ANNOTATED_TRIPLE_EXPLICIT ||
                mode == ConversionMode.ANNOTATED_TRIPLE_EXPANDED);

        if (isAssert && !baseTripleInGraph && !allowAssertingConversion) {
            throw new FatalValidationException("Assertion not allowed.");
        }


        boolean isBNode = c.getReifier().isAnon();
        boolean isLocal = isLocalReifier(c.getReifier(), inGraph);
        boolean hasMetadata = !c.getMetadata().isEmpty();

        boolean okBNode = isBNode && isLocal && hasMetadata;

        if ((mode == ConversionMode.REIFIED_TRIPLE || mode == ConversionMode.ANNOTATED_TRIPLE) && !okBNode) {
            throw new FatalValidationException("Requires blank node, locality and metadata.");
        }

        if (mode == ConversionMode.ANNOTATED_TRIPLE_EXPLICIT && !hasMetadata) {
            throw new FatalValidationException("Requires metadata declaration.");
        }

        return true;
    }

    public static class FatalValidationException extends RuntimeException {
        public FatalValidationException(String message) {
            super(message);
        }
    }

    public void initialize(List<ClusterOld> allClusterOlds) {
        Logger.info("initializing validator.");
        this.multiplicityMap = allClusterOlds.stream()
                .collect(Collectors.groupingBy(
                        c -> new BaseFactOld(c.getSubject(), c.getPredicate(), c.getObject()),
                        Collectors.counting()
                ));
        Logger.info("validator initialized successfully.");
    }

    private boolean isLocalReifier(Resource reifier, Model inGraph) {
        return !inGraph.contains(null, null, reifier);
    }
}
