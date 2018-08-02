/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.security.oauth2.client.web.server;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter;
import org.springframework.security.oauth2.client.web.reactive.ReactiveOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Rob Winch
 * @since 5.1
 */
public class OAuth2AuthorizationCodeGrantWebFilter implements WebFilter {
	private final ReactiveAuthenticationManager authenticationManager;

	private final ReactiveOAuth2AuthorizedClientRepository authorizedClientRepository;

	private ServerAuthenticationSuccessHandler authenticationSuccessHandler;

	private Function<ServerWebExchange, Mono<Authentication>> authenticationConverter;

	private ServerAuthenticationFailureHandler authenticationFailureHandler;

	private ServerWebExchangeMatcher requiresAuthenticationMatcher;
	private AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken("key", "anonymous",
					AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

	public OAuth2AuthorizationCodeGrantWebFilter(
			ReactiveAuthenticationManager authenticationManager,
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientRepository authorizedClientRepository) {
		this.authenticationManager = authenticationManager;
		this.authorizedClientRepository = authorizedClientRepository;
		this.requiresAuthenticationMatcher = new PathPatternParserServerWebExchangeMatcher("/authorize/oauth2/code/{registrationId}");
		this.authenticationConverter = new ServerOAuth2AuthorizationCodeAuthenticationTokenConverter(clientRegistrationRepository);
		this.authenticationSuccessHandler = new RedirectServerAuthenticationSuccessHandler();

		this.authenticationFailureHandler = (webFilterExchange, exception) -> Mono.error(exception);
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return this.requiresAuthenticationMatcher.matches(exchange)
				.filter( matchResult -> matchResult.isMatch())
				.flatMap( matchResult -> this.authenticationConverter.apply(exchange))
				.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
				.flatMap( token -> authenticate(exchange, chain, token));
	}

	private Mono<Void> authenticate(ServerWebExchange exchange,
			WebFilterChain chain, Authentication token) {
		WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);
		return this.authenticationManager.authenticate(token)
				.switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalStateException("No provider found for " + token.getClass()))))
				.flatMap(authentication -> onAuthenticationSuccess(authentication, webFilterExchange))
				.onErrorResume(AuthenticationException.class, e -> this.authenticationFailureHandler
						.onAuthenticationFailure(webFilterExchange, e));
	}

	private Mono<Void> onAuthenticationSuccess(Authentication authentication, WebFilterExchange webFilterExchange) {
		OAuth2AuthorizationCodeAuthenticationToken authenticationResult = (OAuth2AuthorizationCodeAuthenticationToken) authentication;
		OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
				authenticationResult.getClientRegistration(),
				authenticationResult.getName(),
				authenticationResult.getAccessToken(),
				authenticationResult.getRefreshToken());
		return this.authenticationSuccessHandler
					.onAuthenticationSuccess(webFilterExchange, authentication)
					.then(ReactiveSecurityContextHolder.getContext()
							.map(SecurityContext::getAuthentication)
							.defaultIfEmpty(this.anonymousToken)
							.flatMap(principal -> this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, principal, webFilterExchange.getExchange()))
					);
	}
}
