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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.FactoryAccessor;
import ru.avicomp.ontapi.internal.objects.ONTAxiomImpl;
import ru.avicomp.ontapi.internal.objects.ONTClassImpl;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for following axioms:
 * <ul>
 * <li>EquivalentClasses ({@link EquivalentClassesTranslator})</li>
 * <li>EquivalentObjectProperties ({@link EquivalentObjectPropertiesTranslator})</li>
 * <li>EquivalentDataProperties ({@link EquivalentDataPropertiesTranslator})</li>
 * <li>SameIndividual ({@link SameIndividualTranslator})</li>
 * </ul>
 * Also for {@link AbstractTwoWayNaryTranslator} with following subclasses:
 * <ul>
 * <li>DisjointClasses ({@link DisjointClassesTranslator})</li>
 * <li>DisjointObjectProperties ({@link DisjointObjectPropertiesTranslator})</li>
 * <li>DisjointDataProperties ({@link DisjointDataPropertiesTranslator})</li>
 * <li>DifferentIndividuals ({@link DifferentIndividualsTranslator})</li>
 * </ul>
 * <p>
 * Created by szuev on 13.10.2016.
 *
 * @param <Axiom> generic type of {@link OWLAxiom}
 * @param <OWL>   generic type of {@link OWLObject}
 * @param <ONT>   generic type of {@link OntObject}
 */
public abstract class AbstractNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>,
        OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AxiomTranslator<Axiom> {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractNaryTranslator.class);

    private static final Comparator<OWLObject> URI_FIRST_COMPARATOR = (a, b) ->
            a.isAnonymous() == b.isAnonymous() ? 0 : a.isAnonymous() ? -1 : 1;

    void write(OWLNaryAxiom<OWL> thisAxiom, Collection<OWLAnnotation> annotations, OntGraphModel model) {
        List<OWL> operands = thisAxiom.operands().sorted(URI_FIRST_COMPARATOR).collect(Collectors.toList());
        if (operands.isEmpty() && annotations.isEmpty()) { // nothing to write, skip
            return;
        }
        if (operands.size() != 2) {
            throw new OntApiException(getClass().getSimpleName() + ": expected two operands. Axiom: " + thisAxiom);
        }
        WriteHelper.writeTriple(model, operands.get(0), getPredicate(), operands.get(1), annotations);
    }

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        Collection<? extends OWLNaryAxiom<OWL>> axioms = axiom.asPairwiseAxioms();
        if (axioms.isEmpty()) {
            LOGGER.warn("Nothing to write, wrong axiom is given: {}", axiom);
            return;
        }
        axioms.forEach(a -> write(a, axiom.annotationsAsList(), model));
    }

    abstract Property getPredicate();

    abstract Class<ONT> getView();

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return OntModels.listLocalStatements(model, null, getPredicate(), null).filterKeep(this::filter);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return getPredicate().equals(statement.getPredicate()) && filter(statement);
    }

    protected boolean filter(Statement statement) {
        return statement.getSubject().canAs(getView()) && statement.getObject().canAs(getView());
    }

    /**
     * A base for N-Ary axiom impls.
     *
     * @param <A> - subtype of {@link OWLNaryAxiom}
     * @param <M> - subtype of {@link OWLObject}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class NaryAxiomImpl<A extends OWLNaryAxiom<M>, M extends OWLObject>
            extends ONTAxiomImpl<A>
            implements WithManyObjects<M>, WithMerge<ONTObject<A>>, OWLNaryAxiom<M> {

        protected NaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Returns the number of components.
         *
         * @return long
         */
        protected abstract long count();

        /**
         * Creates an instance of {@link NaryAxiomImpl}
         * with additional triples getting from the specified {@code other} object.
         * The returned instance must be equivalent to this instance.
         *
         * @param other {@link ONTObject} with {@link A}, not {@code null}
         * @return {@link NaryAxiomImpl} - a fresh instance that equals to this
         */
        protected abstract NaryAxiomImpl makeCopyWith(ONTObject<A> other);

        /**
         * Creates a factory instance of {@link A}.
         *
         * @param members     a {@code Collection} of {@link M}-members, not {@code null}
         * @param annotations a {@code Collection} of {@link OWLAnnotation}s, can be {@code null}
         * @return {@link A}
         */
        protected abstract A createAxiom(Collection<M> members, Collection<OWLAnnotation> annotations);

        /**
         * Creates a factory instance of {@link A}.
         *
         * @param a           {@link M}, the first component, not {@code null}
         * @param b           {@link M}, the second component, not {@code null}
         * @param annotations a {@code Collection} of {@link OWLAnnotation}s, can be {@code null}
         * @return {@link A}
         */
        private A createAxiom(M a, M b, Collection<OWLAnnotation> annotations) {
            return createAxiom(Arrays.asList(eraseModel(a), eraseModel(b)), annotations);
        }

        @FactoryAccessor
        @Override
        protected final A createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return createAxiom(members().map(x -> eraseModel(x.getOWLObject()))
                    .collect(Collectors.toList()), annotations);
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @SuppressWarnings("unchecked")
        @Override
        public final NaryAxiomImpl merge(ONTObject<A> other) {
            if (this == other) {
                return this;
            }
            if (other instanceof NaryAxiomImpl && sameTriple((NaryAxiomImpl) other)) {
                return this;
            }
            NaryAxiomImpl res = makeCopyWith(other);
            res.hashCode = hashCode;
            return res;
        }

        @FactoryAccessor
        public final Collection<A> asPairwiseAxioms() {
            if (count() == 2) {
                return createSet(eraseModel());
            }
            return walkPairwise((a, b) -> createAxiom(a, b, null));
        }

        @FactoryAccessor
        @Override
        public final Collection<A> splitToAnnotatedPairs() {
            if (count() == 2) {
                return createSet(eraseModel());
            }
            List<OWLAnnotation> annotations = factoryAnnotations().collect(Collectors.toList());
            return walkPairwise((a, b) -> createAxiom(a, b, annotations));
        }
    }

    /**
     * An abstract {@link OWLNaryClassAxiom} implementation.
     *
     * @param <A> subtype of {@link OWLNaryClassAxiom}
     */
    @SuppressWarnings({"NullableProblems", "WeakerAccess"})
    protected abstract static class ClassNaryAxiomImpl<A extends OWLNaryClassAxiom>
            extends NaryAxiomImpl<A, OWLClassExpression> implements OWLNaryClassAxiom {

        protected ClassNaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLClassExpression>> listONTComponents(OntStatement statement,
                                                                                           InternalObjectFactory factory) {
            return Iter.of(factory.getClass(statement.getSubject(OntCE.class)),
                    factory.getClass(statement.getObject(OntCE.class)));
        }

        @Override
        public ONTObject<? extends OWLClassExpression> findByURI(String uri, InternalObjectFactory factory) {
            return ONTClassImpl.find(uri, factory, model);
        }

        @Override
        public Stream<OWLClassExpression> classExpressions() {
            return sorted().map(ONTObject::getOWLObject);
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionsMinus(OWLClassExpression... excludes) {
            return getSetMinus(excludes);
        }

        @Override
        public boolean contains(OWLClassExpression ce) {
            return members().map(ONTObject::getOWLObject).anyMatch(ce::equals);
        }

        /**
         * Creates a {@link OWLSubClassOfAxiom} axiom from factory.
         *
         * @param a - the first operand, not {@code null}
         * @param b - the second operand, not {@code null}
         * @return {@link OWLSubClassOfAxiom}
         */
        @FactoryAccessor
        protected abstract OWLSubClassOfAxiom createSubClassOf(OWLClassExpression a, OWLClassExpression b);

        @FactoryAccessor
        @Override
        public final Collection<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() {
            return walkAllPairwise((a, b) -> createSubClassOf(eraseModel(a), eraseModel(b)));
        }
    }
}
