package org.jnosql.artemis.reflection;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.jnosql.artemis.Entity;

@ApplicationScoped
public class Reflections {

    /**
     * Return The Object from the Field.
     *
     * @param object the object
     * @param field  the field to return object
     * @return - the field value in Object
     */
    public Object getMethod(Object object, Field field) {

        try {
            return field.get(object);
        } catch (Exception exception) {
            Logger.getLogger(Reflections.class.getName()).log(Level.SEVERE, null, exception);
        }
        return null;
    }

    /**
     * Set the field in the Object.
     *
     * @param object the object
     * @param field  the field to return object
     * @param value  the value to object
     * @return - if the operation was execute with success
     */
    public boolean setMethod(Object object, Field field, Object value) {
        try {

            field.set(object, value);

        } catch (Exception exception) {
            Logger.getLogger(Reflections.class.getName()).log(Level.SEVERE, null, exception);
            return false;
        }
        return true;
    }

    /**
     * Create new instance of this class.
     *
     * @param clazz the class to create object
     * @return the new instance that class
     */
    public Object newInstance(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception exception) {
            Logger.getLogger(Reflections.class.getName()).log(Level.SEVERE, null, exception);
            return null;
        }
    }

    /**
     * Find the Field from the name field.
     *
     * @param string the name of field
     * @param clazz  the class
     * @return the field from the name
     */
    public Field getField(String string, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(string)) {
                return field;
            }
        }
        return null;
    }

    /**
     * returns the generic type of field.
     *
     * @param field the field
     * @return a generic type
     */
    public Class<?> getGenericType(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        return (Class<?>) genericType.getActualTypeArguments()[0];

    }

    /**
     * return the key and value of field.
     *
     * @param field the field
     * @return the types of the type
     */
    public KeyValueClass getGenericKeyValue(Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        KeyValueClass keyValueClass = new KeyValueClass();
        keyValueClass.keyClass = (Class<?>) genericType.getActualTypeArguments()[0];
        keyValueClass.valueClass = (Class<?>) genericType.getActualTypeArguments()[1];
        return keyValueClass;
    }

    /**
     * data struteded to store key and value class to map collection.
     *
     * @author otaviojava
     */
    public class KeyValueClass {
        private Class<?> keyClass;
        private Class<?> valueClass;

        public Class<?> getKeyClass() {
            return keyClass;
        }

        public Class<?> getValueClass() {
            return valueClass;
        }

    }

    /**
     * Make the given field accessible, explicitly setting it accessible
     * if necessary. The setAccessible(true) method is only
     * called when actually necessary, to avoid unnecessary
     * conflicts with a JVM SecurityManager (if active).
     *
     * @param field field the field to make accessible
     */
    public void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier
                .isPublic(field.getDeclaringClass().getModifiers()))
                && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public String getEntityName(Class classEntity) {
        return Optional.ofNullable((Entity) classEntity.getAnnotation(Entity.class))
                .map(Entity::value)
                .filter(StringUtils::isNotBlank)
                .orElse(classEntity.getSimpleName());
    }

}
