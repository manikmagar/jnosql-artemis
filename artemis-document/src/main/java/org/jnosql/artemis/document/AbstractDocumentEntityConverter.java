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
package org.jnosql.artemis.document;

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
import org.jnosql.diana.api.document.Document;
import org.jnosql.diana.api.document.DocumentEntity;

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
 * Template method to {@link DocumentEntityConverter}
 */
public abstract class AbstractDocumentEntityConverter  implements DocumentEntityConverter {

    protected abstract ClassRepresentations getClassRepresentations();

    protected abstract Reflections getReflections();

    protected abstract Converters getConverters();

    private final DocumentFieldConverterFactory converterFactory = new DocumentFieldConverterFactory();


    @Override
    public DocumentEntity toDocument(Object entityInstance) {
        requireNonNull(entityInstance, "Object is required");
        ClassRepresentation representation = getClassRepresentations().get(entityInstance.getClass());
        DocumentEntity entity = DocumentEntity.of(representation.getName());
        representation.getFields().stream()
                .map(f -> to(f, entityInstance))
                .filter(FieldValue::isNotEmpty)
                .map(f -> f.toDocument(this, getConverters()))
                .forEach(entity::add);
        return entity;

    }

    @Override
    public <T> T toEntity(Class<T> entityClass, DocumentEntity entity) {
        requireNonNull(entity, "entity is required");
        requireNonNull(entityClass, "entityClass is required");
        return toEntity(entityClass, entity.getDocuments());

    }

    protected  <T> T toEntity(Class<T> entityClass, List<Document> documents) {
        ClassRepresentation representation = getClassRepresentations().get(entityClass);
        T instance = getReflections().newInstance(representation.getConstructor());
        return convertEntity(documents, representation, instance);
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T toEntity(DocumentEntity entity) {
        requireNonNull(entity, "entity is required");
        ClassRepresentation representation = getClassRepresentations().findByName(entity.getName());
        T instance = getReflections().newInstance(representation.getConstructor());
        return convertEntity(entity.getDocuments(), representation, instance);
    }

    private <T> T convertEntity(List<Document> documents, ClassRepresentation representation, T instance) {
        Map<String, FieldRepresentation> fieldsGroupByName = representation.getFieldsGroupByName();

        List<String> names = documents.stream().map(Document::getName).sorted().collect(Collectors.toList());
        Predicate<String> existField = k -> Collections.binarySearch(names, k) >= 0;

        fieldsGroupByName.keySet().stream()
                .filter(existField.or(k -> EMBEDDED.equals(fieldsGroupByName.get(k).getType())))
                .forEach(feedObject(instance, documents, fieldsGroupByName));

        return instance;
    }

    protected <T> Consumer<String> feedObject(T instance, List<Document> documents, Map<String, FieldRepresentation> fieldsGroupByName) {
        return k -> {
            Optional<Document> document = documents.stream().filter(c -> c.getName().equals(k)).findFirst();

            FieldRepresentation field = fieldsGroupByName.get(k);
            DocumentFieldConverter fieldConverter = converterFactory.get(field);
            fieldConverter.convert(instance, documents, document, field);
        };
    }


    private DocumentFieldValue to(FieldRepresentation field, Object entityInstance) {
        Object value = getReflections().getValue(entityInstance, field.getNativeField());
        return DocumentFieldValue.of(value, field);
    }


    private interface DocumentFieldConverter {

        <T> void convert(T instance, List<Document> documents, Optional<Document> document, FieldRepresentation field);
    }

    private class DocumentFieldConverterFactory {

        private final EmbeddedFieldConverter embeddedFieldConverter = new EmbeddedFieldConverter();
        private final DefaultConverter defaultConverter = new DefaultConverter();
        private final CollectionEmbeddableConverter embeddableConverter = new CollectionEmbeddableConverter();

        DocumentFieldConverter get(FieldRepresentation field) {
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

    private class EmbeddedFieldConverter implements DocumentFieldConverter {

        @Override
        public <T> void convert(T instance, List<Document> documents, Optional<Document> document,
                                FieldRepresentation field) {

            if (document.isPresent()) {
                Document sudDocument = document.get();
                Object value = sudDocument.get();
                if (Map.class.isInstance(value)) {
                    Map map = Map.class.cast(value);
                    List<Document> embeddedDocument = new ArrayList<>();
                    for (Object key : map.keySet()) {
                        embeddedDocument.add(Document.of(key.toString(), map.get(key)));
                    }
                    getReflections().setValue(instance, field.getNativeField(), toEntity(field.getNativeField().getType(), embeddedDocument));
                } else {
                    getReflections().setValue(instance, field.getNativeField(), toEntity(field.getNativeField().getType(), sudDocument.get(new TypeReference<List<Document>>() {
                    })));
                }

            } else {
                getReflections().setValue(instance, field.getNativeField(), toEntity(field.getNativeField().getType(), documents));
            }
        }
    }

    private class DefaultConverter  implements DocumentFieldConverter {

        @Override
        public <T> void convert(T instance, List<Document> documents, Optional<Document> document,
                                FieldRepresentation field) {
            Value value = document.get().getValue();
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

    private class CollectionEmbeddableConverter implements DocumentFieldConverter {

        @Override
        public <T> void convert(T instance, List<Document> documents, Optional<Document> document,
                                FieldRepresentation field) {
            document.ifPresent(convertDocument(instance, field));
        }

        private <T> Consumer<Document> convertDocument(T instance, FieldRepresentation field) {
            return document -> {
                GenericFieldRepresentation genericField = GenericFieldRepresentation.class.cast(field);
                Collection collection = genericField.getCollectionInstance();
                List<List<Document>> embeddable = (List<List<Document>>) document.get();
                for (List<Document> documentList : embeddable) {
                    Object element = AbstractDocumentEntityConverter.this.toEntity(genericField.getElementType(), documentList);
                    collection.add(element);
                }
                getReflections().setValue(instance, field.getNativeField(), collection);
            };
        }
    }
}
