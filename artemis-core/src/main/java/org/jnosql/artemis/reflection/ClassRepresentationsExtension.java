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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * This class is a CDI extension to load all class that has {@link Entity} annotation.
 * This extension will load all Classes and put in a map.
 * Where the key is {@link Class#getName()} and the value is {@link ClassRepresentation}
 */
@ApplicationScoped
public class ClassRepresentationsExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(ClassRepresentationsExtension.class.getName());

    private final ClassConverter classConverter = new ClassConverter(new DefaultReflections());

    private final Map<String, ClassRepresentation> representations = new ConcurrentHashMap<>();

    private final Map<Class, ClassRepresentation> classes = new ConcurrentHashMap<>();


    /**
     * Event observer
     *
     * @param target the target
     * @param <T>    the type
     */
    public <T> void initializePropertyLoading(final @Observes ProcessAnnotatedType<T> target) {

        AnnotatedType<T> at = target.getAnnotatedType();
        if (at.isAnnotationPresent(Entity.class)) {
            Class<T> javaClass = target.getAnnotatedType().getJavaClass();
            LOGGER.info("scanning type: " + javaClass.getName());
            ClassRepresentation classRepresentation = classConverter.create(javaClass);
            representations.put(classRepresentation.getName(), classRepresentation);
            classes.put(javaClass, classRepresentation);
        } else if (at.isAnnotationPresent(Embeddable.class)) {
            Class<T> javaClass = target.getAnnotatedType().getJavaClass();
            ClassRepresentation classRepresentation = classConverter.create(javaClass);
            classes.put(javaClass, classRepresentation);
        }

    }


    /**
     * Returns the representations loaded in CDI startup
     *
     * @return the class loaded
     */
    public Map<String, ClassRepresentation> getRepresentations() {
        return representations;
    }

    /**
     * Returns all class found in the process grouped by Java class
     *
     * @return the map instance
     */
    public Map<Class, ClassRepresentation> getClasses() {
        return classes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClassRepresentationsExtension{");
        sb.append("classConverter=").append(classConverter);
        sb.append(", representations-size=").append(representations.size());
        sb.append(", classes=").append(classes);
        sb.append('}');
        return sb.toString();
    }
}
