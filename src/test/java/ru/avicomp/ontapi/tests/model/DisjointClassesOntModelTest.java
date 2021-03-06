/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.model;

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Test for {@code owl:AllDisjointClasses} and {@code owl:disjointWith} using jena and owl-api
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class DisjointClassesOntModelTest extends OntModelTestBase {

    /**
     * The order while removing (see {@link org.apache.jena.rdf.model.RDFList#removeList}) is unpredictable.
     * This method checks that that operation does not broke the whole process and the ontology data.
     */
    @Test
    public void testDisjointAddRemoveInCycle() {
        IntStream.rangeClosed(1, 10).forEach(i -> {
            LOGGER.debug("ITER #{}", i);
            testDisjointAddRemove();
        });
    }

    private void testDisjointAddRemove() {
        OWLDataFactory factory = OntManagers.getDataFactory();
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("ontapi/test1.ttl"));
        LOGGER.debug("Load ontology from file {}", fileIRI);
        OWLOntology original;
        try {
            original = OntManagers.createONT().loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
        debug(original);

        LOGGER.debug("Assemble new ontology with the same content.");
        OntIRI iri = OntIRI.create("http://test.test/complex");
        OntIRI ver = OntIRI.create("http://test.test/complex/version-iri/1.0");
        OntologyModel result = OntManagers.createONT().createOntology(iri.toOwlOntologyID());
        OntGraphModel jena = result.asGraphModel()
                .setNsPrefix("", iri.getIRIString() + "#")
                .getID().setVersionIRI(ver.getIRIString())
                .getModel();

        OWLClass owlSimple1 = factory.getOWLClass(iri.addFragment("Simple1"));
        OWLClass owlSimple2 = factory.getOWLClass(iri.addFragment("Simple2"));
        OWLClass owlComplex1 = factory.getOWLClass(iri.addFragment("Complex1"));
        OWLClass owlComplex2 = factory.getOWLClass(iri.addFragment("Complex2"));
        OntClass ontSimple1 = jena.createOntClass(owlSimple1.getIRI().getIRIString());
        OntClass ontSimple2 = jena.createOntClass(owlSimple2.getIRI().getIRIString());
        OntClass ontComplex1 = jena.createOntClass(owlComplex1.getIRI().getIRIString());
        OntClass ontComplex2 = jena.createOntClass(owlComplex2.getIRI().getIRIString());

        OntOPE property = jena.createObjectProperty(iri.addFragment("hasSimple1").getIRIString())
                .setFunctional(true).addRange(ontSimple1);
        OntCE.ObjectSomeValuesFrom restriction = jena.createObjectSomeValuesFrom(property, ontSimple2);
        ontComplex2.addSuperClass(restriction).addSuperClass(ontComplex1);
        ontComplex2.addComment("comment1", "es");
        Assert.assertEquals("comment1", ontComplex2.getComment("es"));
        ontComplex1.addDisjointClass(ontSimple1);

        // bulk disjoint instead adding one by one (to have the same list of axioms):
        jena.createResource(OWL.AllDisjointClasses).addProperty(
                OWL.members, jena.createList(Stream.of(ontComplex2, ontSimple1, ontSimple2).iterator()));

        debug(result);

        LOGGER.debug("Compare axioms.");
        List<OWLAxiom> actual = result.axioms().sorted().collect(Collectors.toList());
        List<OWLAxiom> expected = original.axioms().sorted().collect(Collectors.toList());
        Assert.assertThat("Axioms", actual, IsEqual.equalTo(expected));

        LOGGER.debug("Remove OWL:disjointWith for {} & {} pair.", ontComplex1, ontSimple1);
        jena.removeAll(ontComplex1, OWL.disjointWith, null);
        ReadWriteUtils.print(result.asGraphModel(), OntFormat.TURTLE);
        actual = result.axioms().sorted().collect(Collectors.toList());
        expected = original.axioms().sorted().collect(Collectors.toList());
        expected.remove(factory.getOWLDisjointClassesAxiom(owlComplex1, owlSimple1));
        expected.stream().map(String::valueOf).forEach(LOGGER::debug);
        Assert.assertThat("Axioms", actual, IsEqual.equalTo(expected));

        LOGGER.debug("Remove owl:AllDisjointClasses using RDFList#removeList");
        Resource anon = jena.listResourcesWithProperty(RDF.type, OWL.AllDisjointClasses).toList().get(0);
        RDFList list = jena.listObjectsOfProperty(anon, OWL.members).mapWith(n -> n.as(RDFList.class)).toList().get(0);
        list.removeList();
        jena.removeAll(anon, null, null);
        debug(result);
        Assert.assertFalse(result.axioms(AxiomType.DISJOINT_CLASSES).findFirst().isPresent());

        LOGGER.debug("Compare axioms.");
        actual = result.axioms().sorted().collect(Collectors.toList());
        expected = original.axioms()
                .filter(a -> !AxiomType.DISJOINT_CLASSES.equals(a.getAxiomType()))
                .sorted().collect(Collectors.toList());
        Assert.assertThat("Axioms", actual, IsEqual.equalTo(expected));

    }
}
