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

import org.springframework.security.session.Session;
import org.springframework.security.session.SessionRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A
 *
 * @author Rob Winch
 */
public class SessionRepositoryFilter extends OncePerRequestFilter {
    private final SessionRepository<Session> sessionRepository;

    public SessionRepositoryFilter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SessionRepositoryRequestWrapper wrappedRequest = new SessionRepositoryRequestWrapper(sessionRepository, request);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            SessionRepositoryRequestWrapper.HttpSessionWrapper wrappedSession = wrappedRequest.currentSession;
            if(wrappedSession == null) {
                if(wrappedRequest.isInvalidateClientSession()) {
                    Cookie sessionCookie = new Cookie("SESSION","");
                    sessionCookie.setMaxAge(0);
                    sessionCookie.setHttpOnly(true);
                    sessionCookie.setSecure(request.isSecure());
                    response.addCookie(sessionCookie);
                }
            } else {
                Session session = wrappedSession.session;
                sessionRepository.save(session);
                Cookie cookie = new Cookie("SESSION", session.getId());
                cookie.setHttpOnly(true);
                cookie.setSecure(request.isSecure());
                response.addCookie(cookie);
            }
        }
    }

    /**
     * A {@link javax.servlet.http.HttpServletRequest} that retrieves the {@link javax.servlet.http.HttpSession} using a
     * {@link org.springframework.security.session.SessionRepository}.
     *
     * @author Rob Winch
     * @since 4.0
     */
    private static final class SessionRepositoryRequestWrapper extends HttpServletRequestWrapper {
        private final SessionRepository sessionRepository;
        private HttpSessionWrapper currentSession;
        private boolean requestedValidSession;

        private SessionRepositoryRequestWrapper(SessionRepository sessionRepository, HttpServletRequest request) {
            super(request);
            this.sessionRepository = sessionRepository;
        }

        private boolean isInvalidateClientSession() {
            return currentSession == null && requestedValidSession;
        }

        @Override
        public HttpSession getSession(boolean create) {
            if(currentSession != null) {
                return currentSession;
            }
            String requestedSessionId = getRequestedSessionId();
            if(requestedSessionId != null) {
                Session session = sessionRepository.getSession(requestedSessionId);
                if(session != null) {
                    this.requestedValidSession = true;
                    session.setLastAccessedTime(System.currentTimeMillis());
                    currentSession = new HttpSessionWrapper(session, getServletContext());
                    currentSession.setNew(false);
                    return currentSession;
                }
            }
            if(!create) {
                return null;
            }
            Session session = sessionRepository.createSession();
            currentSession = new HttpSessionWrapper(session, getServletContext());
            return currentSession;
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

        @Override
        public String getRequestedSessionId() {
            Cookie session = WebUtils.getCookie(this, "SESSION");
            return session == null ? null : session.getValue();
        }

        private final class HttpSessionWrapper implements HttpSession {
            final Session session;
            private final ServletContext servletContext;
            private boolean invalidated;
            private boolean old;

            public HttpSessionWrapper(Session session, ServletContext servletContext) {
                this.session = session;
                this.servletContext = servletContext;
            }

            void updateLastAccessedTime() {
                checkState();
                session.setLastAccessedTime(System.currentTimeMillis());
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
                return getAttribute(name);
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                checkState();
                return Collections.enumeration(session.getAttributeNames());
            }

            @Override
            public String[] getValueNames() {
                checkState();
                Set<String> attrs = session.getAttributeNames();
                return attrs.toArray(new String[0]);
            }

            @Override
            public void setAttribute(String name, Object value) {
                checkState();
                session.setAttribute(name, value);
            }

            @Override
            public void putValue(String name, Object value) {
                setAttribute(name, value);
            }

            @Override
            public void removeAttribute(String name) {
                checkState();
                session.removeAttribute(name);
            }

            @Override
            public void removeValue(String name) {
                removeAttribute(name);
            }

            @Override
            public final void invalidate() {
                checkState();
                this.invalidated = true;
                currentSession = null;
                sessionRepository.delete(getId());
            }

            public void setNew(boolean isNew) {
                this.old = !isNew;
            }

            @Override
            public boolean isNew() {
                checkState();
                return !old;
            }

            private void checkState() {
                if(invalidated) {
                    throw new IllegalStateException("The HttpSession has already be invalidated.");
                }
            }
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
