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
package org.jnosql.artemis.reflection;

import org.jnosql.artemis.Embeddable;
import org.jnosql.artemis.Entity;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * enum that contains kinds of annotations to fields on java.
 */
public enum FieldType {
    EMBEDDED, MAP, COLLECTION, DEFAULT;

    /**
     * select you the kind of annotation on field and then define a enum type, follow the sequences:
     * <ul>
     * <li>Collection</li>
     * <li>Map</li>
     * <li>embedded</li>
     * </ul>.
     *
     * @param field - the field with annotation
     * @return the type
     */
    public static FieldType of(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            return COLLECTION;
        }
        if (Map.class.isAssignableFrom(field.getType())) {
            return MAP;
        }
        if (field.getType().isAnnotationPresent(Embeddable.class) ||
                field.getType().isAnnotationPresent(Entity.class)) {
            return EMBEDDED;
        }

        return DEFAULT;
    }

}
