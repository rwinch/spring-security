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
package org.springframework.security.web.reactive.result.method.annotation;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.server.context.WebSessionSecurityContextRepository;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Resolves the Authentication
 * @author Rob Winch
 * @since 5.0
 */
public class AuthenticationPrincipalArgumentResolver extends HandlerMethodArgumentResolverSupport {

	public AuthenticationPrincipalArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		return exchange.getPrincipal()
			.ofType(Authentication.class)
			.flatMap( a -> {
				Mono<Object> principal = Mono.just(a.getPrincipal());
				return adapter == null ? principal : Mono.just(adapter.fromPublisher(principal));
			});
	}
}
