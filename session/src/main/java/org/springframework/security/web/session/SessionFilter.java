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
import java.util.*;

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
        SessionRequestWrapper wrappedRequest = new SessionRequestWrapper(request, response);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            HttpSessionWrapper wrappedSession = wrappedRequest.currentSession;
            if(wrappedSession != null) {
                sessionRepository.save(wrappedSession.session);
            }
        }
    }

    private class SessionRequestWrapper extends HttpServletRequestWrapper {
        private final HttpServletResponse response;
        private HttpSessionWrapper currentSession;

        private SessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.response = response;
        }

        @Override
        public HttpSession getSession(boolean create) {
            if(currentSession != null) {
                return currentSession;
            }
            String sessionId = getRequestedSessionId();
            if(sessionId != null) {
                Session session = sessionRepository.getSession(sessionId);
                if(session != null) {
                    // session.old
                    session.updateLastAccessedTime();
                    return new HttpSessionWrapper(session, getServletContext()) {
                        void doInvalidate() {
                            currentSession = null;
                            sessionRepository.delete(getId());
                            Cookie sessionCookie = new Cookie("SESSION","");
                            sessionCookie.setMaxAge(0);
                            response.addCookie(sessionCookie);
                        }
                    };
                }
            }
            if(!create) {
                return null;
            }
            Session session = new Session(sessionRepository);
            sessionId = session.getId();
            sessionRepository.save(session);
            Cookie cookie = new Cookie("SESSION", sessionId);
            response.addCookie(cookie);
            currentSession = new HttpSessionWrapper(session, getServletContext()) {
                void doInvalidate() {
                    currentSession = null;
                    sessionRepository.delete(getId());
                    Cookie sessionCookie = new Cookie("SESSION","");
                    sessionCookie.setMaxAge(0);
                    response.addCookie(sessionCookie);
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
