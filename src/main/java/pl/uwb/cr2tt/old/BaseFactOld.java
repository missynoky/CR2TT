package pl.uwb.cr2tt.old;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public record BaseFactOld(Resource subject, Property predicate, RDFNode object) {}
