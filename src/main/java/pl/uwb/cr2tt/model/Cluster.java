package pl.uwb.cr2tt.model;

import lombok.Getter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Collections;
import java.util.Set;

@Getter
public class Cluster {
    private final Resource reifier;
    private final Resource subject;
    private final Property predicate;
    private final RDFNode object;
    private final Set<Statement> metadata;

    public Cluster(Resource reifier, Resource subject, Property predicate, RDFNode object, Set<Statement> metadata) {
        this.reifier = reifier;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.metadata = Collections.unmodifiableSet(metadata);
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
