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
/**
 * This package has the classes to read configuration from file, json recommended, that is normally activated from
 * {@link org.jnosql.artemis.ConfigurationUnit} annotation.
 * <p>The JSON file has the structure below:</p>
 * <pre>{@code
 * [
 * {
 * "description":"that is the description",
 * "name":"name",
 * "provider":"class",
 * "settings":{
 * "key":"value"
 * }
 * },
 * {
 * "description":"that is the description",
 * "name":"name-2",
 * "provider":"class",
 * "settings":{
 * "key":"value"
 * }
 * }
 * ]
 *
 * }
 * </pre>
 * <p>
 * <p>The XML file structure:</p>
 * <pre>{@code
 *  <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 * <configurations>
 *      <configuration>
 *          <description>that is the description</description>
 *          <name>name</name>
 *          <provider>class</provider>
 *          <settings>
 *            <entry>
 *                  <key>key2</key>
 *                  <value>value2</value>
 *              </entry>
 *              <entry>
 *                  <key>key</key>
 *                  <value>value</value>
 *              </entry>
 *          </settings>
 *      </configuration>
 * </configurations>
 *
 *  }
 * <p>Where:</p>
 * <p><b>Name</b> the unit-name in the configuration, when there is more than one configuration unit (optional).</p>
 * <p><b>Description</b>: the description of configuration (optional).</p>
 * <p><b>Provider</b>: the class provider</p>
 * <p><b>settings</b>: The key-value settings to use on the setup class.</p>
 * <p>
 * <pre>
 *
 * &#064;Inject
 * &#064;ConfigurationUnit
 * private Configuration configuration;
 *
 * </pre>
 * <p>
 * When the structure just has one configuration unit the name is not required, however,
 * when there are two or more configurations the name in the configuration must match with the
 * {@link org.jnosql.artemis.ConfigurationUnit#name()} annotation.
 * When the structure just has one configuration unit the name is not required, however,
 * when there are two or more settings the name in the configuration must match with the annotation.
 * If there are more than two configurations in the file and the inject does not inform the name that will
 * throw an exception.
 * </p>
 * <pre>
 *
 * &#064;Inject
 * &#064;ConfigurationUnit(name = "name")
 * private Configuration configuration;
 *
 * </pre>
 * The default configuration structure is within either META-INF or WEB-INF folder.
 * The default JSON file is "jnosql.json", despite you can change using the {@link org.jnosql.artemis.ConfigurationUnit#fileName()}
 * <p>
 * <pre>
 *
 * &#064;Inject
 * &#064;ConfigurationUnit(fileName = "file.json")
 * private Configuration configuration;
 *
 * </pre>
 */
package org.jnosql.artemis.configuration;