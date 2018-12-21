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
package org.nuxeo.zapier.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.zapier.service.ZapierService;

/**
 * @since 0.1
 */
@Operation(id = ZapierKVSCleanup.ID, category = Constants.CAT_FETCH, label = "Zapier KVS Cleanup")
public class ZapierKVSCleanup {

    public static final String ID = "Zapier.KVSCleanup";

    @Context
    private ZapierService zapierService;

    @Param(name = "username")
    private String username;

    @Param(name = "entryId", required = false)
    private String entryId;

    @OperationMethod
    public void run() {
        if (entryId != null) {
            zapierService.removeWebHook(username, entryId);
        } else {
            zapierService.invalidateKVS(username);
        }
    }

}
