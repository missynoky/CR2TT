package pl.uwb.cr2tt.old;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Collections;
import java.util.Set;

public class ClusterOld {
    private final Resource reifier;
    private final Resource subject;
    private final Property predicate;
    private final RDFNode object;
    private final Set<Statement> metadata;

    public ClusterOld(Resource reifier, Resource subject, Property predicate, RDFNode object, Set<Statement> metadata) {
        this.reifier = reifier;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.metadata = Collections.unmodifiableSet(metadata);
    }

    public Resource getReifier() {
        return reifier;
    }

    public Resource getSubject() {
        return subject;
    }

    public Property getPredicate() {
        return predicate;
    }

    public RDFNode getObject() {
        return object;
    }

    public Set<Statement> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "reifier=" + reifier.getLocalName() +
                ", triple=(" + subject.getLocalName() + " " + predicate.getLocalName() + " " + object.toString() + ")" +
                ", metaCount=" + metadata.size() +
                '}';
    }
}
