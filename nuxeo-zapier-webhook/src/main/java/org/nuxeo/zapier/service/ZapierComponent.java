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

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;
import static org.nuxeo.zapier.Constants.HOOK_CACHE_ID;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
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
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.zapier.Constants;
import org.nuxeo.zapier.webhook.WebHook;
import org.nuxeo.zapier.webhook.WebHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
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
        storeWebHook(username, webhook);
        // subscribe the principal
        NotificationService notificationService = Framework.getService(NotificationService.class);
        notificationService.subscribe(username, webhook.getResolverId(), webhook.getRequiredFields());
    }

    @Override
    public void unsubscribe(String username, String hookId) {
        Optional<WebHook> webHook = removeWebHook(username, hookId);
        webHook.ifPresent(hook -> {
            NotificationService notificationService = Framework.getService(NotificationService.class);
            notificationService.unsubscribe(username, hook.getResolverId(), hook.getRequiredFields());
        });
    }

    @Override
    public void postNotification(Notification notification) {
        String username = notification.getUsername();
        Map<String, String> context = notification.getContext();
        TransactionHelper.runInTransaction(() -> {
            try (CloseableCoreSession systemSession = CoreInstance.openCoreSessionSystem(
                    notification.getSourceRepository())) {
                DocumentModel document = systemSession.getDocument(new IdRef(notification.getSourceId()));
                CoreInstance.doPrivileged(systemSession, s -> {
                    // Posting notification(s) to Zapier
                    ClientConfig config = new DefaultClientConfig();
                    Client client = Client.create(config);
                    List<Map<String, String>> jsonArray = new ArrayList<>();
                    Map<String, String> idJson = new HashMap<>();
                    idJson.put("id", notification.getSourceId());
                    idJson.put("url", getURL(notification));
                    idJson.put("originatingEvent", context.get("originatingEvent"));
                    idJson.put("originatingUser", context.get("originatingUser"));
                    idJson.put("repositoryId", notification.getSourceRepository());
                    // TODO doctype, docstate, path?
                    jsonArray.add(idJson);

                    // Compute url with all existing webhooks for the given resolver
                    List<WebHook> webHooks = fetchWebHooks(username).stream()
                                                                    .filter(webHook -> webHook.getResolverId().equals(
                                                                            notification.getResolverId()))
                                                                    .collect(Collectors.toList());
                    List<String> ids = webHooks.stream().map((WebHook::getId)).collect(Collectors.toList());
                    String lastSegment = StringUtils.join(ids, ",");
                    String example = webHooks.get(0).getTargetUrl();
                    if (example.endsWith("/"))
                        example = example.substring(0, example.length() - 2);
                    String url = example.substring(0, example.lastIndexOf("/"));
                    WebResource resource = client.resource(String.format("%s/%s", url, lastSegment));
                    try {
                        ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).post(
                                ClientResponse.class, Blobs.createJSONBlobFromValue(jsonArray).getString());
                        if (response.getStatus() != ClientResponse.Status.ACCEPTED.getStatusCode()) {
                            throw new NuxeoException("Zapier errors with status %s. Check your Zapier account.",
                                    response.getStatus());
                        }
                    } catch (IOException e) {
                        throw new NuxeoException(e);
                    }
                });
            }

        });
    }

    @Override
    public void storeWebHooks(String entryId, List<WebHook> webHookList) {
        WebHooks webHooks = new WebHooks();
        webHooks.setWebHookList(webHookList);
        getKVS().put(entryId, getAvroCodec().encode(webHooks), ttl);
    }

    @Override
    public void storeWebHook(String entryId, WebHook webHook) {
        List<WebHook> webHooksList = fetchWebHooks(entryId);
        webHooksList.add(webHook);
        WebHooks webHooks = new WebHooks();
        webHooks.setWebHookList(webHooksList);
        getKVS().put(entryId, getAvroCodec().encode(webHooks), ttl);
    }

    @Override
    public List<WebHook> fetchWebHooks(String entryId) {
        byte[] entry = getKVS().get(entryId);
        if (entry == null)
            return new ArrayList<>();
        return getAvroCodec().decode(entry).getWebHookList();
    }

    @Override
    public WebHook fetchWebHook(String entryId, String zapId) {
        List<WebHook> webHooks = fetchWebHooks(entryId);
        Optional<WebHook> webHook = webHooks.stream().filter((hook) -> hook.getId().equals(zapId)).findAny();
        return webHook.get();
    }

    @Override
    public Optional<WebHook> removeWebHook(String entryId, String webHookId) {
        List<WebHook> webHooks = fetchWebHooks(entryId);
        Optional<WebHook> webHook = webHooks.stream().filter((hook) -> hook.getId().equals(webHookId)).findAny();
        webHook.ifPresent(webHooks::remove);
        storeWebHooks(entryId, webHooks);
        return webHook;
    }

    protected KeyValueStore getKVS() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(HOOK_CACHE_ID);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        keyValueService = Framework.getService(KeyValueService.class);
        ttl = Long.parseLong(Constants.NOTIFICATION_CACHE_DEFAULT_TTL);
        return super.getAdapter(adapter);
    }

    protected Codec<WebHooks> getAvroCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, WebHooks.class);
    }

    protected String getURL(Notification notification) {
        DocumentViewCodecManager viewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        DocumentLocation docLoc = new DocumentLocationImpl(notification.getSourceRepository(),
                notification.getSourceRef());
        DocumentView docView = new DocumentViewImpl(docLoc);
        return viewCodecManager.getUrlFromDocumentView(docView, true,
                NotificationServiceHelper.getNotificationService().getServerUrlPrefix());
    }
}
