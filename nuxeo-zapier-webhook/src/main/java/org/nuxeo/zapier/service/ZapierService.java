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
package org.nuxeo.zapier.service;

import java.util.List;

import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.zapier.webhook.WebHook;

/**
 * @since 0.1
 */
public interface ZapierService {

    void subscribe(WebHook hook, String username);

    void unsubscribe(WebHook hook, String username);

    void unsubscribeAll(String username);

    void postNotification(Notification notification);

    void store(String cacheName, String entryId, List<WebHook> input);

    List<WebHook> fetch(String cacheName, String entryId);

    void remove(String cacheName, String entryId);
}
