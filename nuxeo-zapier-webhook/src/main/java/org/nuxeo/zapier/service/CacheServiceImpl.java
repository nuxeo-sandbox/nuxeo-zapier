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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.DefaultComponent;

import org.nuxeo.zapier.Constants;

/**
 * @since 0.1
 */
public class CacheServiceImpl<T> extends DefaultComponent implements CacheService<T> {

    protected KeyValueService keyValueService;

    protected long ttl;

    /**
     * By default the cache invalidation timeout is set to 1 day.
     */
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        keyValueService = Framework.getService(KeyValueService.class);
        ttl = Long.parseLong(Constants.NOTIFICATION_CACHE_DEFAULT_TTL);
        return super.getAdapter(adapter);
    }

    @Override
    public void push(String cacheName, String entryId, T input) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutput entry = new ObjectOutputStream(bos)) {
            entry.writeObject(input);
            entry.flush();
            byte[] bytes = bos.toByteArray();
            store.put(entryId, bytes, ttl);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public T get(String cacheName, String entryId, Class<T> clazz) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        byte[] entry = store.get(entryId);
        if (entry == null) {
            try {
                return clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new NuxeoException(e);
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(entry);
        return extractResult(bis);
    }

    protected T extractResult(ByteArrayInputStream bis) {
        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            T result = (T) in.readObject();
            return result;
        } catch (IOException | ClassNotFoundException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void invalidate(String cacheName, String entryId) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        store.put(entryId, (String) null);
    }

    @Override
    public Map<String, T> getAll(String cacheName, List<String> keys) {
        KeyValueStore store = keyValueService.getKeyValueStore(cacheName);
        Map<String, byte[]> entries = store.get(keys);
        if (entries == null)
            return new HashMap<>();
        return entries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(e.getValue());
            return extractResult(bis);
        }));
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
