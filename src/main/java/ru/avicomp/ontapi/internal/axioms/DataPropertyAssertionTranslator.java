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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.FactoryAccessor;
import ru.avicomp.ontapi.internal.objects.ONTDataPropertyImpl;
import ru.avicomp.ontapi.internal.objects.ONTEntityImpl;
import ru.avicomp.ontapi.internal.objects.ONTLiteralImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLDataPropertyAssertionAxiom} implementations.
 * The pattern is {@code a R v}.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class DataPropertyAssertionTranslator
        extends AbstractPropertyAssertionTranslator<OWLDataPropertyExpression, OWLDataPropertyAssertionAxiom> {

    @Override
    public void write(OWLDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(),
                axiom.annotationsAsList());
    }

    /**
     * Lists positive data property assertions: the rule {@code a R v}.
     * See <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model  {@link OntGraphModel} the model
     * @param config {@link InternalConfig}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return listStatements(model).filterKeep(s -> testStatement(s, config));
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.getObject().isLiteral()
                && statement.isData()
                && statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public ONTObject<OWLDataPropertyAssertionAxiom> toAxiom(OntStatement statement,
                                                            Supplier<OntGraphModel> model,
                                                            InternalObjectFactory factory,
                                                            InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLDataPropertyAssertionAxiom> toAxiom(OntStatement statement,
                                                            InternalObjectFactory factory,
                                                            InternalConfig config) {
        ONTObject<? extends OWLIndividual> i = factory.getIndividual(statement.getSubject(OntIndividual.class));
        ONTObject<OWLDataProperty> p = factory.getProperty(statement.getPredicate().as(OntNDP.class));
        ONTObject<OWLLiteral> literal = factory.getLiteral(statement.getLiteral());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLDataPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLDataPropertyAssertionAxiom(p.getOWLObject(), i.getOWLObject(), literal.getOWLObject(),
                        ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(i).append(p).append(literal);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLDataPropertyAssertionAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class AxiomImpl
            extends AssertionImpl<OWLDataPropertyAssertionAxiom,
            OWLIndividual, OWLDataPropertyExpression, OWLLiteral>
            implements OWLDataPropertyAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLDataPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param model     {@link OntGraphModel}-provider, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return WithAssertion.create(statement, model,
                    SimpleImpl.FACTORY, WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLIndividual> findONTSubject(InternalObjectFactory factory) {
            return findByURIOrBlankId(subject, factory);
        }

        @Override
        public ONTObject<? extends OWLLiteral> findONTObject(InternalObjectFactory factory) {
            return ONTLiteralImpl.find((LiteralLabel) object, factory, model);
        }

        @Override
        public ONTObject<? extends OWLDataProperty> findONTPredicate(InternalObjectFactory factory) {
            return findONTProperty(factory);
        }

        public ONTObject<OWLDataProperty> findONTProperty(InternalObjectFactory factory) {
            return ONTDataPropertyImpl.find(predicate, factory, model);
        }

        @FactoryAccessor
        @Override
        protected OWLDataPropertyAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDataPropertyAssertionAxiom(getFPredicate(), getFSubject(),
                    getFObject(), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(getFSubject()),
                    df.getOWLDataHasValue(getFPredicate(), getFObject()));
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public Set<OWLDataProperty> getDataPropertySet() {
            return createSet(getProperty().asOWLDataProperty());
        }

        @Override
        public boolean containsDataProperty(OWLDataProperty property) {
            return predicate.equals(ONTEntityImpl.getURI(property));
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            return hasURISubject() ? createSet(getSubject().asOWLNamedIndividual()) : createSet();
        }

        /**
         * An {@link OWLDataPropertyAssertionAxiom} that has no sub-annotations.
         */
        public static class SimpleImpl extends AxiomImpl
                implements Simple<OWLIndividual, OWLDataPropertyExpression, OWLLiteral> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                return hasURISubject() ? createSet() : createSet(getSubject().asOWLAnonymousIndividual());
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return createSet(getONTObject().getOWLObject().getDatatype());
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                Set<OWLEntity> res = createSortedSet();
                InternalObjectFactory factory = getObjectFactory();
                res.add(findONTProperty(factory).getOWLObject());
                if (hasURISubject()) {
                    res.add(findONTSubject(factory).getOWLObject().asOWLNamedIndividual());
                }
                res.add(findONTObject(factory).getOWLObject().getDatatype());
                return res;
            }

            @Override
            public boolean containsNamedIndividual(OWLNamedIndividual individual) {
                return getSubject().equals(individual);
            }
        }

        /**
         * An {@link OWLDataPropertyAssertionAxiom} that has sub-annotations.
         * This class has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class WithAnnotationsImpl extends AxiomImpl
                implements WithAnnotations<WithAnnotationsImpl, OWLIndividual, OWLDataPropertyExpression, OWLLiteral> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, WithAnnotationsImpl> FACTORY =
                    WithAnnotationsImpl::new;
            protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

            public WithAnnotationsImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<WithAnnotationsImpl, Object[]> getContentCache() {
                return content;
            }
        }
    }
}
