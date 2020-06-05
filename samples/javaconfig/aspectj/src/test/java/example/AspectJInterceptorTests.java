/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringJUnitConfig(AspectjSecurityConfig.class)
public class AspectJInterceptorTests {

	@Autowired
	private Service service;

	@Autowired
	private SecuredService securedService;

	@Test
	public void publicMethod() {
		this.service.publicMethod();
	}

	@Test
	public void securedMethodNotAuthenticated() {
		assertThatCode(() -> this.service.secureMethod())
			.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	@WithMockAdminUser
	public void securedMethodWrongRole() {
		assertThatCode(() -> this.service.secureMethod())
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser
	public void securedMethodEverythingOk() {
		this.service.secureMethod();
	}

	@Test
	public void securedClassNotAuthenticated() {
		assertThatCode(() -> this.securedService.secureMethod())
			.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	@WithMockAdminUser
	public void securedClassWrongRole() {
		assertThatCode(() -> this.securedService.secureMethod())
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockAdminUser
	public void securedClassWrongRoleOnNewedInstance() {
		assertThatCode(() -> new SecuredService().secureMethod())
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser
	public void securedClassEverythingOk() {
		this.securedService.secureMethod();
		new SecuredService().secureMethod();
	}

	// SEC-2595
	@Test
	public void notProxy() {
		assertThat(Proxy.isProxyClass(this.securedService.getClass())).isFalse();
	}
}
