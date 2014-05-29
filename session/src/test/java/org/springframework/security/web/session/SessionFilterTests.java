package org.springframework.security.web.session;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SessionFilterTests {
    private final static String SESSION_ATTR_NAME = HttpSession.class.getName();

    private SessionRepository sessionRepository;

    private SessionFilter filter;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private MockFilterChain chain;

    @Before
    public void setup() throws Exception {
        sessionRepository = new InMemorySessionRepository();
        filter = new SessionFilter(sessionRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Test
    public void doFilterGetSessionNew() throws Exception {
        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession();
            }
        });

        assertNewSession();
    }

    @Test
    public void doFilterGetSessionTrueNew() throws Exception {
        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession(true);
            }
        });

        assertNewSession();
    }

    @Test
    public void doFilterGetSessionFalseNew() throws Exception {
        getWrappedRequest().getSession(false);

        assertNoSession();
    }

    // --- saving

    @Test
    public void doFilterGetAttr() throws Exception {
        final String ATTR_NAME = "attr";
        final String ATTR_VALUE = "value";
        final String ATTR_NAME2 = "attr2";
        final String ATTR_VALUE2 = "value2";

        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession().setAttribute(ATTR_NAME, ATTR_VALUE);
                wrappedRequest.getSession().setAttribute(ATTR_NAME2, ATTR_VALUE2);
            }
        });

        assertNewSession();

        setSessionCookie(getSessionCookie().getValue());
        response.reset();

        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                assertThat(wrappedRequest.getSession().getAttribute(ATTR_NAME)).isEqualTo(ATTR_VALUE);
                assertThat(wrappedRequest.getSession().getAttribute(ATTR_NAME2)).isEqualTo(ATTR_VALUE2);
            }
        });
    }


    // --- invalidate

    @Test
    public void doFilterInvalidateInvalidateIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.invalidate();
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateCreationTimeIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.getCreationTime();
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateAttributeIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.getAttribute("attr");
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateValueIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.getValue("attr");
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateAttributeNamesIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.getAttributeNames();
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateValueNamesIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.getValueNames();
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateSetAttributeIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.setAttribute("a","b");
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidatePutValueIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.putValue("a", "b");
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateRemoveAttributeIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.removeAttribute("name");
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateRemoveValueIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.removeValue("name");
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateNewIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.isNew();
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateLastAccessedTimeIllegalState() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();
        try {
            session.getLastAccessedTime();
            fail("Expected Exception");
        } catch(IllegalStateException success) {}
    }

    @Test
    public void doFilterInvalidateId() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();

        // no exception
        session.getId();
    }

    @Test
    public void doFilterInvalidateServletContext() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();

        // no exception
        session.getServletContext();
    }

    @Test
    public void doFilterInvalidateSessionContext() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();

        // no exception
        session.getSessionContext();
    }

    @Test
    public void doFilterInvalidateMaxInteractiveInterval() throws Exception {
        HttpSession session = getWrappedRequest().getSession();
        session.invalidate();

        // no exception
        session.getMaxInactiveInterval();
        session.setMaxInactiveInterval(3600);
    }

    @Test
    public void doFilterInvalidateAndGetSession() throws Exception {
        final String ATTR_NAME = "attr";
        final String ATTR_VALUE = "value";
        final String ATTR_NAME2 = "attr2";
        final String ATTR_VALUE2 = "value2";

        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession().setAttribute(ATTR_NAME, ATTR_VALUE);
                wrappedRequest.getSession().invalidate();
                wrappedRequest.getSession().setAttribute(ATTR_NAME2, ATTR_VALUE2);
            }
        });

        assertNewSession();

        setSessionCookie(getSessionCookie().getValue());
        response.reset();

        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                assertThat(wrappedRequest.getSession().getAttribute(ATTR_NAME)).isNull();
                assertThat(wrappedRequest.getSession().getAttribute(ATTR_NAME2)).isEqualTo(ATTR_VALUE2);
            }
        });
    }

    // --- invalid session ids

    @Test
    public void doFilterGetSessionInvalidSessionId() throws Exception {
        setSessionCookie("INVALID");
        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession();
            }
        });

        assertNewSession();
    }

    @Test
    public void doFilterGetSessionTrueInvalidSessionId() throws Exception {
        setSessionCookie("INVALID");
        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession(true);
            }
        });

        assertNewSession();
    }

    @Test
    public void doFilterGetSessionFalseInvalidSessionId() throws Exception {
        setSessionCookie("INVALID");
        doFilter(new DoInFilter() {
            @Override
            public void doFilter(HttpServletRequest wrappedRequest) {
                wrappedRequest.getSession(false);
            }
        });

        assertNoSession();
    }

    private void assertNewSession() {
        Cookie cookie = getSessionCookie();
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotEqualTo("INVALID");
        assertThat(request.getSession(false)).isNull();
    }

    private void assertNoSession() {
        assertThat(getSessionCookie()).isNull();
        assertThat(request.getSession(false)).isNull();
    }

    private Cookie getSessionCookie() {
        return response.getCookie("SESSION");
    }

    private void setSessionCookie(String sessionId) {
        request.setCookies(new Cookie[] { new Cookie("SESSION", sessionId) });
    }

    private HttpServletRequest getWrappedRequest() throws ServletException, IOException {
        chain.reset();
        filter.doFilter(request, response, chain);
        return (HttpServletRequest) chain.getRequest();
    }

    private void doFilter(final DoInFilter doInFilter) throws ServletException, IOException {
        chain = new MockFilterChain(new HttpServlet() {}, new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                doInFilter.doFilter(request);
            }
        });
        filter.doFilter(request, response, chain);
    }

    interface DoInFilter {
        void doFilter(HttpServletRequest wrappedRequest);
    }
}