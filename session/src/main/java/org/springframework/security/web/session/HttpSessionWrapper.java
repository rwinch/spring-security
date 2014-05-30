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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * @author Rob Winch
 */
abstract class HttpSessionWrapper implements HttpSession {
    final Session session;
    private final ServletContext servletContext;
    private boolean invalidated;

    public HttpSessionWrapper(Session session, ServletContext servletContext) {
        this.session = session;
        this.servletContext = servletContext;
    }

    void updateLastAccessedTime() {
        checkState();
        session.updateLastAccessedTime();
    }

    @Override
    public long getCreationTime() {
        checkState();
        return session.getCreationTime();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        checkState();
        return session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        session.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return NOOP_SESSION_CONTEXT;
    }

    @Override
    public Object getAttribute(String name) {
        checkState();
        return session.getAttribute(name);
    }

    @Override
    public Object getValue(String name) {
        checkState();
        return session.getValue(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkState();
        return session.getAttributeNames();
    }

    @Override
    public String[] getValueNames() {
        checkState();
        return session.getValueNames();
    }

    @Override
    public void setAttribute(String name, Object value) {
        checkState();
        session.setAttribute(name, value);
    }

    @Override
    public void putValue(String name, Object value) {
        checkState();
        session.putValue(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        checkState();
        session.removeAttribute(name);
    }

    @Override
    public void removeValue(String name) {
        checkState();
        session.removeValue(name);
    }

    @Override
    public final void invalidate() {
        checkState();
        this.invalidated = true;
        doInvalidate();
    }

    abstract void doInvalidate();

    @Override
    public boolean isNew() {
        checkState();
        return false;
    }

    private void checkState() {
        if(invalidated) {
            throw new IllegalStateException("The HttpSession has already be invalidated.");
        }
    }

    private static final HttpSessionContext NOOP_SESSION_CONTEXT = new HttpSessionContext() {
        @Override
        public HttpSession getSession(String sessionId) {
            return null;
        }

        @Override
        public Enumeration<String> getIds() {
            return EMPTY_ENUMERATION;
        }
    };

    private final static Enumeration<String> EMPTY_ENUMERATION = new Enumeration<String>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public String nextElement() {
            throw new NoSuchElementException("a");
        }
    };
}
