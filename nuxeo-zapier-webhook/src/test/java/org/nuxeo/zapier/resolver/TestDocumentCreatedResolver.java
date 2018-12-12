/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */

package org.nuxeo.zapier.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.zapier.resolver.DocumentCreatedResolver.DOCUMENT_TYPE;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.NotificationStreamCallback;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.runtime.stream.StreamHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.google.common.collect.ImmutableMap;

@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.zapier.webhook")
@Deploy("org.nuxeo.zapier.webhook:OSGI-INF/default-contrib.xml")
public class TestDocumentCreatedResolver {
    @Inject
    protected NotificationService ns;

    @Inject
    protected NotificationStreamCallback nsc;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testDocumentCreatedResolver() {
        assertThat(CounterNotifier.processed).isEqualTo(0);

        DocumentModel doc = session.createDocumentModel("/", "foo", "File");
        session.createDocument(doc);

        waitAllAsync();
        assertThat(CounterNotifier.processed).isEqualTo(0);

        nsc.doSubscribe("dummy", "documentCreated", ImmutableMap.of(DOCUMENT_TYPE, "File"));

        doc = session.createDocumentModel("/", "another", "File");
        session.createDocument(doc);

        waitAllAsync();
        assertThat(CounterNotifier.processed).isEqualTo(1);
    }

    @Test
    public void testMultipleTypeRegistration() {
        assertThat(CounterNotifier.processed).isEqualTo(0);

        DocumentModel doc = session.createDocumentModel("/", "foo", "File");
        session.createDocument(doc);

        waitAllAsync();
        assertThat(CounterNotifier.processed).isEqualTo(0);

        nsc.doSubscribe("dummy", "documentCreated", ImmutableMap.of(DOCUMENT_TYPE, "File,Folder"));

        doc = session.createDocumentModel("/", "another", "File");
        session.createDocument(doc);
        waitAllAsync();

        doc = session.createDocumentModel("/", "aFolder", "Folder");
        session.createDocument(doc);
        waitAllAsync();

        assertThat(CounterNotifier.processed).isEqualTo(2);
    }

    protected void waitAllAsync() {
        session.save();

        txFeature.nextTransaction();
        assertThat(StreamHelper.drainAndStop()).isTrue();
    }
}
