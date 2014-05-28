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

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

/**
 * @author Rob Winch
 */
public class SessionFilter extends OncePerRequestFilter {
    private Map<String,Session> sessions = new HashMap<String,Session>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new SessionRequestWrapper(request, response), response);
    }

    private class SessionRequestWrapper extends HttpServletRequestWrapper {
        private final HttpServletResponse response;
        private String sessionId;

        private SessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.response = response;
        }

        @Override
        public HttpSession getSession(boolean create) {
            String sessionId = getSessionId();
            if(sessionId != null) {
                Session session = sessions.get(sessionId);
                // session.old
                session.lastAccessedTime = System.currentTimeMillis();
                return session;
            }
            if(!create) {
                return null;
            }
            Session session = new Session(response);
            sessionId = session.getId();
            sessions.put(sessionId, session);
            Cookie cookie = new Cookie("SESSION", sessionId);
            response.addCookie(cookie);
            return session;
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        private String getSessionId() {
            if(sessionId == null) {
                sessionId = getRequestedSessionId();
            }
            return sessionId;
        }

        @Override
        public String getRequestedSessionId() {
            Cookie session = WebUtils.getCookie(this, "SESSION");
            return session == null ? null : session.getValue();
        }
    }

    private class Session implements HttpSession {
        private String id = UUID.randomUUID().toString();
        private boolean invalid;
        private Map<String,Object> sessionAttrs = new HashMap<String, Object>();
        private boolean old;
        private long lastAccessedTime = System.currentTimeMillis();
        private long creationTime = lastAccessedTime;
        private final HttpServletResponse response;

        private Session(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public long getCreationTime() {
            return creationTime;
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
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public void setMaxInactiveInterval(int interval) {

        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        @Override
        public HttpSessionContext getSessionContext() {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return sessionAttrs.get(name);
        }

        @Override
        public Object getValue(String name) {
            return getAttribute(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.enumeration(sessionAttrs.keySet());
        }

        @Override
        public String[] getValueNames() {
            return sessionAttrs.keySet().toArray(new String[0]);
        }

        @Override
        public void setAttribute(String name, Object value) {
            sessionAttrs.put(name, value);
        }

        @Override
        public void putValue(String name, Object value) {
            setAttribute(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            sessionAttrs.remove(name);
        }

        @Override
        public void removeValue(String name) {
            removeAttribute(name);
        }

        @Override
        public void invalidate() {
            checkState();
            sessions.remove(getId());
            Cookie sessionCookie = new Cookie("SESSION","");
            sessionCookie.setMaxAge(0);
            response.addCookie(sessionCookie);
            this.invalid = true;
        }

        @Override
        public boolean isNew() {
            return !old;
        }

        private void checkState() {
            if(invalid) {
                throw new IllegalStateException("The session is invalided");
            }
        }

        @Override
        public boolean equals(Object obj) {
            return id.equals(((Session)obj).getId());
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
