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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.zapier.service.ZapierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * @since 0.1
 */
public class ZapierComponent extends DefaultComponent implements ZapierService {

    private static final Logger log = LoggerFactory.getLogger(ZapierComponent.class);

    private List<EventBundle> eventBundles = new ArrayList<>();

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
                            Framework.getProperty(Constants.ZAPIER_REDIRECT_URI_KEY), Constants.ENABLED, true,
                            Constants.AUTO_GRANT, true));
                }
            }
        });
    }

    @Override
    public List<EventBundle> getEventBundles() {
        return eventBundles;
    }

    @Override
    public void setEventBundles(List<EventBundle> eventBundles) {
        this.eventBundles = eventBundles;
    }

    @Override
    public void addEventBundle(EventBundle eventBundle) {
        this.eventBundles.add(eventBundle);
    }
}
