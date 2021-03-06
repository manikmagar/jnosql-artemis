/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.jnosql.artemis.column.query;


import org.jnosql.artemis.Repository;
import org.jnosql.artemis.column.ColumnTemplate;
import org.jnosql.artemis.reflection.ClassRepresentation;
import org.jnosql.artemis.reflection.ClassRepresentations;
import org.jnosql.artemis.reflection.Reflections;

import java.lang.reflect.ParameterizedType;


/**
 * Proxy handle to generate {@link Repository}
 *
 * @param <T>  the type
 * @param <ID> the ID type
 */
class ColumnRepositoryProxy<T, ID> extends AbstractColumnRepositoryProxy {


    private final ColumnTemplate template;

    private final ColumnRepository repository;

    private final Reflections reflections;

    private final ClassRepresentation classRepresentation;

    private final ColumnQueryParser queryParser;

    private final ColumnQueryDeleteParser deleteParser;


    ColumnRepositoryProxy(ColumnTemplate template, ClassRepresentations classRepresentations, Class<?> repositoryType,
                          Reflections reflections) {
        this.template = template;
        Class<T> typeClass = Class.class.cast(ParameterizedType.class.cast(repositoryType.getGenericInterfaces()[0])
                .getActualTypeArguments()[0]);
        this.classRepresentation = classRepresentations.get(typeClass);
        this.repository = new ColumnRepository(template, classRepresentation);
        this.reflections = reflections;
        this.queryParser = new ColumnQueryParser();
        this.deleteParser = new ColumnQueryDeleteParser();
    }

    @Override
    protected Repository getRepository() {
        return repository;
    }

    @Override
    protected ClassRepresentation getClassRepresentation() {
        return classRepresentation;
    }

    @Override
    protected ColumnQueryParser getQueryParser() {
        return queryParser;
    }

    @Override
    protected ColumnQueryDeleteParser getDeleteParser() {
        return deleteParser;
    }

    @Override
    protected ColumnTemplate getTemplate() {
        return template;
    }


    class ColumnRepository extends AbstractColumnRepository implements Repository {

        private final ColumnTemplate template;

        private final ClassRepresentation classRepresentation;

        ColumnRepository(ColumnTemplate template, ClassRepresentation classRepresentation) {
            this.template = template;
            this.classRepresentation = classRepresentation;
        }

        @Override
        protected ColumnTemplate getTemplate() {
            return template;
        }

        @Override
        protected ClassRepresentation getClassRepresentation() {
            return classRepresentation;
        }

        @Override
        protected Reflections getReflections() {
            return reflections;
        }

    }
}
