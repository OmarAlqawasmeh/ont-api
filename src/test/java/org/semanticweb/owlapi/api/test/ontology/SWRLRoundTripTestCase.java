/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.semanticweb.owlapi.api.test.ontology;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;

@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class SWRLRoundTripTestCase extends TestBase {

    private static final String NS = "urn:test";

    @Test
    public void shouldDoCompleteRoundtrip() throws Exception {
        OWLClass a = Class(IRI(NS + "#", "A"));
        OWLDataProperty p = df.getOWLDataProperty(NS + "#", "P");
        SWRLVariable x = df.getSWRLVariable(NS + "#", "X");
        SWRLVariable y = df.getSWRLVariable(NS + "#", "Y");
        OWLOntology ontology = getOWLOntology();
        Set<SWRLAtom> body = new TreeSet<>();
        body.add(df.getSWRLDataPropertyAtom(p, x, y));
        body.add(df.getSWRLDataRangeAtom(df.getOWLDatatype(XSDVocabulary.STRING), y));
        Set<SWRLAtom> head = new TreeSet<>();
        head.add(df.getSWRLClassAtom(a, x));
        SWRLRule rule = df.getSWRLRule(body, head);
        ontology.getOWLOntologyManager().addAxiom(ontology, rule);
        ontology = roundTrip(ontology, new OWLXMLDocumentFormat());
        OWLOntology onto2 = roundTrip(ontology, new OWLXMLDocumentFormat());
        equal(ontology, onto2);
    }

    @Test
    public void shouldDoCompleteRoundtripManchesterOWLSyntax() throws Exception {
        OWLClass a = Class(IRI(NS + "#", "A"));
        OWLDataProperty p = df.getOWLDataProperty(NS + "#", "P");
        SWRLVariable x = df.getSWRLVariable(NS + "#", "X");
        SWRLVariable y = df.getSWRLVariable(NS + "#", "Y");
        OWLOntology ontology = getOWLOntology();
        Set<SWRLAtom> body = new TreeSet<>();
        body.add(df.getSWRLDataPropertyAtom(p, x, y));
        body.add(df.getSWRLDataRangeAtom(df.getOWLDatatype(XSDVocabulary.STRING), y));
        Set<SWRLAtom> head = new TreeSet<>();
        head.add(df.getSWRLClassAtom(a, x));
        SWRLRule rule = df.getSWRLRule(body, head);
        ontology.getOWLOntologyManager().addAxiom(ontology, rule);
        ontology = roundTrip(ontology, new ManchesterSyntaxDocumentFormat());
        OWLOntology onto2 = roundTrip(ontology, new ManchesterSyntaxDocumentFormat());
        equal(ontology, onto2);
    }

    @Test
    public void shouldDoCompleteRoundtripWithAnnotationsOWLXML() throws Exception {
        OWLOntology ontology = prepareOntology();
        OWLXMLDocumentFormat f = new OWLXMLDocumentFormat();
        OWLOntology ontology2 = loadOntologyFromString(saveOntology(ontology, f));
        equal(ontology, ontology2);
        ontology2.axioms(AxiomType.SWRL_RULE).forEach(r -> Assert.assertFalse(noLabel(r)));
        ontology2.axioms(AxiomType.DATATYPE_DEFINITION).forEach(r -> Assert.assertFalse(noLabel(r)));
    }

    @Test
    public void shouldDoCompleteRoundtripWithAnnotationsTurtle() throws Exception {
        OWLOntology ontology = prepareOntology();
        OWLDocumentFormat f = new TurtleDocumentFormat();
        OWLOntology ontology2 = loadOntologyFromString(saveOntology(ontology, f));
        equal(ontology, ontology2);
        ontology2.axioms(AxiomType.SWRL_RULE).forEach(r -> Assert.assertFalse(noLabel(r)));
        ontology2.axioms(AxiomType.DATATYPE_DEFINITION).forEach(r -> Assert.assertFalse(noLabel(r)));
    }

    @Test
    public void shouldDoCompleteRoundtripWithAnnotationsFunctional() throws Exception {
        OWLOntology ontology = prepareOntology();
        OWLDocumentFormat f = new FunctionalSyntaxDocumentFormat();
        OWLOntology ontology2 = loadOntologyFromString(saveOntology(ontology, f));
        equal(ontology, ontology2);
        ontology2.axioms(AxiomType.SWRL_RULE).forEach(r -> Assert.assertFalse(noLabel(r)));
        ontology2.axioms(AxiomType.DATATYPE_DEFINITION).forEach(r -> Assert.assertFalse(noLabel(r)));
    }

    @Test
    public void shouldDoCompleteRoundtripWithAnnotationsRDFXML() throws Exception {
        OWLOntology ontology = prepareOntology();
        OWLDocumentFormat f = new RDFXMLDocumentFormat();
        OWLOntology ontology2 = loadOntologyFromString(saveOntology(ontology, f));
        equal(ontology, ontology2);
        ontology2.axioms(AxiomType.SWRL_RULE).forEach(r -> Assert.assertFalse(noLabel(r)));
        ontology2.axioms(AxiomType.DATATYPE_DEFINITION).forEach(r -> Assert.assertFalse(noLabel(r)));
    }

    @Test
    public void shouldDoCompleteRoundtripWithAnnotationsDatatypeRDFXML() throws Exception {
        OWLOntology ontology = prepareOntology1();
        OWLDocumentFormat f = new RDFXMLDocumentFormat();
        OWLOntology ontology2 = loadOntologyFromString(saveOntology(ontology, f));
        equal(ontology, ontology2);
        ontology2.axioms(AxiomType.DATATYPE_DEFINITION).forEach(r -> Assert.assertFalse(noLabel(r)));
    }

    private boolean noLabel(OWLAxiom r) {
        return r.annotations(df.getRDFSLabel()).count() == 0;
    }

    @Ignore("man syntax does not like annotations")
    @Test
    public void shouldDoCompleteRoundtripWithAnnotationsMan() throws Exception {
        OWLOntology ontology = prepareOntology();
        OWLDocumentFormat f = new ManchesterSyntaxDocumentFormat();
        StringDocumentTarget save = saveOntology(ontology, f);
        OWLOntology ontology2 = loadOntologyFromString(save);
        equal(ontology, ontology2);
        ontology2.axioms(AxiomType.SWRL_RULE).forEach(r -> Assert.assertFalse(noLabel(r)));
        ontology2.axioms(AxiomType.DATATYPE_DEFINITION).forEach(r -> Assert.assertFalse(noLabel(r)));
    }

    /**
     * @return
     * @throws OWLOntologyCreationException
     */
    private OWLOntology prepareOntology() {
        OWLClass a = Class(IRI(NS + "#", "A"));
        OWLDataProperty p = df.getOWLDataProperty(NS + "#", "P");
        SWRLVariable x = df.getSWRLVariable(NS + "#", "X");
        SWRLVariable y = df.getSWRLVariable(NS + "#", "Y");
        OWLOntology ontology = getOWLOntology();
        Set<SWRLAtom> body = new TreeSet<>();
        body.add(df.getSWRLDataPropertyAtom(p, x, y));
        body.add(df.getSWRLDataRangeAtom(df.getOWLDatatype(XSDVocabulary.STRING), y));
        Set<SWRLAtom> head = new TreeSet<>();
        head.add(df.getSWRLClassAtom(a, x));
        SWRLRule rule = df.getSWRLRule(body, head, singleton(df.getRDFSLabel("test")));
        ontology.getOWLOntologyManager().addAxiom(ontology, rule);
        OWLDatatypeDefinitionAxiom def = df.getOWLDatatypeDefinitionAxiom(df.getOWLDatatype("urn:my#", "datatype"), df
                .getOWLDatatypeMaxExclusiveRestriction(200D), singleton(df.getRDFSLabel("datatype definition")));
        ontology.getOWLOntologyManager().addAxiom(ontology, def);
        return ontology;
    }

    private OWLOntology prepareOntology1() {
        OWLOntology ontology = getOWLOntology();
        OWLDatatypeDefinitionAxiom def = df.getOWLDatatypeDefinitionAxiom(df.getOWLDatatype("urn:my#", "datatype"), df
                .getOWLDatatypeMaxExclusiveRestriction(200D), singleton(df.getRDFSLabel("datatype definition")));
        ontology.getOWLOntologyManager().addAxiom(ontology, def);
        return ontology;
    }

    @Test
    public void shouldParse() throws OWLOntologyCreationException {
        String s = "<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns=\"urn:test#\"\n"
                + "     xml:base=\"urn:test#test\"\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
                + "     xmlns:swrl=\"http://www.w3.org/2003/11/swrl#\"\n"
                + "     xmlns:swrlb=\"http://www.w3.org/2003/11/swrlb#\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "    <owl:Ontology rdf:about=\"urn:test#test\"/>\n"
                + "    <rdfs:Datatype rdf:about=\"urn:my#datatype\">\n"
                + "        <owl:equivalentClass>\n"
                + "            <rdfs:Datatype rdf:about=\"http://www.w3.org/2001/XMLSchema#double\"/>\n"
                + "        </owl:equivalentClass></rdfs:Datatype><owl:Axiom>\n"
                + "        <rdfs:label >datatype definition</rdfs:label>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2002/07/owl#equivalentClass\"/>\n"
                + "        <owl:annotatedSource rdf:resource=\"urn:my#datatype\"/>\n"
                + "        <owl:annotatedTarget>\n"
                + "            <rdfs:Datatype rdf:about=\"http://www.w3.org/2001/XMLSchema#double\"/>\n"
                + "        </owl:annotatedTarget>\n"
                + "    </owl:Axiom></rdf:RDF>";
        OWLOntology o = loadOntologyFromString(s);
        OWLDatatypeDefinitionAxiom def = df.getOWLDatatypeDefinitionAxiom(df.getOWLDatatype("urn:my#", "datatype"), df
                .getDoubleOWLDatatype(), singleton(df.getRDFSLabel("datatype definition")));
        Assert.assertTrue(o.axioms().anyMatch(def::equals));
    }


    @Test
    public void shouldParse2() throws OWLOntologyCreationException {
        // ONT-API comment: a typo in the class declaration 'urn:urn:test#myClass'.
        // I replaced it with 'urn:test#myClass'.
        // I don't thing there is something to fix in the API: the Jena loader reads graph as is,
        // so it would be strange do not expect the same IRI as in RDF, whereas OWL-API  is trying to interpret everything,
        // omitting obvious errors. It is not the way.
        // We should not be tolerant to broken or inconsistent ontologies.
        String s = "<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xml:base=\"http://www.w3.org/2002/07/owl\"\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "    <Class rdf:about=\"urn:test#myClass\">\n"
                + "        <rdfs:subClassOf rdf:resource=\"urn:test#test\"/>\n"
                + "    </Class>\n"
                + "    <Axiom>\n"
                + "        <rdfs:label>datatype definition</rdfs:label>\n"
                + "        <annotatedProperty rdf:resource=\"http://www.w3.org/2000/01/rdf-schema#subClassOf\"/>\n"
                + "        <annotatedSource rdf:resource=\"urn:test#myClass\"/>\n"
                + "        <annotatedTarget rdf:resource=\"urn:test#test\"/>\n"
                + "    </Axiom>\n"
                + "    <Class rdf:about=\"urn:test\"/>\n"
                + "</rdf:RDF>\n";
        OWLOntology o = loadOntologyFromString(s);
        OWLSubClassOfAxiom def = df.getOWLSubClassOfAxiom(df.getOWLClass("urn:test#", "myClass"),
                df.getOWLClass("urn:test#", "test"), singleton(df.getRDFSLabel("datatype definition")));
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(o);
        Assert.assertTrue(o.axioms().anyMatch(def::equals));
    }
}
