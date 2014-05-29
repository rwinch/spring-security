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
public final class Session {
    private String id = UUID.randomUUID().toString();
    private boolean invalid;
    private Map<String,Object> sessionAttrs = new HashMap<String, Object>();
    private boolean old;
    private long lastAccessedTime = System.currentTimeMillis();
    private long creationTime = lastAccessedTime;

    Session() {}

    Session(Session session) {
        this.id = session.getId();
        this.invalid = session.invalid;
        this.sessionAttrs = new HashMap<String, Object>(session.sessionAttrs);
        this.old = session.old;
        this.lastAccessedTime = session.lastAccessedTime;
        this.creationTime = session.creationTime;
    }

    boolean isOld() {
        return this.old;
    }

    void setOld(boolean old) {
        this.old = old;
    }

    void updateLastAccessedTime() {
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

    }

    public int getMaxInactiveInterval() {
        return 0;
    }


    public Object getAttribute(String name) {
        return sessionAttrs.get(name);
    }

    public Object getValue(String name) {
        return getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(sessionAttrs.keySet());
    }

    public String[] getValueNames() {
        return sessionAttrs.keySet().toArray(new String[0]);
    }

    public void setAttribute(String name, Object value) {
        sessionAttrs.put(name, value);
    }

    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    public void removeAttribute(String name) {
        sessionAttrs.remove(name);
    }

    public void removeValue(String name) {
        removeAttribute(name);
    }

    public boolean equals(Object obj) {
        return id.equals(((Session)obj).getId());
    }

    public int hashCode() {
        return id.hashCode();
    }
}