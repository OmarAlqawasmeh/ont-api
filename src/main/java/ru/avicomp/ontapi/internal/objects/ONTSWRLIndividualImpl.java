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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ModelObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.model.OntSWRL;

import java.util.function.Supplier;

/**
 * A {@link SWRLIndividualArgument} implementation that is also {@link ONTObject}.
 * <p>
 * Created by @ssz on 22.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLIndividualArgumentImpl
 * @see org.semanticweb.owlapi.model.SWRLIArgument
 * @see OntSWRL.IArg
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTSWRLIndividualImpl extends ONTResourceImpl
        implements SWRLIndividualArgument, ONTObject<SWRLIndividualArgument> {

    protected ONTSWRLIndividualImpl(String uri, Supplier<OntGraphModel> m) {
        super(uri, m);
    }

    protected ONTSWRLIndividualImpl(BlankNodeId id, Supplier<OntGraphModel> m) {
        super(id, m);
    }

    /**
     * Wraps the given {@link OntIndividual} as {@link SWRLIndividualArgument} and {@link ONTObject}.
     *
     * @param i {@link OntIndividual}, not {@code null}, must be anonymous
     * @param m a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTSWRLIndividualImpl} instance
     */
    public static ONTSWRLIndividualImpl create(OntIndividual i, Supplier<OntGraphModel> m) {
        if (i.isURIResource()) {
            return new ONTSWRLIndividualImpl(i.getURI(), m);
        }
        return new ONTSWRLIndividualImpl(i.asNode().getBlankNodeId(), m);
    }

    @Override
    public SWRLIndividualArgument getOWLObject() {
        return this;
    }

    @Override
    protected BlankNodeId getBlankNodeId() {
        if (node instanceof BlankNodeId) return (BlankNodeId) node;
        throw new IllegalStateException();
    }

    @Override
    protected String getURI() {
        if (node instanceof String) return (String) node;
        throw new IllegalStateException();
    }

    @Override
    public Node asNode() {
        if (node instanceof String)
            return NodeFactory.createURI((String) node);
        if (node instanceof BlankNodeId)
            return NodeFactory.createBlankNode((BlankNodeId) node);
        return wrongState();
    }

    private <X> X wrongState() {
        throw new IllegalStateException("Must be URI or b-node: " + node);
    }

    @Override
    public OntSWRL.IArg asResource() {
        return as(OntSWRL.IArg.class);
    }

    public OntIndividual asIndividual() {
        return as(OntIndividual.class);
    }

    @Override
    public OWLIndividual getIndividual() {
        return getONTIndividual().getOWLObject();
    }

    public ONTObject<? extends OWLIndividual> getONTIndividual() {
        InternalObjectFactory of = getObjectFactory();
        if (of instanceof ModelObjectFactory) {
            if (node instanceof String)
                return ((ModelObjectFactory) of).getNamedIndividual((String) node);
            if (node instanceof BlankNodeId)
                return ((ModelObjectFactory) of).getAnonymousIndividual((BlankNodeId) node);
            return wrongState();
        }
        return of.get(asIndividual());
    }

}