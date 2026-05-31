package pl.uwb.cr2tt.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Objects;
import java.util.Set;


public class Cluster {
    private final Resource clusterNode;
    private final Resource subjectNode;
    private final Property predicateNode;
    private final RDFNode objectNode;
    private final Set<Statement> metadata;
    private final boolean isLocal;

    private final int nSpo;
    private final boolean inGIn;
    private final boolean isNestedTarget;

    public Cluster(Resource clusterNode, Resource subjectNode, Property predicateNode,
                   RDFNode objectNode, Set<Statement> metadata, int nSpo, boolean inGIn, boolean isLocal, boolean isNestedTarget) {
        this.clusterNode = Objects.requireNonNull(clusterNode, "clusterNode cannot be null");
        this.subjectNode = Objects.requireNonNull(subjectNode, "subjectNode cannot be null");
        this.predicateNode = Objects.requireNonNull(predicateNode, "predicateNode cannot be null");
        this.objectNode = Objects.requireNonNull(objectNode, "objectNode cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");

        this.nSpo = nSpo;
        this.inGIn = inGIn;
        this.isLocal = isLocal;
        this.isNestedTarget = isNestedTarget;
    }

    public Resource getClusterNode() { return clusterNode; }
    public Resource getSubjectNode() { return subjectNode; }
    public Property getPredicateNode() { return predicateNode; }
    public RDFNode getObjectNode() { return objectNode; }
    public Set<Statement> getMetadata() { return metadata; }
    public int getNSpo() { return nSpo; }
    public boolean isInGIn() { return inGIn; }
    public boolean isLocal() { return isLocal; }
    public boolean isNestedTarget() { return isNestedTarget; }
}
