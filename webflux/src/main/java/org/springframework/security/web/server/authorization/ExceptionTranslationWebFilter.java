/*
 *
 *  * Copyright 2002-2017 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.springframework.security.web.server.authorization;


import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.web.server.AuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.www.HttpBasicAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 *
 * @author Rob Winch
 * @since 5.0
 */
public class ExceptionTranslationWebFilter implements WebFilter {
	private AuthenticationEntryPoint entryPoint = new HttpBasicAuthenticationEntryPoint();

	private AccessDeniedHandler accessDeniedHandler = new HttpStatusAccessDeniedHandler(HttpStatus.FORBIDDEN);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(exchange)
			.onErrorResume(AccessDeniedException.class, denied -> {
				return exchange.getPrincipal()
					.switchIfEmpty( Mono.defer( () -> entryPoint.commence(exchange, new AuthenticationCredentialsNotFoundException("Not Authenticated", denied))))
					.flatMap( principal -> accessDeniedHandler.handle(exchange, denied));
			});
	}

}
