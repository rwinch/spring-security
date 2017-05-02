/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.security.config.web.server;

import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.authorization.RoleAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import org.springframework.security.web.server.authorization.DelegatingReactiveAuthorizationManager;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class AuthorizeExchangeBuilder extends AbstractServerWebExchangeMatcherRegistry<AuthorizeExchangeBuilder.Access> {
	private DelegatingReactiveAuthorizationManager.Builder managerBldr = DelegatingReactiveAuthorizationManager.builder();
	private ServerWebExchangeMatcher matcher;

	@Override
	protected Access registerMatcher(ServerWebExchangeMatcher matcher) {
		this.matcher = matcher;
		return new Access();
	}

	public WebFilter build() {
		return new AuthorizationWebFilter(managerBldr.build());
	}

	public final class Access {

		public void permitAll() {
			access( (a,e) -> Mono.just(true));
		}

		public void denyAll() {
			access( (a,e) -> Mono.just(false));
		}

		public void hasRole(String role) {
			access(new RoleAuthorizationManager<>("ROLE_"+ role));
		}

		public void authenticated() {
			access(AuthenticatedAuthorizationManager.authenticated());
		}

		public void access(ReactiveAuthorizationManager<AuthorizationContext> manager) {
			managerBldr.add(matcher, manager);
			matcher = null;
		}
	}
}
