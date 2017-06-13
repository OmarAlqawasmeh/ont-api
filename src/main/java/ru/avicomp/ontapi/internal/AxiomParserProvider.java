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

package ru.avicomp.ontapi.internal;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;
import ru.avicomp.ontapi.OntApiException;

/**
 * Axiom Graph Translator loader.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParserProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxiomParserProvider.class);

    public static Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> getParsers() {
        return ParserHolder.PARSERS;
    }

    public static <A extends OWLAxiom> AxiomTranslator<A> get(Class<A> type) {
        return get(AxiomType.getTypeForClass(type));
    }

    public static <A extends OWLAxiom> AxiomTranslator<A> get(A axiom) {
        return get(OntApiException.notNull(axiom, "Null axiom.").getAxiomType());
    }

    @SuppressWarnings("unchecked")
    public static <T extends OWLAxiom> AxiomTranslator<T> get(AxiomType<? extends OWLAxiom> type) {
        return OntApiException.notNull((AxiomTranslator<T>) getParsers().get(OntApiException.notNull(type, "Null axiom type")), "Can't find parser for axiom " + type.getActualClass());
    }

    private static class ParserHolder {
        private static final Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> PARSERS = init();

        static {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("There are following axiom-parsers (" + PARSERS.size() + "): ");
                PARSERS.forEach((type, parser) -> LOGGER.debug(type + " ::: " + parser.getClass()));
            }
        }

        private static Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> init() {
            Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> res = new HashMap<>();
            Set<Class<? extends AxiomTranslator>> parserClasses = collectParserClasses();
            AxiomType.AXIOM_TYPES.forEach(type -> {
                Class<? extends AxiomTranslator> parserClass = findParserClass(parserClasses, type);
                try {
                    res.put(type, parserClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new OntApiException("Can't instance parser for type: " + type, e);
                }
            });
            return res;
        }

        private static Class<? extends AxiomTranslator> findParserClass(Set<Class<? extends AxiomTranslator>> classes, AxiomType<? extends OWLAxiom> type) {
            return classes.stream()
                    .filter(p -> isRelatedToAxiom(p, type.getActualClass()))
                    .findFirst()
                    .orElseThrow(() -> new OntApiException("Can't find parser class for type " + type));
        }

        private static boolean isRelatedToAxiom(Class<? extends AxiomTranslator> parserClass, Class<? extends OWLAxiom> actualClass) {
            ParameterizedType type = ((ParameterizedType) parserClass.getGenericSuperclass());
            if (type == null) return false;
            for (Type t : type.getActualTypeArguments()) {
                if (actualClass.getName().equals(t.getTypeName())) return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private static Set<Class<? extends AxiomTranslator>> collectParserClasses() {
            try {
                Set<ClassPath.ClassInfo> classes = ClassPath.from(Thread.currentThread().getContextClassLoader()).getTopLevelClasses(AxiomTranslator.class.getPackage().getName());
                Stream<Class> res = classes.stream().map(ParserHolder::parserClass).filter(c -> !Modifier.isAbstract(c.getModifiers())).filter(AxiomTranslator.class::isAssignableFrom);
                return res.map((Function<Class, Class<? extends AxiomTranslator>>) c -> c).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new OntApiException("Can't collect parsers classes", e);
            }
        }

        private static Class parserClass(ClassPath.ClassInfo info) {
            try {
                return Class.forName(info.getName());
            } catch (ClassNotFoundException e) {
                throw new OntApiException("Can't find class " + info, e);
            }
        }
    }


}
