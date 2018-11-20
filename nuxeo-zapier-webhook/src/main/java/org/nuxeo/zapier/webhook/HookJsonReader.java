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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The reader for the Zapier hook. Example:
 * {"url":"https://hooks.zapier.com/hooks/standard/3397485/cdd3f24afecc42d88199175f8d82f1bf/","event":["documentCreated","documentModified"]}
 *
 * @since 0.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class HookJsonReader extends AbstractJsonReader<Hook> {

    @Override
    public Hook read(JsonNode jsonNode) {
        Hook hook = new Hook();
        hook.setTargetUrl(jsonNode.get("url").asText());
        hook.setEvents(getStringListField(jsonNode,"events"));
        hook.setDocTypes(getStringListField(jsonNode,"docTypes"));
        return hook;
    }

}
