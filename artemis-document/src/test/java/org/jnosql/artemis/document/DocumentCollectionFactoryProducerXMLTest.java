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

import org.jnosql.artemis.ConfigurationUnit;
import org.jnosql.artemis.CDIJUnitRunner;
import org.jnosql.diana.api.Settings;
import org.jnosql.diana.api.document.DocumentCollectionManagerAsyncFactory;
import org.jnosql.diana.api.document.DocumentCollectionManagerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(CDIJUnitRunner.class)
public class DocumentCollectionFactoryProducerXMLTest {

    @Inject
    @ConfigurationUnit(fileName = "document.xml", name = "name")
    private DocumentCollectionManagerFactory<?> factoryA;

    @Inject
    @ConfigurationUnit(fileName = "document.xml", name = "name-2")
    private DocumentCollectionManagerFactory factoryB;


    @Inject
    @ConfigurationUnit(fileName = "document.xml", name = "name")
    private DocumentCollectionManagerAsyncFactory<?> factoryAsyncA;

    @Inject
    @ConfigurationUnit(fileName = "document.xml", name = "name-2")
    private DocumentCollectionManagerAsyncFactory factoryAsyncB;


    @Test
    public void shouldReadInjectDocumentCollection() {
        factoryA.get("database");
        Assert.assertTrue(DocumentCollectionManagerMock.DocumentMock.class.isInstance(factoryA));
        DocumentCollectionManagerMock.DocumentMock mock = DocumentCollectionManagerMock.DocumentMock.class.cast(factoryA);
        Map<String, Object> settings = new HashMap<>();
        settings.put("key","value");
        settings.put("key2","value2");
        assertEquals(Settings.of(settings), mock.getSettings());
    }

    @Test
    public void shouldReadInjectDocumentCollectionB() {
        factoryB.get("database");
        Assert.assertTrue(DocumentCollectionManagerMock.DocumentMock.class.isInstance(factoryB));
        DocumentCollectionManagerMock.DocumentMock mock = DocumentCollectionManagerMock.DocumentMock.class.cast(factoryB);
        Map<String, Object> settings = new HashMap<>();
        settings.put("key","value");
        settings.put("key2","value2");
        settings.put("key3","value3");
        assertEquals(Settings.of(settings), mock.getSettings());
    }

    @Test
    public void shouldReadInjectDocumentCollectionAsync() {
        factoryAsyncA.getAsync("database");
        Assert.assertTrue(DocumentCollectionManagerMock.DocumentMock.class.isInstance(factoryAsyncA));
        DocumentCollectionManagerMock.DocumentMock mock = DocumentCollectionManagerMock.DocumentMock.class.cast(factoryAsyncA);
        Map<String, Object> settings = new HashMap<>();
        settings.put("key","value");
        settings.put("key2","value2");
        assertEquals(Settings.of(settings), mock.getSettings());
    }

    @Test
    public void shouldReadInjectDocumentCollectionAsyncB() {
        factoryAsyncB.getAsync("database");
        Assert.assertTrue(DocumentCollectionManagerMock.DocumentMock.class.isInstance(factoryAsyncB));
        DocumentCollectionManagerMock.DocumentMock mock = DocumentCollectionManagerMock.DocumentMock.class.cast(factoryAsyncB);
        Map<String, Object> settings = new HashMap<>();
        settings.put("key","value");
        settings.put("key2","value2");
        settings.put("key3","value3");
        assertEquals(Settings.of(settings), mock.getSettings());
    }
}