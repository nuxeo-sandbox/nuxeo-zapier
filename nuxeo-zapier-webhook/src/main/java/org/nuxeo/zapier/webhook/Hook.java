package org.nuxeo.zapier.webhook;

import java.io.Serializable;
import java.util.List;

/**
 * @since 0.1
 */
public class Hook implements Serializable {

    protected String targetUrl;

    protected List<String> events;

    protected String entity;

    public Hook() {

    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }
}
