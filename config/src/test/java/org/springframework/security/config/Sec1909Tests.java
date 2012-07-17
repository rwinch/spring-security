package org.springframework.security.config;

import org.junit.After;
import org.junit.Test;
import org.springframework.security.config.util.InMemoryXmlApplicationContext;

public class Sec1909Tests {

    private InMemoryXmlApplicationContext appContext;

    @After
    public void tearDown() throws Exception {
        if(appContext != null) {
            appContext.destroy();
        }
    }

    @Test
    public void deprecatedExample() {
        setContext("<b:bean class=\"org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler\" />\n" +
                "    <global-method-security secured-annotations=\"enabled\" />\n" +
                "    <http auto-config=\"true\" access-denied-page=\"/accessDenied\">\n" +
                "        <form-login login-page=\"/login\" default-target-url=\"/log/viewLogs\" authentication-failure-url=\"/login?login_error=1\" />\n" +
                "        <logout logout-success-url=\"/logout\" />\n" +
                "        <session-management invalid-session-url=\"/login\">\n" +
                "            <concurrency-control max-sessions=\"1\" expired-url=\"/sessionTimeout\" error-if-maximum-exceeded=\"true\" />\n" +
                "        </session-management>\n" +
                "        <http-basic />\n" +
                "        <anonymous enabled=\"true\" granted-authority=\"ROLE_ANONYMOUS\" username=\"Anonymous_User\" />\n" +
                "        <intercept-url pattern=\"/log/**\" access=\"ROLE_USER,ROLE_ADMIN\" />\n" +
                "    </http>");
    }

    private void setContext(String configuration) {
        String xml = configuration + ConfigTestUtils.AUTH_PROVIDER_XML;
        appContext = new InMemoryXmlApplicationContext(xml);
    }
}
