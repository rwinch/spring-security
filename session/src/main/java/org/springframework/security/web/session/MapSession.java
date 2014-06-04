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
public final class MapSession implements Session {
    private String id = UUID.randomUUID().toString();
    private Map<String,Object> sessionAttrs = new HashMap<String, Object>();
    private long creationTime = System.currentTimeMillis();
    private long lastAccessedTime = creationTime;
    private int maxInactiveInterval = 15;

    public MapSession() {}

    public MapSession(Session session) {
        this.id = session.getId();
        this.sessionAttrs = new HashMap<String, Object>(session.getAttributeNames().size());
        for(String attrName : session.getAttributeNames()) {
            Object attrValue = session.getAttribute(attrName);
            this.sessionAttrs.put(attrName, attrValue);
        }
        this.lastAccessedTime = session.getLastAccessedTime();
        this.creationTime = session.getCreationTime();
        this.maxInactiveInterval = session.getMaxInactiveInterval();
    }

    @Override
    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
        return sessionAttrs.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return sessionAttrs.keySet();
    }

    @Override
    public void setAttribute(String name, Object value) {
        sessionAttrs.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        sessionAttrs.remove(name);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.sessionAttrs = new HashMap<String,Object>(attributes);
    }

    public boolean equals(Object obj) {
        return id.equals(((MapSession) obj).getId());
    }

    public int hashCode() {
        return id.hashCode();
    }
}