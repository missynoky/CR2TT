package pl.uwb.cr2tt.core;

import pl.uwb.cr2tt.model.Cluster;
import pl.uwb.cr2tt.model.ConversionMode;
import pl.uwb.cr2tt.model.BaseTriplePolicy;


public class ClusterValidator {
    public String validateCluster(Cluster c, ConversionMode m, BaseTriplePolicy p, boolean allowAssert) {

        int n_spo = c.getNSpo();
        boolean in_G_in = c.isInGIn();

        if (n_spo > 1 && (m == ConversionMode.REIFIED_TRIPLE || m == ConversionMode.ANNOTATED_TRIPLE)) {
            return "Multiple reifications for same triple require explicit mode";
        }

        if (p == BaseTriplePolicy.REQUIRE && !in_G_in) {
            return "Missing base triple";
        }

        if (p == BaseTriplePolicy.FORBID_EXTRA_ASSERTED && in_G_in) {
            return "Triple already asserted";
        }

        boolean isAssert = (m == ConversionMode.ANNOTATED_TRIPLE ||
                m == ConversionMode.ANNOTATED_TRIPLE_EXPLICIT ||
                m == ConversionMode.ANNOTATED_TRIPLE_EXPANDED);

        if (isAssert && !in_G_in && !allowAssert) {
            return "Assertion not allowed";
        }

        boolean isBNode = c.getClusterNode().isAnon();
        boolean isLocal = c.isLocal();
        boolean hasMetadata = !c.getMetadata().isEmpty();

        boolean okBNode = isBNode && isLocal && hasMetadata;

        if ((m == ConversionMode.REIFIED_TRIPLE || m == ConversionMode.ANNOTATED_TRIPLE) && !okBNode) {
            return "Requires blank node, locality and metadata";
        }

        if (m == ConversionMode.ANNOTATED_TRIPLE_EXPLICIT && !hasMetadata) {
            return "Requires metadata declaration";
        }

        return null;
    }
}
