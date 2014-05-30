/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.security.web.session;

import java.util.*;

/**
* @author Rob Winch
*/
public final class MapSession {
    private String id = UUID.randomUUID().toString();
    private boolean invalid;
    private Map<String,Object> sessionAttrs = new HashMap<String, Object>();
    private long creationTime = System.currentTimeMillis();
    private long lastAccessedTime = creationTime;
    private int maxInactiveInterval = 1800;

    MapSession() {}

    MapSession(MapSession session) {
        this.id = session.getId();
        this.invalid = session.invalid;
        this.sessionAttrs = new HashMap<String, Object>(session.sessionAttrs);
        this.lastAccessedTime = session.lastAccessedTime;
        this.creationTime = session.creationTime;
        this.maxInactiveInterval = session.maxInactiveInterval;
    }

    public void updateLastAccessedTime() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getId() {
        return id;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public Object getAttribute(String name) {
        return sessionAttrs.get(name);
    }

    public Set<String> getAttributeNames() {
        return sessionAttrs.keySet();
    }

    public void setAttribute(String name, Object value) {
        sessionAttrs.put(name, value);
    }

    public void removeAttribute(String name) {
        sessionAttrs.remove(name);
    }

    public boolean equals(Object obj) {
        return id.equals(((MapSession) obj).getId());
    }

    public int hashCode() {
        return id.hashCode();
    }
}