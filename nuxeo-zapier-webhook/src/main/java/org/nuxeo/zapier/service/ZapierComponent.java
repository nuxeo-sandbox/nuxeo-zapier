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
package org.nuxeo.zapier.service;

import static org.nuxeo.zapier.Constants.HOOK_CACHE_ID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.util.Strings;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.zapier.Constants;
import org.nuxeo.zapier.webhook.WebHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * @since 0.1
 */
public class ZapierComponent extends DefaultComponent implements ZapierService {

    private static final Logger log = LoggerFactory.getLogger(ZapierComponent.class);

    protected KeyValueService keyValueService;

    protected long ttl;

    @Override
    public void start(ComponentContext context) {
        log.info("Zapier component activation: oauth provider setup");
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session s = directoryService.open(Constants.OAUTH_2_CLIENTS_DIRECTORY)) {
                Map<String, Serializable> filter = new HashMap<>();
                filter.put(Constants.CLIENT_ID, Constants.NUXEO_ZAPIER);
                if (s.query(filter).isEmpty()) {
                    s.createEntry(ImmutableMap.of(Constants.CLIENT_ID, Constants.NUXEO_ZAPIER, Constants.NAME,
                            Constants.NUXEO_ZAPIER_NAME, Constants.REDIRECT_UR_IS,
                            Framework.getProperty(Constants.ZAPIER_REDIRECT_URI_KEY, "http://example"),
                            Constants.ENABLED, true, Constants.AUTO_GRANT, true));
                }
            }
        });
    }

    @Override
    public void subscribe(WebHook webhook, String username) {
        // Store the webhook
        List<WebHook> webhooks = fetch(HOOK_CACHE_ID, username);
        webhooks.add(webhook);
        store(HOOK_CACHE_ID, username, webhooks);
        NotificationService notificationService = Framework.getService(NotificationService.class);
        // subscribe the principal
        notificationService.subscribe(username, webhook.getResolverId(), webhook.getRequiredFields());
    }

    @Override
    public void unsubscribe(WebHook hook, String username) {

    }

    @Override
    public void unsubscribeAll(String username) {
        // Remove webhooks
        remove(HOOK_CACHE_ID, username);
        NotificationService notificationService = Framework.getService(NotificationService.class);
        // unsubscribe the principal
        notificationService.getResolvers().forEach((resolver) -> {
            notificationService.unsubscribe(username, resolver.getId(), new HashMap<>());
        });
    }

    @Override
    public void postNotification(Notification notification) {
        String username = notification.getUsername();
        Map<String, String> context = notification.getContext();
        String repositoryName = notification.getSourceRepository();
        TransactionHelper.runInTransaction(() -> {
            try (CloseableCoreSession systemSession = CoreInstance.openCoreSessionSystem(repositoryName)) {
                DocumentModel document = systemSession.getDocument(new IdRef(notification.getSourceId()));
                CoreInstance.doPrivileged(systemSession, s -> {
                    // Posting notification(s) to Zapier
                    ClientConfig config = new DefaultClientConfig();
                    Client client = Client.create(config);
                    List<Map<String, String>> jsonArray = new ArrayList<>();
                    Map<String, String> idJson = new HashMap<>();
                    idJson.put("id", username);
                    idJson.put("docUrl", getURL(document));
                    idJson.put("message", context.get("message"));
                    jsonArray.add(idJson);

                    // Simplify with https://zapier.com/help/webhooks/#triggering-multiple-webhooks-at-once
                    fetch(HOOK_CACHE_ID, username).stream()
                                                  .filter(hook -> hook.getResolverId()
                                                                      .equals(notification.getResolverId()))
                                                  .map(hook -> client.resource(hook.getTargetUrl()))
                                                  .forEach(res -> {
                                                      try {
                                                          res.type(MediaType.APPLICATION_JSON_TYPE).post(
                                                                  ClientResponse.class,
                                                                  Blobs.createJSONBlobFromValue(jsonArray).getString());
                                                      } catch (IOException e) {
                                                          throw new NuxeoException(e);
                                                      }
                                                  });
                });
            }
        });
    }

    @Override
    public void store(String cacheName, String entryId, List<WebHook> input) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutput entry = new ObjectOutputStream(bos)) {
            entry.writeObject(input);
            entry.flush();
            byte[] bytes = bos.toByteArray();
            store.put(entryId, bytes, ttl);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public List<WebHook> fetch(String cacheName, String entryId) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        byte[] entry = store.get(entryId);
        if (entry == null)
            return new ArrayList<>();
        ByteArrayInputStream bis = new ByteArrayInputStream(entry);
        return extractResult(bis);
    }

    @Override
    public void remove(String cacheName, String entryId) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        store.put(entryId, (String) null);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        keyValueService = Framework.getService(KeyValueService.class);
        ttl = Long.parseLong(Constants.NOTIFICATION_CACHE_DEFAULT_TTL);
        return super.getAdapter(adapter);
    }

    protected List<WebHook> extractResult(ByteArrayInputStream bis) {
        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            List<WebHook> result = (List<WebHook>) in.readObject();
            return result;
        } catch (IOException | ClassNotFoundException e) {
            throw new NuxeoException(e);
        }
    }

    protected String getURL(DocumentModel document) {
        DocumentViewCodecManager viewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        DocumentLocation docLoc = new DocumentLocationImpl(document);
        TypeInfo adapter = document.getAdapter(TypeInfo.class);
        if (adapter == null) {
            return Strings.EMPTY;
        }
        DocumentView docView = new DocumentViewImpl(docLoc, adapter.getDefaultView());
        return viewCodecManager.getUrlFromDocumentView(docView, true,
                NotificationServiceHelper.getNotificationService().getServerUrlPrefix());
    }
}
