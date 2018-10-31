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
        return hook;
    }

}
