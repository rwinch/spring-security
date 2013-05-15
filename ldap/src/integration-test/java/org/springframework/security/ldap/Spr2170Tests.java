/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.security.ldap;

import javax.naming.directory.DirContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.ldap.core.support.SimpleDirContextAuthenticationStrategy;
import org.springframework.security.ldap.server.ApacheDSContainer;

/**
 * @author Rob Winch
 *
 */
public class Spr2170Tests extends AbstractLdapIntegrationTests {
    static {
        System.setProperty("com.sun.jndi.ldap.connect.pool.debug", "all");
        System.setProperty("com.sun.jndi.ldap.connect.pool.maxsize", "20");
        System.setProperty("com.sun.jndi.ldap.connect.pool.prefsize", "10");
        System.setProperty("com.sun.jndi.ldap.connect.pool.timeout", "300000");
    }

    private static ApacheDSContainer server;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new ApacheDSContainer("dc=springframework,dc=org", "classpath:test-server.ldif");
        server.setPort(53389);
        server.afterPropertiesSet();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void poolleak() throws Exception {

        DefaultSpringSecurityContextSource context = (DefaultSpringSecurityContextSource) getContextSource();
//        context.setAuthenticationStrategy(new SimpleDirContextAuthenticationStrategy());

        for (int i = 0; i < 100; i++) {
            DirContext dirContext = null;
            try {
                dirContext = context.getContext("uid=user" + i
                        + ",ou=people,dc=springframework,dc=org", "password");
            } finally {
                LdapUtils.closeContext(dirContext);
            }
        }
        com.sun.jndi.ldap.LdapPoolManager.showStats(System.out);
        System.out.println("Done");
    }
}