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
package org.nuxeo.zapier.webhook;

import static org.nuxeo.zapier.Constants.HOOK_CACHE_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.zapier.service.ZapierService;

import com.sun.jersey.api.NotFoundException;

/**
 * @since 0.1
 */
@Path("/hook")
@WebObject(type = "hook")
@Produces(MediaType.APPLICATION_JSON)
public class WebHookObject extends ModuleRoot {

    @GET
    @Path("{hookId}")
    public Blob doGet(@PathParam("hookId") String hookId) throws IOException {
        ZapierService zapierService = Framework.getService(ZapierService.class);
        List<WebHook> webhooks = zapierService.fetch(HOOK_CACHE_ID, ctx.getPrincipal().getName());
        Optional<WebHook> webHook = webhooks.stream()
                                            .filter((webhook) -> webhook.getZapId().equals(hookId))
                                            .findFirst();
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", hookId);
        idJson.put("target_url", webHook.get().getTargetUrl());
        return Blobs.createJSONBlobFromValue(idJson);
    }

    @GET
    @Path("/example")
    public Blob getExample() throws IOException {
        List<Map<String, String>> jsonArray = new ArrayList<>();
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", "202");
        idJson.put("category", "eventDocumentCategory");
        idJson.put("principalName", "someone");
        idJson.put("comment", "something");
        idJson.put("eventId", "documentModified");
        idJson.put("docLifeCycle", "a state");
        idJson.put("docPath", "/somewhere");
        idJson.put("docType", "type");
        idJson.put("docUUID", "32233fa-23323-fee032");
        idJson.put("repositoryId", "default");
        idJson.put("eventDate", "2018-09-06T00:00:00.524Z");
        idJson.put("extended", "");
        jsonArray.add(idJson);
        return Blobs.createJSONBlobFromValue(jsonArray);
    }

    @DELETE
    @Path("{hookId}")
    public void doDelete(@PathParam("hookId") String hookId) {
        ZapierService zapierService = Framework.getService(ZapierService.class);
        String username = ctx.getPrincipal().getName();
        List<WebHook> webHooks = zapierService.fetch(HOOK_CACHE_ID, username);
        zapierService.store(HOOK_CACHE_ID, username,
                webHooks.stream().filter(hook -> !hook.getZapId().equals(hookId)).collect(Collectors.toList()));
    }

    @POST
    public Blob doPost(WebHook webhook) throws IOException {
        Map<String, String> idJson = new HashMap<>();
        String url = webhook.getTargetUrl();
        // Set the webhook Id (zap Id) - return to Zapier 'username-zapId'
        String[] segments = url.split("/");
        String zapId = segments[segments.length - 1];
        String webHookId = String.format("%s-%s", ctx.getPrincipal().getName(), zapId);
        idJson.put("id", webHookId);
        webhook.setZapId(zapId);
        ZapierService zapierService = Framework.getService(ZapierService.class);
        String username = ctx.getUserSession().getPrincipal().getName();
        zapierService.subscribe(webhook, username);
        return Blobs.createJSONBlobFromValue(idJson);
    }

    @Override
    public Object handleError(Throwable t) {
        if (t instanceof NotFoundException) {
            return Response.status(404).build();
        } else {
            return super.handleError(t);
        }
    }

    @Override
    public Response redirect(String uri) {
        if (!uri.contains("://") && !uri.startsWith("/")) {
            return super.redirect(this.getPath() + "/" + uri);
        } else {
            return super.redirect(uri);
        }
    }
}
