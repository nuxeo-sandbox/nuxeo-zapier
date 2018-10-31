
package org.nuxeo.zapier.service;

import java.util.List;
import java.util.Map;

/**
 * @since 0.1
 */
public interface CacheService<T> {

    void push(String cacheName, String entryId, T input);

    T get(String cacheName, String entryId, Class<T> clazz);

    void invalidate(String cacheName, String entryId);

    Map<String, T> getAll(String cacheName, List<String> keys);
}
