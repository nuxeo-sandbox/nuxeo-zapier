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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_TYPE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;

public class DocumentCreatedResolver extends SubscribableResolver {
    @Override
    public List<String> getRequiredContextFields() {
        return Collections.singletonList(SOURCE_DOC_TYPE);
    }

    @Override
    public boolean accept(EventRecord eventRecord) {
        return DOCUMENT_CREATED.equals(eventRecord.getEventName());
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.singletonMap(SOURCE_DOC_TYPE, eventRecord.getDocumentSourceType());
    }

    @Override
    public void subscribe(String username, Map<String, String> ctx) {
        perDocumentType(ctx, newCtx -> super.subscribe(username, newCtx));
    }

    @Override
    public void unsubscribe(String username, Map<String, String> ctx) {
        perDocumentType(ctx, newCtx -> super.unsubscribe(username, newCtx));
    }

    private void perDocumentType(Map<String, String> ctx, Consumer<Map<String, String>> cons) {
        Arrays.stream(ctx.getOrDefault(SOURCE_DOC_TYPE, "").split(",")).map(String::trim).forEach(s -> {
            Map<String, String> newCtx = new HashMap<>(ctx);
            newCtx.put(SOURCE_DOC_TYPE, s);
            cons.accept(newCtx);
        });
    }
}
