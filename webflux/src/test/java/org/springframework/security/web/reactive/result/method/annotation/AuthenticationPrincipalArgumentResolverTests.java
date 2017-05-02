/*
 *
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

package org.springframework.security.web.reactive.result.method.annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.method.ResolvableMethod;
import org.springframework.security.web.reactive.result.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


/**
 * @author Rob Winch
 * @since 5.0
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationPrincipalArgumentResolverTests {
	@Mock
	ServerWebExchange exchange;
	@Mock
	BindingContext bindingContext;
	@Mock
	Authentication authentication;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("method").build();

	AuthenticationPrincipalArgumentResolver resolver;

	@Before
	public void setup() {
		resolver =  new AuthenticationPrincipalArgumentResolver(new ReactiveAdapterRegistry());
		when(authentication.getPrincipal()).thenReturn("user");
	}

	@Test
	public void resolveArgumentWhenIsAuthenticationThenObtainsPrincipal() throws Exception {
		MethodParameter parameter = this.testMethod.arg(String.class);
		when(exchange.getPrincipal()).thenReturn(Mono.just(authentication));

		Mono<Object> argument = resolver.resolveArgument(parameter, bindingContext, exchange);

		assertThat(argument.block()).isEqualTo(authentication.getPrincipal());
	}

	@Test
	public void resolveArgumentWhenIsNotAuthenticationThenMonoEmpty() throws Exception {
		MethodParameter parameter = this.testMethod.arg(String.class);
		when(exchange.getPrincipal()).thenReturn(Mono.just(() -> ""));

		Mono<Object> argument = resolver.resolveArgument(parameter, bindingContext, exchange);

		assertThat(argument).isNotNull();
		assertThat(argument.block()).isNull();
	}

	@Test
	public void resolveArgumentWhenIsEmptyThenMonoEmpty() throws Exception {
		MethodParameter parameter = this.testMethod.arg(String.class);
		when(exchange.getPrincipal()).thenReturn(Mono.empty());

		Mono<Object> argument = resolver.resolveArgument(parameter, bindingContext, exchange);

		assertThat(argument).isNotNull();
		assertThat(argument.block()).isNull();
	}

	@Test
	public void resolveArgumentWhenMonoIsAuthenticationThenObtainsPrincipal() throws Exception {
		MethodParameter parameter = this.testMethod.arg(Mono.class, String.class);
		when(exchange.getPrincipal()).thenReturn(Mono.just(authentication));

		Mono<Object> argument = resolver.resolveArgument(parameter, bindingContext, exchange);

		assertThat(argument.cast(Mono.class).block().block()).isEqualTo(authentication.getPrincipal());
	}

	void method(String principal, Mono<String> monoPrincipal) {}
}
