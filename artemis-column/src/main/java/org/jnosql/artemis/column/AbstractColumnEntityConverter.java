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
package org.jnosql.artemis.column;

import org.jnosql.artemis.AttributeConverter;
import org.jnosql.artemis.Converters;
import org.jnosql.artemis.reflection.ClassRepresentation;
import org.jnosql.artemis.reflection.ClassRepresentations;
import org.jnosql.artemis.reflection.FieldRepresentation;
import org.jnosql.artemis.reflection.FieldValue;
import org.jnosql.artemis.reflection.GenericFieldRepresentation;
import org.jnosql.artemis.reflection.Reflections;
import org.jnosql.diana.api.TypeReference;
import org.jnosql.diana.api.Value;
import org.jnosql.diana.api.column.Column;
import org.jnosql.diana.api.column.ColumnEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.jnosql.artemis.reflection.FieldType.COLLECTION;
import static org.jnosql.artemis.reflection.FieldType.EMBEDDED;

/**
 * Template method to {@link ColumnEntityConverter}
 */
public abstract class AbstractColumnEntityConverter implements ColumnEntityConverter {

    private final ColumnFieldConverterFactory converterFactory = new ColumnFieldConverterFactory();


    protected abstract ClassRepresentations getClassRepresentations();

    protected abstract Reflections getReflections();

    protected abstract Converters getConverters();

    @Override
    public ColumnEntity toColumn(Object entityInstance) {
        requireNonNull(entityInstance, "Object is required");
        ClassRepresentation representation = getClassRepresentations().get(entityInstance.getClass());
        ColumnEntity entity = ColumnEntity.of(representation.getName());
        representation.getFields().stream()
                .map(f -> to(f, entityInstance))
                .filter(FieldValue::isNotEmpty)
                .map(f -> f.toColumn(this, getConverters()))
                .forEach(entity::add);
        return entity;
    }

    @Override
    public <T> T toEntity(Class<T> entityClass, ColumnEntity entity) {
        requireNonNull(entity, "entity is required");
        requireNonNull(entityClass, "entityClass is required");
        return toEntity(entityClass, entity.getColumns());
    }

    protected  <T> T toEntity(Class<T> entityClass, List<Column> columns) {
        ClassRepresentation representation = getClassRepresentations().get(entityClass);
        T instance = getReflections().newInstance(representation.getConstructor());
        return convertEntity(columns, representation, instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T toEntity(ColumnEntity entity) {
        requireNonNull(entity, "entity is required");
        ClassRepresentation representation = getClassRepresentations().findByName(entity.getName());
        T instance = getReflections().newInstance(representation.getConstructor());
        return convertEntity(entity.getColumns(), representation, instance);
    }

    protected ColumnFieldValue to(FieldRepresentation field, Object entityInstance) {
        Object value = getReflections().getValue(entityInstance, field.getNativeField());
        return DefaultColumnFieldValue.of(value, field);
    }

    protected  <T> Consumer<String> feedObject(T instance, List<Column> columns, Map<String, FieldRepresentation> fieldsGroupByName) {
        return (String k) -> {
            Optional<Column> column = columns.stream().filter(c -> c.getName().equals(k)).findFirst();
            FieldRepresentation field = fieldsGroupByName.get(k);
            ColumnFieldConverter fieldConverter = converterFactory.get(field);
            fieldConverter.convert(instance, columns, column, field);
        };
    }

    private <T> T convertEntity(List<Column> columns, ClassRepresentation representation, T instance) {
        Map<String, FieldRepresentation> fieldsGroupByName = representation.getFieldsGroupByName();
        List<String> names = columns.stream().map(Column::getName).sorted().collect(Collectors.toList());
        Predicate<String> existField = k -> Collections.binarySearch(names, k) >= 0;
        fieldsGroupByName.keySet().stream()
                .filter(existField.or(k -> EMBEDDED.equals(fieldsGroupByName.get(k).getType())))
                .forEach(feedObject(instance, columns, fieldsGroupByName));

        return instance;
    }


    private class ColumnFieldConverterFactory {

        private final EmbeddedFieldConverter embeddedFieldConverter = new EmbeddedFieldConverter();
        private final DefaultConverter defaultConverter = new DefaultConverter();
        private final CollectionEmbeddableConverter embeddableConverter = new CollectionEmbeddableConverter();

        ColumnFieldConverter get(FieldRepresentation field) {
            if (EMBEDDED.equals(field.getType())) {
                return embeddedFieldConverter;
            } else if (isCollectionEmbeddable(field)) {
                return embeddableConverter;
            } else {
                return defaultConverter;
            }
        }

        private boolean isCollectionEmbeddable(FieldRepresentation field) {
            return COLLECTION.equals(field.getType()) && GenericFieldRepresentation.class.cast(field).isEmbeddable();
        }
    }

    private interface ColumnFieldConverter {

        <T> void convert(T instance, List<Column> columns, Optional<Column> column, FieldRepresentation field);
    }

    private class EmbeddedFieldConverter implements ColumnFieldConverter {

        @Override
        public <T> void convert(T instance, List<Column> columns, Optional<Column> column, FieldRepresentation field) {
            if (column.isPresent()) {
                Column subColumn = column.get();
                Object value = subColumn.get();
                if (Map.class.isInstance(value)) {
                    Map map = Map.class.cast(value);
                    List<Column> embeddedColumns = new ArrayList<>();
                    for (Object key : map.keySet()) {
                        embeddedColumns.add(Column.of(key.toString(), map.get(key)));
                    }
                    getReflections().setValue(instance, field.getNativeField(), toEntity(field.getNativeField().getType(), embeddedColumns));
                } else {
                    getReflections().setValue(instance, field.getNativeField(), toEntity(field.getNativeField().getType(),
                            subColumn.get(new TypeReference<List<Column>>() {
                            })));
                }

            } else {
                getReflections().setValue(instance, field.getNativeField(), toEntity(field.getNativeField().getType(), columns));
            }
        }
    }

    private class DefaultConverter implements ColumnFieldConverter {

        @Override
        public <T> void convert(T instance, List<Column> columns, Optional<Column> column, FieldRepresentation field) {
            Value value = column.get().getValue();
            Optional<Class<? extends AttributeConverter>> converter = field.getConverter();
            if (converter.isPresent()) {
                AttributeConverter attributeConverter = getConverters().get(converter.get());
                Object attributeConverted = attributeConverter.convertToEntityAttribute(value.get());
                getReflections().setValue(instance, field.getNativeField(), field.getValue(Value.of(attributeConverted)));
            } else {
                getReflections().setValue(instance, field.getNativeField(), field.getValue(value));
            }
        }
    }

    private class CollectionEmbeddableConverter implements ColumnFieldConverter {

        @Override
        public <T> void convert(T instance, List<Column> columns, Optional<Column> column, FieldRepresentation field) {
            column.ifPresent(convertColumn(instance, field));
        }

        private <T> Consumer<Column> convertColumn(T instance, FieldRepresentation field) {
            return column -> {
                GenericFieldRepresentation genericField = GenericFieldRepresentation.class.cast(field);
                Collection collection = genericField.getCollectionInstance();
                List<List<Column>> embeddable = (List<List<Column>>) column.get();
                for (List<Column> columnList : embeddable) {
                    Object element = AbstractColumnEntityConverter.this.toEntity(genericField.getElementType(), columnList);
                    collection.add(element);
                }
                getReflections().setValue(instance, field.getNativeField(), collection);
            };
        }
    }
}
