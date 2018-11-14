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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.zapier.service.CacheService;

import com.sun.jersey.api.NotFoundException;

/**
 * @since 0.1
 */
@Path("/hook")
@WebObject(type = "hook")
@Produces(MediaType.APPLICATION_JSON)
public class HookObject extends ModuleRoot {

    private static final String HOOK_CACHE_ID = "zapier-hook-cache";

    private static final Log log = LogFactory.getLog(HookObject.class);

    @GET
    public List<Hook> doGetAll() {
        CacheService cacheService = Framework.getService(CacheService.class);
        // FIXME: how can we get all values without ids?
        return (List<Hook>) cacheService.getAll(HOOK_CACHE_ID, null);
    }

    @GET
    @Path("{hookId}")
    public Blob doGet(@PathParam("hookId") String hookId) throws IOException {
        CacheService cacheService = Framework.getService(CacheService.class);
        Hook hook = (Hook) cacheService.get(HOOK_CACHE_ID, hookId,
                Hook.class);
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", hook.getEvents().toString());
        idJson.put("event", hook.getEvents().toString());
        idJson.put("target_url", hook.getTargetUrl());
        return Blobs.createJSONBlobFromValue(idJson);
    }

    @GET
    @Path("/auditexample")
    public Blob doGet() throws IOException {
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
        CacheService cacheService = Framework.getService(CacheService.class);
        cacheService.invalidate(HOOK_CACHE_ID, hookId);
    }

    @PUT
    @Path("{hookId}")
    public void doPut(@PathParam("hookId") String hookId, Hook hook) {
        CacheService cacheService = Framework.getService(CacheService.class);
        cacheService.push(HOOK_CACHE_ID, hookId, hook);
    }

    @POST
    public Blob doPost(Hook hook) throws IOException {
        CacheService cacheService = Framework.getService(CacheService.class);
        cacheService.push(HOOK_CACHE_ID, ctx.getCoreSession().getPrincipal().getName(), hook);
        Map<String, String> idJson = new HashMap<>();
        idJson.put("id", hook.getEvents().toString());
        log.warn("targetUrl:" + hook.getTargetUrl());
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
