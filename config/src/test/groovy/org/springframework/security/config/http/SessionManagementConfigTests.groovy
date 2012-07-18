/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.config.http

import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.util.FieldUtils
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy
import org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter
import org.springframework.security.web.session.ConcurrentSessionFilter
import org.springframework.security.web.session.SessionManagementFilter
import static org.junit.Assert.assertSame

/**
 * Tests session-related functionality for the &lt;http&gt; namespace element and &lt;session-management&gt;
 *
 * @author Luke Taylor
 * @author Rob Winch
 */
class SessionManagementConfigTests extends AbstractHttpConfigTests {

    def concurrentSessionSupportAddsFilterAndExpectedBeans() {
        when:
        httpAutoConfig {
            'session-management'() {
                'concurrency-control'('session-registry-alias':'sr', 'expired-url': '/expired')
            }
        }
        createAppContext();
        List filters = getFilters("/someurl");
        def concurrentSessionFilter = filters.get(0)

        then:
        concurrentSessionFilter instanceof ConcurrentSessionFilter
        concurrentSessionFilter.expiredUrl == '/expired'
        appContext.getBean("sr") != null
        getFilter(SessionManagementFilter.class) != null
        sessionRegistryIsValid();

        concurrentSessionFilter.handlers.size() == 1
        def logoutHandler = concurrentSessionFilter.handlers[0]
        logoutHandler instanceof SecurityContextLogoutHandler
        logoutHandler.invalidateHttpSession

    }

    def 'concurrency-control adds custom logout handlers'() {
        when: 'Custom logout and remember-me'
        httpAutoConfig {
            'session-management'() {
                'concurrency-control'()
            }
            'logout'('invalidate-session': false)
            'remember-me'()
        }
        createAppContext();

        List filters = getFilters("/someurl")
        ConcurrentSessionFilter concurrentSessionFilter = filters.get(0)
        def logoutHandlers = concurrentSessionFilter.handlers

        then: 'ConcurrentSessionFilter contains the customized LogoutHandlers'
        logoutHandlers.size() == 2
        def securityCtxlogoutHandler = logoutHandlers.find { it instanceof SecurityContextLogoutHandler }
        securityCtxlogoutHandler.invalidateHttpSession == false
        def remembermeLogoutHandler = logoutHandlers.find { it instanceof RememberMeServices }
        remembermeLogoutHandler == getFilter(RememberMeAuthenticationFilter.class).rememberMeServices
    }

    def 'concurrency-control with remember-me and no LogoutFilter contains SecurityContextLogoutHandler and RememberMeServices as LogoutHandlers'() {
        when: 'RememberMe and No LogoutFilter'
        xml.http(['entry-point-ref': 'entryPoint'], {
            'session-management'() {
                'concurrency-control'()
            }
            'remember-me'()
        })
        bean('entryPoint', 'org.springframework.security.web.authentication.Http403ForbiddenEntryPoint')
        createAppContext()

        List filters = getFilters("/someurl")
        ConcurrentSessionFilter concurrentSessionFilter = filters.get(0)
        def logoutHandlers = concurrentSessionFilter.handlers

        then: 'SecurityContextLogoutHandler and RememberMeServices are in ConcurrentSessionFilter logoutHandlers'
        !filters.find { it instanceof LogoutFilter }
        logoutHandlers.size() == 2
        def securityCtxlogoutHandler = logoutHandlers.find { it instanceof SecurityContextLogoutHandler }
        securityCtxlogoutHandler.invalidateHttpSession == true
        logoutHandlers.find { it instanceof RememberMeServices } == getFilter(RememberMeAuthenticationFilter).rememberMeServices
    }

    def 'concurrency-control with no remember-me or LogoutFilter contains SecurityContextLogoutHandler as LogoutHandlers'() {
        when: 'No Logout Filter or RememberMe'
        xml.http(['entry-point-ref': 'entryPoint'], {
            'session-management'() {
                'concurrency-control'()
            }
        })
        bean('entryPoint', 'org.springframework.security.web.authentication.Http403ForbiddenEntryPoint')
        createAppContext()

        List filters = getFilters("/someurl")
        ConcurrentSessionFilter concurrentSessionFilter = filters.get(0)
        def logoutHandlers = concurrentSessionFilter.handlers

        then: 'Only SecurityContextLogoutHandler is found in ConcurrentSessionFilter logoutHandlers'
        !filters.find { it instanceof LogoutFilter }
        logoutHandlers.size() == 1
        def securityCtxlogoutHandler = logoutHandlers.find { it instanceof SecurityContextLogoutHandler }
        securityCtxlogoutHandler.invalidateHttpSession == true
    }
}
