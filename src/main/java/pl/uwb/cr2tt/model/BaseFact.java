package pl.uwb.cr2tt.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public record BaseFact(Resource subject, Property predicate, RDFNode object) {}
