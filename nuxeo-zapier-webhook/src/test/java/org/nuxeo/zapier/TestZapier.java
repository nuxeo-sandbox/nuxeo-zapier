/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.zapier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_TYPE;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.stream.StreamHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.zapier.service.ZapierService;
import org.nuxeo.zapier.webhook.WebHook;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, NotificationFeature.class })
@Deploy({ "org.nuxeo.zapier.webhook", "org.nuxeo.ecm.platform.oauth", "org.nuxeo.ecm.platform.web.common",
        "org.nuxeo.ecm.platform.url.api", "org.nuxeo.ecm.platform.url.core" })
@Deploy("org.nuxeo.zapier.webhook:OSGI-INF/default-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestZapier {

    private static final String ADMINISTRATOR = "Administrator";

    private static final String DOCUMENT_CREATED = "documentCreated";

    @Inject
    protected CoreSession session;

    @Inject
    protected ZapierService zapierService;

    @Inject
    protected NotificationService notificationService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void iCanManageWebHooks() {
        // Setup
        SetupWebHook setupWebHook = new SetupWebHook().invoke();
        WebHook hook = setupWebHook.getHook();
        Map<String, String> params = setupWebHook.getParams();

        // Subscribe a webhook
        zapierService.subscribe(hook, ADMINISTRATOR);
        // Assert subscription exists
        waitAllAsync();
        assertThat(notificationService.getSubscriptions(DOCUMENT_CREATED, params).getUsernames()).contains(
                ADMINISTRATOR);
        assertThat(zapierService.fetchWebHooks(ADMINISTRATOR)).isNotEmpty();

        // Unsubscribe a webhook
        zapierService.unsubscribe(ADMINISTRATOR, hook.getId());
        // Assert subscription is gone
        waitAllAsync();
        assertThat(notificationService.getSubscriptions(DOCUMENT_CREATED, params).getUsernames()).isEmpty();
        assertThat(zapierService.fetchWebHooks(ADMINISTRATOR)).isEmpty();
    }

    @Test
    public void iCanBeNotified() {
        // Setup
        SetupWebHook setupWebHook = new SetupWebHook().invoke();
        WebHook hook = setupWebHook.getHook();

        // Subscribe and post a notification
        zapierService.subscribe(hook, ADMINISTRATOR);
        EventRecord eventRecord = EventRecord.builder()
                                             .withEventName("test")
                                             .withUsername(ADMINISTRATOR)
                                             .withDocument(session.getRootDocument())
                                             .build();
        Resolver resolver = mock(Resolver.class);
        when(resolver.getId()).thenReturn(DOCUMENT_CREATED);
        when(resolver.getMessageKey()).thenReturn("test");
        Notification notif = Notification.builder()
                                         .fromEvent(eventRecord)
                                         .withUsername(ADMINISTRATOR)
                                         .withResolver(resolver, Locale.getDefault())
                                         .build();

        try {
            zapierService.postNotification(notif);
            fail("Should fail here - We just check that Zapier service has found a webhook to post to");
        } catch (NuxeoException e) {
            assertThat(e).isNotNull();
        }
    }

    private void waitAllAsync() {
        session.save();

        txFeature.nextTransaction();
        assertThat(StreamHelper.drainAndStop()).isTrue();
    }

    private class SetupWebHook {
        private WebHook hook;

        private Map<String, String> params;

        private WebHook getHook() {
            return hook;
        }

        private Map<String, String> getParams() {
            return params;
        }

        private SetupWebHook invoke() {
            hook = new WebHook();
            hook.setTargetUrl("http://localhost:8080/nuxeo");
            hook.setResolverId(DOCUMENT_CREATED);
            hook.setId("hookId");
            params = new HashMap<>();
            params.put(SOURCE_DOC_TYPE, "File");
            hook.setRequiredFields(params);
            return this;
        }
    }
}
