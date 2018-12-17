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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        WebHook webHook = zapierService.fetchWebHook(ctx.getPrincipal().getName(), hookId);
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", hookId);
        idJson.put("target_url", webHook.getTargetUrl());
        return Blobs.createJSONBlobFromValue(idJson);
    }

    @GET
    @Path("/example")
    public Blob getExample() throws IOException {
        List<Map<String, String>> jsonArray = new ArrayList<>();
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", "documentId");
        idJson.put("url", "documentURL");
        idJson.put("title", "documentTitle");
        idJson.put("originatingUser", "byWho");
        idJson.put("originatingEvent", "documentModified");
        idJson.put("docLifeCycle", "documentState");
        idJson.put("repositoryId", "repositoryId");
        jsonArray.add(idJson);
        return Blobs.createJSONBlobFromValue(jsonArray);
    }

    @DELETE
    @Path("{hookId}")
    public Response doDelete(@PathParam("hookId") String hookId) {
        // Unsubscription
        ZapierService zapierService = Framework.getService(ZapierService.class);
        zapierService.unsubscribe(ctx.getPrincipal().getName(), hookId);
        return Response.ok().build();
    }

    @POST
    public Blob doPost(WebHook webhook) throws IOException {
        // Compute the webHook id (last segment of Zapier URL)
        String[] segments = webhook.getTargetUrl().split("/");
        String webHookId = segments[segments.length - 1];
        webhook.setId(webHookId);

        // Subscription
        ZapierService zapierService = Framework.getService(ZapierService.class);
        zapierService.subscribe(webhook, ctx.getPrincipal().getName());

        // Return the webHook id to Zapier
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", webHookId);
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
