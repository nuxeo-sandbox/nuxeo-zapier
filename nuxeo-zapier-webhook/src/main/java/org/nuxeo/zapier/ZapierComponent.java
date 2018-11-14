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

import static org.nuxeo.zapier.Constants.AUTO_GRANT;
import static org.nuxeo.zapier.Constants.CLIENT_ID;
import static org.nuxeo.zapier.Constants.ENABLED;
import static org.nuxeo.zapier.Constants.NUXEO_ZAPIER;
import static org.nuxeo.zapier.Constants.NUXEO_ZAPIER_NAME;
import static org.nuxeo.zapier.Constants.OAUTH_2_CLIENTS_DIRECTORY;
import static org.nuxeo.zapier.Constants.REDIRECT_UR_IS;
import static org.nuxeo.zapier.Constants.ZAPIER_REDIRECT_URI_KEY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * @since 0.1
 */
public class ZapierComponent extends DefaultComponent {

    private static final Logger log = LoggerFactory.getLogger(ZapierComponent.class);

    @Override
    public void start(ComponentContext context) {
        log.info("Zapier component activation: oauth provider setup");
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session s = directoryService.open(OAUTH_2_CLIENTS_DIRECTORY)) {
                Map<String, Serializable> filter = new HashMap<>();
                filter.put(CLIENT_ID, NUXEO_ZAPIER);
                if (s.query(filter).isEmpty()) {
                    s.createEntry(
                            ImmutableMap.of(CLIENT_ID, NUXEO_ZAPIER, Constants.NAME, NUXEO_ZAPIER_NAME, REDIRECT_UR_IS,
                                    Framework.getProperty(ZAPIER_REDIRECT_URI_KEY), ENABLED, true, AUTO_GRANT, true));
                }
            }
        });
    }
}
