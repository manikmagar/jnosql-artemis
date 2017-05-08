/*
 * Copyright 2017 Otavio Santana and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jnosql.artemis.column.query;

import org.jnosql.artemis.IdNotFoundException;
import org.jnosql.artemis.Repository;
import org.jnosql.artemis.column.ColumnTemplate;
import org.jnosql.artemis.reflection.ClassRepresentation;
import org.jnosql.artemis.reflection.FieldRepresentation;
import org.jnosql.artemis.reflection.Reflections;
import org.jnosql.diana.api.column.Column;
import org.jnosql.diana.api.column.ColumnCondition;
import org.jnosql.diana.api.column.ColumnDeleteQuery;
import org.jnosql.diana.api.column.ColumnQuery;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * The {@link Repository} template method
 */
public abstract class AbstractColumnRepository<T, ID> implements Repository<T, ID> {

    private static final Supplier<IdNotFoundException> KEY_NOT_FOUND_EXCEPTION_SUPPLIER = ()
            -> new IdNotFoundException("To use this resource you must annotaded a fiel with @org.jnosql.artemisId");

    protected abstract ColumnTemplate getTemplate();

    protected abstract ClassRepresentation getClassRepresentation();

    protected abstract Reflections getReflections();

    @Override
    public T save(T entity) throws NullPointerException {
        Objects.requireNonNull(entity, "Entity is required");
        Object id = getReflections().getValue(entity, getIdField().getField());
        if (existsById((ID) id)) {
            return getTemplate().update(entity);
        } else {
            return getTemplate().insert(entity);
        }
    }

    @Override
    public T save(T entity, Duration ttl) {
        Objects.requireNonNull(entity, "Entity is required");
        Object id = getReflections().getValue(entity, getIdField().getField());
        if (existsById((ID) id)) {
            return getTemplate().update(entity);
        } else {
            return getTemplate().insert(entity, ttl);
        }
    }

    @Override
    public Iterable<T> save(Iterable<T> entities) throws NullPointerException {
        return getTemplate().insert(entities);
    }

    @Override
    public Iterable<T> save(Iterable<T> entities, Duration ttl) throws NullPointerException {
        return getTemplate().insert(entities, ttl);
    }

    @Override
    public void deleteById(ID id) throws NullPointerException {
        requireNonNull(id, "is is required");
        ColumnDeleteQuery query = ColumnDeleteQuery.of(getClassRepresentation().getName());
        String columnName = this.getIdField().getName();
        query.with(ColumnCondition.eq(Column.of(columnName, id)));
        getTemplate().delete(query);
    }

    @Override
    public void deleteById(Iterable<ID> ids) throws NullPointerException {
        requireNonNull(ids, "ids is required");
        ids.forEach(this::deleteById);
    }

    @Override
    public void delete(Iterable<T> entities) throws NullPointerException {
        requireNonNull(entities, "entities is required");
        entities.forEach(this::delete);
    }

    @Override
    public void delete(T entity) throws NullPointerException {
        requireNonNull(entity, "entity is required");
        Object idValue = getReflections().getValue(entity, this.getIdField().getField());
        requireNonNull(idValue, "id value is required");
        deleteById((ID) idValue);
    }

    @Override
    public Optional<T> findById(ID id) throws NullPointerException {
        requireNonNull(id, "id is required");

        ColumnQuery query = ColumnQuery.of(getClassRepresentation().getName());
        String columnName = this.getIdField().getName();
        query.with(ColumnCondition.eq(Column.of(columnName, id)));
        return getTemplate().singleResult(query);
    }

    @Override
    public Iterable<T> findById(Iterable<ID> ids) throws NullPointerException {
        requireNonNull(ids, "ids is required");
        return (Iterable) stream(ids.spliterator(), false)
                .flatMap(optionalToStream()).collect(Collectors.toList());
    }

    private FieldRepresentation getIdField() {
        return getClassRepresentation().getId().orElseThrow(KEY_NOT_FOUND_EXCEPTION_SUPPLIER);
    }

    private Function optionalToStream() {
        return id -> {
            Optional entity = this.findById((ID) id);
            return entity.isPresent() ? Stream.of(entity.get()) : Stream.empty();
        };
    }

    @Override
    public boolean existsById(ID id) throws NullPointerException {
        return findById(id).isPresent();
    }

}
