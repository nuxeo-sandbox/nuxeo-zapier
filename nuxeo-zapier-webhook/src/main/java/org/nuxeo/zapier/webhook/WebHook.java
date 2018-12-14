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

import java.io.Serializable;
import java.util.Map;

/**
 * @since 0.1
 */
public class WebHook implements Serializable {

    protected String id;

    protected String targetUrl;

    protected String resolverId;

    protected Map<String, String> requiredFields;

    public WebHook() {
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String zapId) {
        this.id = zapId;
    }

    public Map<String, String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(Map<String, String> requiredFields) {
        this.requiredFields = requiredFields;
    }

    public String getResolverId() {
        return resolverId;
    }

    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

}
