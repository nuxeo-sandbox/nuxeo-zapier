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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * @since 0.1
 */
public class ZapierComponent extends DefaultComponent implements ZapierService {

    private static final Logger log = LoggerFactory.getLogger(ZapierComponent.class);

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
        Resolver resolver = notificationService.getResolver(webhook.getResolverId());
        if (resolver instanceof SubscribableResolver)
            notificationService.subscribe(username, webhook.getResolverId(), webhook.getRequiredFields());
    }

    @Override
    public void unsubscribe(String username, String hookId) {
        Optional<WebHook> webHook = removeWebHook(username, hookId);
        webHook.ifPresent(hook -> {
            NotificationService notificationService = Framework.getService(NotificationService.class);
            Resolver resolver = notificationService.getResolver(hook.getResolverId());
            if (resolver instanceof SubscribableResolver)
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
                    // No notification for proxies/versions
                    if (document.isProxy() || document.isVersion())
                        return;
                    // Posting notification(s) to Zapier
                    ClientConfig config = new DefaultClientConfig();
                    Client client = Client.create(config);
                    List<Map<String, String>> jsonArray = new ArrayList<>();
                    Map<String, String> idJson = new HashMap<>();
                    idJson.put("id", notification.getSourceId());
                    idJson.put("url", getURL(notification));
                    idJson.put("title", document.getTitle());
                    idJson.put("state", document.getCurrentLifeCycleState());
                    idJson.put("originatingEvent", context.get("originatingEvent"));
                    idJson.put("originatingUser", getUserName(context.get("originatingUser")));
                    idJson.put("repositoryId", notification.getSourceRepository());
                    addBlobURL(idJson, document);
                    jsonArray.add(idJson);

                    // Compute url with all existing webhooks for the given resolver
                    List<WebHook> webHooks = fetchWebHooks(username).stream()
                                                                    .filter(webHook -> webHook.getResolverId().equals(
                                                                            notification.getResolverId()))
                                                                    .collect(Collectors.toList());
                    triggerSingleWebHooks(notification, username, client, jsonArray, webHooks);
                    // TODO: List<String> ids = webHooks.stream().map((WebHook::getId)).collect(Collectors.toList());
                    // TODO: triggerMultipleWebHooks(notification, username, client, jsonArray, webHooks, ids);
                });
            }

        });
    }

    // TODO: NXP-26575
    // TODO: be careful with the replacement of 'standard' by 'catch' in the url
    // TODO: https://zapier.com/help/webhooks/#triggering-multiple-webhooks-at-once
    protected void triggerMultipleWebHooks(Notification notification, String username, Client client,
            List<Map<String, String>> jsonArray, List<WebHook> webHooks, List<String> ids) {
        String lastSegment = StringUtils.join(ids, ",");
        String example = webHooks.get(0).getTargetUrl();
        if (example.endsWith("/"))
            example = example.substring(0, example.length() - 2);
        String url = example.substring(0, example.lastIndexOf("/"));
        String finalUrl = String.format("%s/%s", url, lastSegment);
        WebResource resource = client.resource(finalUrl);
        try {
            ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class,
                    Blobs.createJSONBlobFromValue(jsonArray).getString());
            if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                throw new NuxeoException(String.format(
                        "Zapier errors with status %d. Check your Zapier account.\n Request URL: %s\n Resolver Id: %s\n Username: %s",
                        response.getStatus(), finalUrl, notification.getResolverId(), username));
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected void triggerSingleWebHooks(Notification notification, String username, Client client,
            List<Map<String, String>> jsonArray, List<WebHook> webHooks) {
        webHooks.forEach(webHook -> {
            WebResource resource = client.resource(webHook.getTargetUrl());
            try {
                ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class,
                        Blobs.createJSONBlobFromValue(jsonArray).getString());
                if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                    throw new NuxeoException(String.format(
                            "Zapier errors with status %d. Check your Zapier account.\n Request URL: %s\n Resolver Id: %s\n Username: %s",
                            response.getStatus(), webHook.getTargetUrl(), notification.getResolverId(), username));
                } else if (response.getStatus() == ClientResponse.Status.GONE.getStatusCode()) {
                    // Clean webhook removed on Zapier
                    removeWebHook(username, webHook.getId());
                }
            } catch (ClientHandlerException | IOException e) {
                throw new NuxeoException(e);
            }
        });

    }

    protected void addBlobURL(Map<String, String> idJson, DocumentModel document) {
        if (document.hasSchema("file") && document.getPropertyValue("file:content") != null) {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            String url = downloadService.getDownloadUrl(document, "file:content", null);
            idJson.put("binary", Framework.getProperty("nuxeo.url", "http://localhost:8080/nuxeo") + "/" + url);
        }
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
        return webHook.orElse(null);
    }

    @Override
    public Optional<WebHook> removeWebHook(String entryId, String webHookId) {
        List<WebHook> webHooks = fetchWebHooks(entryId);
        Optional<WebHook> webHook = webHooks.stream().filter((hook) -> hook.getId().equals(webHookId)).findAny();
        webHook.ifPresent(webHooks::remove);
        storeWebHooks(entryId, webHooks);
        return webHook;
    }

    @Override
    public void invalidateKVS(String keyId) {
        getKVS().put(keyId, (byte[]) null);
    }

    protected KeyValueStore getKVS() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(HOOK_CACHE_ID);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
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
        DocumentView docView = new DocumentViewImpl(docLoc, "view_documents");
        return viewCodecManager.getUrlFromDocumentView(docView, true,
                Framework.getProperty("nuxeo.url", "http://localhost:8080/nuxeo"));
    }

    public static String getUserName(String username) {
        NuxeoPrincipal principal = Framework.getService(UserManager.class).getPrincipal(username);
        return getUserName(principal);
    }

    public static String getUserName(NuxeoPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return getUserName(principal.getFirstName(), principal.getLastName(), principal.getName());
    }

    public static String getUserName(String firstName, String lastName, String username) {
        String fullUsername = Stream.of(firstName, lastName).filter(StringUtils::isNotBlank).collect(joining(" "));
        return isNotBlank(fullUsername) ? fullUsername : username;
    }
}
