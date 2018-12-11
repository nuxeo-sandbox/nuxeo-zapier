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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.zapier.Constants.HOOK_CACHE_ID;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.zapier.service.ZapierService;
import org.nuxeo.zapier.webhook.WebHook;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, NotificationFeature.class })
@Deploy({ "org.nuxeo.zapier.webhook", "org.nuxeo.ecm.platform.oauth", "org.nuxeo.ecm.platform.web.common" })
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestZapier {

    @Inject
    protected CoreSession session;

    @Inject
    protected ZapierService zapierService;

    @Inject
    protected NotificationService notificationService;

    @Test
    public void iCanManageWebHooks() {
        // Subscribe a web hook
        WebHook hook = new WebHook();
        hook.setTargetUrl("url");
        hook.setResolverId("fileCreated");
        hook.setEntity("resolver");
        zapierService.subscribe(hook, "Administrator");

        // Assert subscription exists
        notificationService.getSubscriptions("fileCreated", new HashMap<>());
        List<WebHook> webHooks = zapierService.fetch(HOOK_CACHE_ID, session.getPrincipal().getName());

        // Unsubscribe a webhook
        zapierService.unsubscribe(hook, "Administrator");

        // Assert subscription is gone
        notificationService.getSubscriptions("fileCreated", new HashMap<>());
        webHooks = zapierService.fetch(HOOK_CACHE_ID, session.getPrincipal().getName());
    }

    @Test
    public void iCanBeNotified() {
        // Subscribe a web hook and prepare notification
        WebHook hook = new WebHook();
        hook.setTargetUrl("url");
        hook.setResolverId("fileCreated");
        hook.setEntity("resolver");
        zapierService.subscribe(hook, "Administrator");
        EventRecord eventRecord = EventRecord.builder()
                                             .withEventName("test")
                                             .withUsername("Administrator")
                                             .withDocument(session.getRootDocument())
                                             .build();
        Resolver resolver = mock(Resolver.class);
        when(resolver.getId()).thenReturn("fileCreated");
        Notification notif = Notification.builder()
                                         .fromEvent(eventRecord)
                                         .withUsername("bobby")
                                         .withResolver(resolver)
                                         .build();
        zapierService.postNotification(notif);

        // Check notification has been sent
    }
}
