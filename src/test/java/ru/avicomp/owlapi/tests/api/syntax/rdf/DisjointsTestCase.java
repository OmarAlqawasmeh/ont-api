/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ru.avicomp.owlapi.tests.api.syntax.rdf;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.owlapi.OWLFunctionalSyntaxFactory;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Test cases for rendering of disjoint axioms.
 * The OWL 1.1 specification makes it possible to specify that a set of classes are mutually disjoint.
 * Unfortunately, this must be represented in RDF as a set of pairwise disjoint statements.
 * In otherwords, DisjointClasses(A, B, C) must be represented as DisjointWith(A, B), DisjointWith(A, C) DisjointWith(B, C).
 * ~This test case ensure that these axioms are serialised correctly.
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public class DisjointsTestCase extends TestBase {

    @Test
    public void testAnonDisjoints() throws Exception {
        OWLOntology ontA = getOWLOntology();
        OWLClass clsA = OWLFunctionalSyntaxFactory.createClass();
        OWLClass clsB = OWLFunctionalSyntaxFactory.createClass();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.createObjectProperty();
        OWLClassExpression descA = df.getOWLObjectSomeValuesFrom(prop, clsA);
        OWLClassExpression descB = df.getOWLObjectSomeValuesFrom(prop, clsB);
        Set<OWLClassExpression> classExpressions = new HashSet<>();
        classExpressions.add(descA);
        classExpressions.add(descB);
        OWLAxiom ax = df.getOWLDisjointClassesAxiom(classExpressions);
        ontA.add(ax);
        OWLOntology ontB = roundTrip(ontA);
        Assert.assertTrue(ontB.axioms().anyMatch(ax::equals));
    }

    @Test // copy-pasted form 5.1.8 owlapi-contract
    public void shouldAcceptSingleDisjointAxiom() {
        // The famous idiomatic use of DisjointClasses with one operand
        OWLClass t = df.getOWLClass("urn:test:class");
        OWLDisjointClassesAxiom ax = df.getOWLDisjointClassesAxiom(Collections.singletonList(t));
        Assert.assertEquals(df.getOWLDisjointClassesAxiom(Arrays.asList(t, df.getOWLThing())), ax.getAxiomWithoutAnnotations());
        OWLLiteral value = df.getOWLLiteral("DisjointClasses(<urn:test:class>) replaced by DisjointClasses(<urn:test:class> owl:Thing)");
        OWLAnnotation a = ax.annotationsAsList().get(0);
        Assert.assertEquals(value, a.getValue());
        Assert.assertEquals(df.getRDFSComment(), a.getProperty());
    }

    @Test(expected = OWLRuntimeException.class) // copy-pasted form 5.1.8 owlapi-contract
    public void shouldRejectDisjointClassesWithSingletonThing() {
        df.getOWLDisjointClassesAxiom(Collections.singletonList(df.getOWLThing()));
    }

    @Test(expected = OWLRuntimeException.class) // copy-pasted form 5.1.8 owlapi-contract
    public void shouldRejectDisjointClassesWithSingletonNothing() {
        df.getOWLDisjointClassesAxiom(Collections.singletonList(df.getOWLNothing()));
    }
}
