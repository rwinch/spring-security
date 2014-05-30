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
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * @author Rob Winch
 */
public class SessionFilter extends OncePerRequestFilter {
    private final SessionRepository sessionRepository;

    public SessionFilter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SessionRequestWrapper wrappedRequest = new SessionRequestWrapper(sessionRepository, request);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            HttpSessionWrapper wrappedSession = wrappedRequest.currentSession;
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

    private static final class SessionRequestWrapper extends HttpServletRequestWrapper {
        private final SessionRepository sessionRepository;
        private HttpSessionWrapper currentSession;
        private boolean requestedValidSession;

        private SessionRequestWrapper(SessionRepository sessionRepository, HttpServletRequest request) {
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
                MapSession session = sessionRepository.getSession(requestedSessionId);
                if(session != null) {
                    this.requestedValidSession = true;
                    session.updateLastAccessedTime();
                    currentSession = new HttpSessionWrapper(session, getServletContext()) {
                        void doInvalidate() {
                            currentSession = null;
                            sessionRepository.delete(getId());
                        }
                    };
                    currentSession.setNew(false);
                    return currentSession;
                }
            }
            if(!create) {
                return null;
            }
            Session session = new MapSession();
            currentSession = new HttpSessionWrapper(session, getServletContext()) {
                void doInvalidate() {
                    currentSession = null;
                    sessionRepository.delete(getId());
                }
            };
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
    }

}
