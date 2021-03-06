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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An enhanced view of a {@link Model Jena Model} that is known to contain <b>OWL2</b> ontology data.
 * This is an analogue of {@link org.apache.jena.ontology.OntModel Apache Jena OntModel}
 * to work with Ontology {@code Graph} in accordance with OWL2 DL specification.
 * <p>
 * In addition to the standard {@link Resource Jena Resource}s and {@link Statement Jena Statement}s
 * this model provides access to different ontological components in the form of {@link OntObject Object}s
 * and {@link OntStatement Ontology Statement}s that support OWL Annotations.
 * Some of the {@link OntObject}s can be constructed using another kind of resource -
 * {@link OntList}, which is an extended analogue of the standard {@link RDFList Jena RDFList}.
 * <p>
 * The model also has a component-level support of Semantic Web Rule Language (SWRL).
 * <p>
 * Note: since this model is only for OWL2 semantics,
 * it does not support {@link org.apache.jena.ontology.Profile Jena Profile}s, and model configuration
 * is delegated directly to the {@link ru.avicomp.ontapi.jena.impl.conf.OntPersonality Ontology Personality}.
 * <p>
 * Note: it does not extends {@link InfModel} interface, although
 * encapsulated graph can always be wrapped as {@link InfModel} (see {@link OntGraphModel#getInferenceModel(Reasoner)}).
 * <p>
 * Note: in additional to native Jena {@link org.apache.jena.util.iterator.ExtendedIterator Extended Iterator}s,
 * this model also provides access to RDF in the from of {@link Stream}s, that obey the same rules:
 * both {@code Stream} and {@code ExtendedIterator} must be closed explicitly
 * if they are no longer needed but not yet exhausted.
 * For more details see {@link org.apache.jena.util.iterator.ClosableIterator}.
 * In case of {@code Stream} try-with-resources can be used.
 * <p>
 * Created by @szuev on 11.11.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf'>OWL2 RDF mapping</a>
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>A Quick Guide</a>
 * @see <a href='https://www.w3.org/TR/owl2-syntax/'>OWL 2 Web Ontology Language Structural Specification and Functional-Style Syntax (Second Edition)</a>
 * @see <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>
 */
public interface OntGraphModel extends Model, CreateClasses, CreateRanges, CreateDisjoint, CreateSWRL {

    /**
     * Returns the base {@code Graph},
     * i.e. the primary ontological {@code Graph} that does not contain any sub-graphs hierarchy.
     * Only the base graph can be edited from this interface view.
     * To get the whole union graph use the method {@link #getGraph()}.
     *
     * @return {@link Graph}
     * @see #getGraph()
     */
    Graph getBaseGraph();

    /**
     * Returns the {@link Model standard jena model} that corresponds to the {@link #getBaseGraph() base graph}.
     * Note: there is the {@link org.apache.jena.enhanced.BuiltinPersonalities#model Jena Builtin Personality}
     * within the returned model.
     *
     * @return {@link Model}
     * @see #getBaseGraph()
     */
    Model getBaseModel();

    /**
     * Creates an inference model shadow using this model as data.
     * Note(1): there is the {@link org.apache.jena.enhanced.BuiltinPersonalities#model Jena Builtin Personality}
     * within the returned model.
     * Note(2): any changes in the returned {@link InfModel Inference Model} do not affect on this model.
     *
     * @param reasoner {@link Reasoner}, not {@code null}
     * @return {@link InfModel}
     * @throws org.apache.jena.reasoner.ReasonerException if the data is ill-formed according to the
     *                                                    constraints imposed by this reasoner.
     */
    InfModel getInferenceModel(Reasoner reasoner);

    /**
     * Gets an Ontology ID object.
     * Since OWL2 graph can only contain the one {@code @uri rdf:type owl:Ontology} triple inside,
     * this method creates such statement if it absent;
     * in case there are more than one {@code Resource} with the type equaled to {@code owl:Ontology},
     * it chooses the most bulky one (i.e. those that contains the most number of associated statements)
     * and all the others leave intact.
     *
     * @return {@link OntID} an existing or fresh {@link Resource},
     * that is subject in the {@code _:x rdf:type owl:Ontology} statement
     * @see ru.avicomp.ontapi.jena.utils.Graphs#ontologyNode
     */
    OntID getID();

    /**
     * Creates a new {@code @uri rdf:type owl:Ontology} statement for the specified {@code uri}
     * and wraps it as Ontology ID Resource.
     * Removes all extra ontology objects if they are present and moves their content to the new one,
     * as it is required by OWL2 specification.
     *
     * @param uri String, can be null to make anonymous ontology
     * @return the new {@link OntID} instance
     * @throws OntJenaException if ontology can't be added (e.g. due to collision with imports)
     */
    OntID setID(String uri);

    /**
     * Adds a sub model both to the {@code owl:import} section and to the graph hierarchy.
     *
     * @param m {@link OntGraphModel ont jena model} to add, not {@code null}
     * @return this model to allow cascading calls
     * @throws OntJenaException if specified ontology is anonymous
     *                          or already present in the imports (both as graph and in owl-declaration)
     * @see OntID#addImport(String)
     */
    OntGraphModel addImport(OntGraphModel m) throws OntJenaException;

    /**
     * Removes a sub-model from {@code owl:import} and from the graph hierarchy.
     * Does nothing, if the specified model does not belong to this ontology.
     * Matching is performed by graph, not uri (see {@link #hasImport(OntGraphModel)} description).
     *
     * @param m {@link OntGraphModel ont jena model} to remove, not {@code null}
     * @return <b>this</b> model to allow cascading calls
     * @see OntID#removeImport(String)
     * @see #hasImport(OntGraphModel)
     */
    OntGraphModel removeImport(OntGraphModel m);

    /**
     * Removes the import (both {@code owl:import} declaration and the corresponding graph)
     * by the given uri if it is found.
     *
     * @param uri String, an iri of ontology to find, not {@code null}
     * @return <b>this</b> model to allow cascading calls
     * @see OntID#getImportsIRI()
     * @see #hasImport(String)
     */
    OntGraphModel removeImport(String uri);

    /**
     * Lists all sub-models
     * that belong to the top-level hierarchy and have {@code owl:import} reference inside the base graph.
     * Caution: since recursive hierarchies are not prohibited,
     * the rectilinear usage of this method may cause a StackOverflow Error.
     *
     * @return {@code Stream} of {@link OntGraphModel}s
     * @see OntID#imports()
     */
    Stream<OntGraphModel> imports();

    /**
     * Answers {@code true} if the given model is present in the {@link OWL#imports owl:imports} of this model.
     * This means that at the top-level of the import hierarchy there is a base graph of the given {@code other} model.
     * Please note: the model may contain the same uri as that of the specified model, but a different (base) graph,
     * i.e. if the method {@link #hasImport(String)} returns {@code true},
     * it does not mean this method also returns {@code true}.
     *
     * @param other {@link OntGraphModel} to test, not {@code null}
     * @return {@code true} if the model is in imports
     * @since 1.4.0
     */
    boolean hasImport(OntGraphModel other);

    /**
     * Answers {@code true} if the model has a graph with the given uri both in {@code owl:imports} and graph-hierarchy.
     *
     * @param uri String, not {@code null}
     * @return boolean
     * @see OntID#getImportsIRI()
     * @since 1.4.0
     */
    boolean hasImport(String uri);

    /**
     * Lists all ont-objects of the specified type.
     *
     * @param type {@link Class} the concrete type of {@link OntObject}, not {@code null}
     * @param <O>  any ont-object subtype
     * @return {@code Stream} of {@link OntObject}s of the type {@link O}
     * @see #ontEntities()
     */
    <O extends OntObject> Stream<O> ontObjects(Class<? extends O> type);

    /**
     * Lists all entities declared in the model.
     * Built-ins are not included.
     * The retrieved entities can belong to the underlying graphs also.
     * Note: this method returns non-distinct stream,
     * while the expression {@code ontObjects(OntEntity.class)} is supposed to be distinct stream.
     * The duplicate elements (by {@code equals} and {@code hasCode}, not by real class-type)
     * means that there is so called punning.
     *
     * @return {@code Stream} of {@link OntEntity}
     * @see #ontObjects(Class)
     * @see #ontEntities(Class)
     */
    Stream<OntEntity> ontEntities();

    /**
     * Lists all class-asserted individuals.
     * <p>
     * A class assertion axiom is a statement {@code a rdf:type C},
     * where {@code a} is a retrieving individual (named or anonymous) and {@code C} is any class expression.
     * Notice, that the method {@link OntGraphModel#ontObjects(Class)}
     * called with the parameter {@code OntIndividual.class}
     * (i.e. {@code model.ontObject(OntIndividual.class)}) must return all individuals from a model,
     * even those which have no explicit declarations (e.g. any part of {@code owl:sameAs} is an individual),
     * while this method returns only class-asserted individuals.
     * Also notice: the method {@link #namedIndividuals()} must return only explicitly declared named individuals,
     * while this method does not require the declaration {@link OWL#NamedIndividual owl:NamedIndividual}
     * to be present for an individual: according to the specification it is optional, for more details see
     * <a href='https://www.w3.org/TR/owl2-syntax/#Typing_Constraints_of_OWL_2_DL'>5.8.1 Typing Constraints of OWL 2 DL</a>.
     * Also note: in case of valid distinct {@link #getGraph() RDF graph}
     * the returned {@code Stream} is also distinct,
     * which means an individual that has more than one class assertions, must appear in the stream only once.
     *
     * @return {@code Stream} of {@link OntIndividual}s
     * @see OntGraphModel#namedIndividuals()
     * @since 1.4.1
     */
    Stream<OntIndividual> individuals();

    /**
     * Returns an ont-entity for the specified type and uri.
     * This method can also be used to wrap builtin entities, which, in fact, are not belonging to the graph,
     * but can be considered as belonged to the model.
     * An IRI for such a built-in entity must be in
     * the {@link ru.avicomp.ontapi.jena.impl.conf.OntPersonality.Builtins Builtins Vocabulary},
     * otherwise the method returns {@code null}.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not {@code null}.
     * @param uri, String, not {@code null}.
     * @param <E>  type of ont-entity
     * @return {@link OntEntity} or {@code null}
     * @see #fetchOntEntity(Class, String)
     */
    <E extends OntEntity> E getOntEntity(Class<E> type, String uri);

    /**
     * Lists all ont-statements.
     *
     * @return {@code Stream} of {@link OntStatement}
     * @see Model#listStatements()
     */
    Stream<OntStatement> statements();

    /**
     * Lists all statements for the specified subject, predicate and object (SPO).
     *
     * @param s {@link Resource}, the subject
     * @param p {@link Property}, the predicate
     * @param o {@link RDFNode}, the object
     * @return {@code Stream} of {@link OntStatement}
     * @see Model#listStatements(Resource, Property, RDFNode)
     */
    Stream<OntStatement> statements(Resource s, Property p, RDFNode o);

    /**
     * Lists all statements from the {@link OntGraphModel#getBaseGraph() base graph}
     * for the specified subject, predicate and object.
     * Effectively equivalent to the {@code model.statements(s, p, o).filter(OntStatement::isLocal)} expression.
     *
     * @param s {@link Resource}, the subject
     * @param p {@link Property}, the predicate
     * @param o {@link RDFNode}, the object
     * @return {@code Stream} of {@link OntStatement}
     * @see OntGraphModel#statements(Resource, Property, RDFNode)
     * @see OntStatement#isLocal()
     * @since 1.3.0
     */
    Stream<OntStatement> localStatements(Resource s, Property p, RDFNode o);

    /**
     * Answers an {@link OntStatement Ontology Statement} in this model who's SPO is that of the {@code triple}.
     *
     * @param triple {@link Triple}, not {@code null}
     * @return {@link OntStatement}
     */
    @Override
    OntStatement asStatement(Triple triple);

    /**
     * Answers iff the statement belongs to the base graph.
     *
     * @param statement {@link Statement}
     * @return true if statement is local.
     * @see OntStatement#isLocal()
     * @see OntObject#isLocal()
     * @see OntGraphModel#localStatements(Resource, Property, RDFNode)
     */
    boolean isLocal(Statement statement);

    /**
     * Removes the given {@link OntObject Ontology Object} from the graph-model
     * including its {@link OntObject#content() content} and annotations.
     * This operation does not guarantee clearing all object references:
     * it takes into account only statements where the given object in a subject position.
     * For example, in case of deleting an OWL class
     * that is on the right side in a statement with the predicate {@code rdfs:subClassOf},
     * that statement remains unchanged in the graph, but becomes meaningless:
     * its right side will no longer be a class, but just uri.
     * But if a class is on the left side of the statement with the {@code rdfs:subClassOf} predicate,
     * that statement is be removed from the graph along with its annotations,
     * because it is belongs to the class content.
     *
     * @param obj {@link OntObject}
     * @return <b>this</b> model
     * @see OntObject#content()
     */
    OntGraphModel removeOntObject(OntObject obj);

    /**
     * Removes the statement from the graph-model including its annotations with sub-annotations hierarchy.
     *
     * @param statement {@link OntStatement}
     * @return <b>this</b> model
     * @see #remove(Statement)
     */
    OntGraphModel removeOntStatement(OntStatement statement);

    /**
     * Creates an owl-entity by the {@code type} and {@code iri}.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not {@code null}
     * @param iri  String, not {@code null}
     * @param <E>  type of ont-entity
     * @return {@link OntEntity}
     * @throws OntJenaException.Creation in case something is wrong
     * @see #getOntEntity(Class, String)
     */
    <E extends OntEntity> E createOntEntity(Class<E> type, String iri);

    /**
     * Creates a facet restriction by the given type and literal value.
     * Each call to this method creates a fresh b-node within the graph.
     *
     * @param type    {@link Class}, the type of {@link OntFR}, not {@code null}
     * @param literal {@link Literal}, not {@code null}
     * @param <F>     type of ont-facet-restriction
     * @return {@link OntFR}
     * @see OntDR.Restriction
     * @see OntGraphModel#createRestrictionDataRange(OntDT, Collection)
     */
    <F extends OntFR> F createFacetRestriction(Class<F> type, Literal literal);

    /*
     * ================================================
     * Overridden methods inherited from PrefixMapping:
     * ================================================
     */

    @Override
    OntGraphModel setNsPrefix(String prefix, String uri);

    @Override
    OntGraphModel removeNsPrefix(String prefix);

    @Override
    OntGraphModel clearNsPrefixMap();

    @Override
    OntGraphModel setNsPrefixes(PrefixMapping other);

    @Override
    OntGraphModel setNsPrefixes(Map<String, String> map);

    @Override
    OntGraphModel withDefaultMappings(PrefixMapping map);

    /*
     * =====================================================
     * Overridden methods inherited from Model and ModelCon:
     * =====================================================
     */

    @Override
    OntGraphModel add(Statement s);

    @Override
    OntGraphModel remove(Statement s);

    @Override
    OntGraphModel add(Resource s, Property p, RDFNode o);

    @Override
    OntGraphModel remove(Resource s, Property p, RDFNode o);

    @Override
    OntGraphModel add(Model m);

    @Override
    OntGraphModel remove(Model m);

    @Override
    OntGraphModel add(StmtIterator i);

    @Override
    OntGraphModel remove(StmtIterator i);

    @Override
    OntGraphModel removeAll(Resource s, Property p, RDFNode o);

    @Override
    OntGraphModel removeAll();

    /*
     * ===================================
     * Default methods for simplification:
     * ===================================
     */

    default OntClass createOntClass(String uri) {
        return createOntEntity(OntClass.class, uri);
    }

    default OntDT createDatatype(String uri) {
        return createOntEntity(OntDT.class, uri);
    }

    default OntIndividual.Named createIndividual(String uri) {
        return createOntEntity(OntIndividual.Named.class, uri);
    }

    default OntNAP createAnnotationProperty(String uri) {
        return createOntEntity(OntNAP.class, uri);
    }

    default OntNDP createDataProperty(String uri) {
        return createOntEntity(OntNDP.class, uri);
    }

    default OntNOP createObjectProperty(String uri) {
        return createOntEntity(OntNOP.class, uri);
    }

    default OntClass getOntClass(String uri) {
        return getOntEntity(OntClass.class, uri);
    }

    default OntDT getDatatype(String uri) {
        return getOntEntity(OntDT.class, uri);
    }

    default OntIndividual.Named getIndividual(String uri) {
        return getOntEntity(OntIndividual.Named.class, uri);
    }

    default OntNAP getAnnotationProperty(String uri) {
        return getOntEntity(OntNAP.class, uri);
    }

    default OntNDP getDataProperty(String uri) {
        return getOntEntity(OntNDP.class, uri);
    }

    default OntNOP getObjectProperty(String uri) {
        return getOntEntity(OntNOP.class, uri);
    }

    default OntClass getOntClass(Resource uri) {
        return getOntClass(uri.getURI());
    }

    default OntDT getDatatype(Resource uri) {
        return getDatatype(uri.getURI());
    }

    default OntIndividual.Named getIndividual(Resource uri) {
        return getIndividual(uri.getURI());
    }

    default OntNAP getAnnotationProperty(Resource uri) {
        return getAnnotationProperty(uri.getURI());
    }

    default OntNDP getDataProperty(Resource uri) {
        return getDataProperty(uri.getURI());
    }

    default OntNOP getObjectProperty(Resource uri) {
        return getObjectProperty(uri.getURI());
    }

    default Stream<OntStatement> localStatements() {
        return localStatements(null, null, null);
    }

    default <E extends OntEntity> Stream<E> ontEntities(Class<E> type) {
        return ontObjects(type);
    }

    /**
     * Retrieves a {@link OntDT datatype} from the given literal.
     *
     * @param literal {@link Literal}, not {@code null}
     * @return {@link OntDT}
     */
    default OntDT getDatatype(Literal literal) {
        String uri = literal.getDatatypeURI();
        if (uri != null) {
            return getDatatype(uri);
        }
        String lang = literal.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            return getDatatype(RDF.langString);
        }
        return getDatatype(XSD.xstring);
    }

    /**
     * Returns a entity of the given type and with the specified URI, creating it if needed.
     *
     * @param type a class-type of entity
     * @param uri  String uri, not {@code null}
     * @param <E>  any subtype of {@link OntEntity}
     * @return {@link E}
     */
    default <E extends OntEntity> E fetchOntEntity(Class<E> type, String uri) {
        E res = getOntEntity(type, uri);
        return res == null ? createOntEntity(type, uri) : res;
    }

    /**
     * Lists all named class expressions (OWL classes).
     *
     * @return {@code Stream} of {@link OntClass Ontology Class}es
     * @since 1.4.0
     */
    default Stream<OntClass> classes() {
        return ontEntities(OntClass.class);
    }

    /**
     * Lists all annotation properties.
     *
     * @return {@code Stream} of {@link OntNAP Annotation Property}s
     * @since 1.4.0
     */
    default Stream<OntNAP> annotationProperties() {
        return ontEntities(OntNAP.class);
    }

    /**
     * Lists all data properties.
     *
     * @return {@code Stream} of {@link OntNDP Data Property}s
     * @since 1.4.0
     */
    default Stream<OntNDP> dataProperties() {
        return ontEntities(OntNDP.class);
    }

    /**
     * Lists all named object property expressions (object properties in short).
     *
     * @return {@code Stream} of {@link OntNOP Named Object Property}s
     * @since 1.4.0
     */
    default Stream<OntNOP> objectProperties() {
        return ontEntities(OntNOP.class);
    }

    /**
     * Lists all datatypes (named data range expressions).
     *
     * @return {@code Stream} of {@link OntDT Ontology Datatype}s
     * @since 1.4.0
     */
    default Stream<OntDT> datatypes() {
        return ontEntities(OntDT.class);
    }

    /**
     * Lists all named individuals,
     * i.e. all those individuals which have explicitly {@link OWL#NamedIndividual owl:NamedIndividual} declaration.
     *
     * @return {@code Stream} of {@link OntIndividual.Named Named Individual}s
     * @see #individuals()
     * @see OntCE#individuals()
     * @since 1.4.0
     */
    default Stream<OntIndividual.Named> namedIndividuals() {
        return ontEntities(OntIndividual.Named.class);
    }

    default <E extends OntEntity> E getOntEntity(Class<E> type, Resource uri) {
        return getOntEntity(type, uri.getURI());
    }

    /*
     * ===================================
     * Some common built-in OWL2 entities:
     * ===================================
     */

    default OntNAP getRDFSComment() {
        return getAnnotationProperty(RDFS.comment);
    }

    default OntNAP getRDFSLabel() {
        return getAnnotationProperty(RDFS.label);
    }

    default OntClass getOWLThing() {
        return getOntClass(OWL.Thing);
    }

    default OntClass getOWLNothing() {
        return getOntClass(OWL.Nothing);
    }

    default OntDT getRDFSLiteral() {
        return getDatatype(RDFS.Literal);
    }

    default OntNOP getOWLTopObjectProperty() {
        return getObjectProperty(OWL.topObjectProperty);
    }

    default OntNOP getOWLBottomObjectProperty() {
        return getObjectProperty(OWL.bottomObjectProperty);
    }

    default OntNDP getOWLTopDataProperty() {
        return getDataProperty(OWL.topDataProperty);
    }

    default OntNDP getOWLBottomDataProperty() {
        return getDataProperty(OWL.bottomDataProperty);
    }

    /**
     * Lists all individuals that participate in class assertion axioms.
     *
     * @return {@code Stream} of {@link OntIndividual}s
     * @since 1.3.0
     * @deprecated since 1.4.1: use {@link #individuals()} instead
     */
    @Deprecated
    default Stream<OntIndividual> classAssertions() {
        return individuals();
    }

    /**
     * Lists all built-in OWL entities,
     * that are present somewhere in the whole or only the base graph (depending on the second parameter).
     * <p>
     * The original comment:
     * The presence means that a builtin entity (e.g. a built-in OWL Class {@link #getOWLThing()} owl:Thing}),
     * is a part of some OWL statement (SPO) (e.g. {@code <SomeClass> rdfs:subClassOf owl:Thing}).
     * If an entity is only mentioned in some SPO, which is beyond the OWL2 syntax,
     * than it is not included in the returned {@code Stream}.
     * To list all model builtins (i.e. from sub-model hierarchy also,
     * not only from the base graph) the method {@link #ontBuiltins(Class)} can also be used.
     * Note that the result can be configured
     * through {@link ru.avicomp.ontapi.jena.impl.conf.OntPersonality.Builtins Builtins Vocabulary}.
     * <p>
     * Note: since 1.4.1 this functionality is scheduled to be deleted,
     * though it is possible that it will be returned after a time.
     * If you find this functionality useful, please contact me.
     *
     * @param type  a concrete class-type of entity
     * @param local if {@code true} only the base graph is considered
     * @param <E>   any subtype of {@link OntEntity}
     * @return {@code Stream} of builtin {@link OntEntity}s
     * @see <a href='https://github.com/avicomp/ont-api/issues/40'>functionality description</a>
     * @since 1.4.0
     * @deprecated since 1.4.1: marked as obsolete because it is almost useless at this time, see issue #40
     */
    @Deprecated
    default <E extends OntEntity> Stream<E> ontBuiltins(Class<E> type, boolean local) {
        if (local) return ontBuiltins(type);
        return Stream.concat(ontEntities(type), imports().flatMap(x -> x.ontBuiltins(type))).distinct();
    }

    /**
     * Lists all built-in OWL entities, that participate somewhere in the base graph.
     *
     * @param type a concrete class-type of entity
     * @param <E>  any subtype of {@link OntEntity}
     * @return <b>distinct</b> {@code Stream} of builtin entities
     * @see <a href='https://github.com/avicomp/ont-api/issues/40'>functionality description</a>
     * @since 1.4.0
     * @deprecated since 1.4.1: marked as obsolete because it is almost useless at this time, see issue #40
     */
    @Deprecated
    default <E extends OntEntity> Stream<E> ontBuiltins(Class<E> type) {
        return localStatements().flatMap(s -> Stream.of(s.getSubject(), s.getPredicate(), s.getObject()))
                .filter(x -> x.canAs(type)).map(o -> o.as(type))
                .filter(OntEntity::isBuiltIn)
                .distinct();
    }
}
