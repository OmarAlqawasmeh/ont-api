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

package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal Object Factory to map {@link OntObject} =&gt; {@link OWLObject}.
 * Used by the {@link InternalModel} while read objects from the graph.
 * It is a functional analogue of {@code uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals}.
 * <p>
 * Created by @szuev on 14.03.2018.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternals.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals</a>
 * @see ONTObject
 */
public interface InternalObjectFactory {

    InternalObjectFactory DEFAULT = new SimpleObjectFactory(OntManagers.getDataFactory());

    DataFactory getOWLDataFactory();

    ONTObject<OWLClass> getClass(OntClass ce);

    ONTObject<? extends OWLClassExpression> getClass(OntCE ce);

    ONTObject<OWLDatatype> getDatatype(OntDT dr);

    ONTObject<? extends OWLDataRange> getDatatype(OntDR dr);

    ONTObject<OWLObjectProperty> getProperty(OntNOP nop);

    ONTObject<OWLAnnotationProperty> getProperty(OntNAP nap);

    ONTObject<OWLDataProperty> getProperty(OntNDP ndp);

    ONTObject<? extends OWLObjectPropertyExpression> getProperty(OntOPE.Inverse iop);

    ONTObject<OWLNamedIndividual> getIndividual(OntIndividual.Named i);

    ONTObject<OWLAnonymousIndividual> getIndividual(OntIndividual.Anonymous i);

    ONTObject<OWLFacetRestriction> getFacetRestriction(OntFR fr);

    ONTObject<OWLLiteral> getLiteral(Literal literal);

    ONTObject<OWLAnnotation> getAnnotation(OntStatement s);

    ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable var);

    ONTObject<? extends SWRLIArgument> getSWRLArgument(OntSWRL.IArg arg);

    ONTObject<? extends SWRLDArgument> getSWRLArgument(OntSWRL.DArg arg);

    ONTObject<? extends SWRLAtom> getSWRLAtom(OntSWRL.Atom atom);

    /**
     * Gets an {@link IRI} that is wrapped as {@code ONTObject} from the specified {@code String}.
     *
     * @param uri {@code String}, not {@code null}
     * @return {@link ONTObject} that wraps {@link IRI}
     * @see #toIRI(String)
     */
    ONTObject<IRI> getIRI(String uri);

    /**
     * Gets an {@link IRI} from the {@code String}.
     *
     * @param str URI, not {@code null}
     * @return {@link IRI}
     * @see #getIRI(String)
     */
    default IRI toIRI(String str) {
        return IRI.create(OntApiException.notNull(str, "Null IRI."));
    }

    /**
     * Gets a {@code Collection} of axiom's {@link OWLAnnotation}s which are wrapped as {@link ONTObject}-containers.
     *
     * @param axiom  {@link OntStatement} - the root statement of an axiom, not {@code null}
     * @param config {@link InternalConfig} the configuration, to
     * @return a {@code Collection} of {@link OWLAnnotation}s as {@link ONTObject}s
     */
    default Collection<ONTObject<OWLAnnotation>> getAnnotations(OntStatement axiom, InternalConfig config) {
        Map<OWLAnnotation, ONTObject<OWLAnnotation>> res = new HashMap<>();
        ReadHelper.listAnnotations(axiom, config, this).forEachRemaining(x -> WithMerge.add(res, x));
        return res.values();
    }

    /**
     * Gets an {@link OWLAnnotationSubject} from the the {@code OntObject}-resource.
     *
     * @param subject {@link OntObject}, either URI-resource or anonymous individual, not {@code null}
     * @return {@link ONTObject} of {@link OWLAnnotationSubject}
     */
    default ONTObject<? extends OWLAnnotationSubject> getSubject(OntObject subject) {
        if (OntApiException.notNull(subject, "Null resource").isURIResource()) {
            return getIRI(subject.getURI());
        }
        if (subject.isAnon()) {
            return getIndividual(OntModels.asAnonymousIndividual(subject));
        }
        throw new OntApiException.IllegalArgument("Not an AnnotationSubject " + subject);
    }

    /**
     * Gets an {@link OWLAnnotationValue} for the the {@code RDFNode}
     *
     * @param value {@link OntObject}, either URI-resource, anonymous individual or literal, not {@code null}
     * @return {@link ONTObject} of {@link OWLAnnotationValue}
     */
    default ONTObject<? extends OWLAnnotationValue> getValue(RDFNode value) {
        if (OntApiException.notNull(value, "Null node").isLiteral()) {
            return getLiteral(value.asLiteral());
        }
        if (value.isURIResource()) {
            return getIRI(value.asResource().getURI());
        }
        if (value.isAnon()) {
            return getIndividual(OntModels.asAnonymousIndividual(value));
        }
        throw new OntApiException.IllegalArgument("Not an AnnotationValue " + value);
    }

    /**
     * Gets an {@link OWLIndividual} wrapped in {@link ONTObject} for the given {@link OntIndividual}.
     *
     * @param individual {@link OntIndividual}, not {@code null}
     * @return {@link ONTObject} of {@link OWLIndividual}
     */
    default ONTObject<? extends OWLIndividual> getIndividual(OntIndividual individual) {
        if (OntApiException.notNull(individual, "Null individual").isURIResource()) {
            return getIndividual(individual.as(OntIndividual.Named.class));
        }
        return getIndividual(individual.as(OntIndividual.Anonymous.class));
    }

    /**
     * Gets an {@link OWLEntity} as {@link ONTObject} from the {@link OntEntity}.
     *
     * @param entity {@link OntEntity}, not {@code null}
     * @return {@link ONTObject} of {@link OWLEntity}
     */
    default ONTObject<? extends OWLEntity> getEntity(OntEntity entity) {
        Class<? extends OntEntity> type = OntModels.getOntType(OntApiException.notNull(entity, "Null entity"));
        if (type == OntClass.class) {
            return getClass((OntClass) entity);
        }
        if (type == OntDT.class) {
            return getDatatype((OntDT) entity);
        }
        if (type == OntIndividual.Named.class) {
            return getIndividual((OntIndividual.Named) entity);
        }
        if (type == OntNAP.class) {
            return getProperty((OntNAP) entity);
        }
        if (type == OntNDP.class) {
            return getProperty((OntNDP) entity);
        }
        if (type == OntNOP.class) {
            return getProperty((OntNOP) entity);
        }
        throw new OntApiException.IllegalArgument("Unsupported " + entity);
    }

    /**
     * Gets an {@link OWLPropertyExpression} as {@link ONTObject} from the property expression.
     * @param property {@link OntPE}, not {@code null}
     * @return {@link ONTObject} of {@link OWLPropertyExpression}
     */
    default ONTObject<? extends OWLPropertyExpression> getProperty(OntPE property) {
        if (OntApiException.notNull(property, "Null property expression.").canAs(OntNAP.class)) {
            return getProperty(property.as(OntNAP.class));
        }
        return getProperty((OntDOP) property);
    }

    /**
     * Gets an {@link OWLPropertyExpression} as {@link ONTObject} from the data or object property expression.
     *
     * @param property {@link OntDOP}, not {@code null}
     * @return {@link ONTObject} of {@link OWLPropertyExpression}
     */
    default ONTObject<? extends OWLPropertyExpression> getProperty(OntDOP property) {
        // process Object Properties first to match OWL-API-impl behaviour
        if (OntApiException.notNull(property, "Null Data/Object property").canAs(OntOPE.class)) {
            return getProperty(property.as(OntOPE.class));
        }
        if (property.canAs(OntNDP.class)) {
            return getProperty(property.as(OntNDP.class));
        }
        throw new OntApiException("Unsupported property " + property);
    }

    /**
     * Gets an {@link OWLObjectPropertyExpression} as {@link ONTObject} from the {@link OntOPE}.
     *
     * @param property {@link OntOPE}, not {@code null}
     * @return {@link ONTObject} of {@link OWLObjectPropertyExpression}
     */
    default ONTObject<? extends OWLObjectPropertyExpression> getProperty(OntOPE property) {
        if (OntApiException.notNull(property, "Null object property.").isAnon()) {
            return getProperty(property.as(OntOPE.Inverse.class));
        }
        return getProperty(property.as(OntNOP.class));
    }

}
