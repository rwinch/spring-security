/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.security.oauth2.client.config.annotation.web.configurers;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.AuthorizationGrantTokenExchanger;
import org.springframework.security.oauth2.client.authorization.AuthorizationRequestUriBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userdetails.UserInfoUserDetailsService;
import org.springframework.security.oauth2.core.userdetails.OAuth2UserDetails;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Joe Grandja
 */
public final class OAuth2LoginSecurityConfigurer<B extends HttpSecurityBuilder<B>> extends
		AbstractHttpConfigurer<OAuth2LoginSecurityConfigurer<B>, B> {

	private final AuthorizationCodeRequestRedirectFilterConfigurer<B> authorizationCodeRequestRedirectFilterConfigurer;
	private final AuthorizationCodeAuthenticationFilterConfigurer<B> authorizationCodeAuthenticationFilterConfigurer;
	private final UserInfoEndpointConfig userInfoEndpointConfig;

	public OAuth2LoginSecurityConfigurer() {
		this.authorizationCodeRequestRedirectFilterConfigurer = new AuthorizationCodeRequestRedirectFilterConfigurer<>();
		this.authorizationCodeAuthenticationFilterConfigurer = new AuthorizationCodeAuthenticationFilterConfigurer<>();
		this.userInfoEndpointConfig = new UserInfoEndpointConfig();
	}

	public OAuth2LoginSecurityConfigurer<B> clients(ClientRegistration... clientRegistrations) {
		Assert.notEmpty(clientRegistrations, "clientRegistrations cannot be empty");
		return clients(new InMemoryClientRegistrationRepository(Arrays.asList(clientRegistrations)));
	}

	public OAuth2LoginSecurityConfigurer<B> clients(ClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		Assert.notEmpty(clientRegistrationRepository.getRegistrations(), "clientRegistrationRepository cannot be empty");
		this.getBuilder().setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
		return this;
	}

	public OAuth2LoginSecurityConfigurer<B> authorizationRequestBuilder(AuthorizationRequestUriBuilder authorizationRequestBuilder) {
		Assert.notNull(authorizationRequestBuilder, "authorizationRequestBuilder cannot be null");
		this.authorizationCodeRequestRedirectFilterConfigurer.authorizationRequestBuilder(authorizationRequestBuilder);
		return this;
	}

	public OAuth2LoginSecurityConfigurer<B> authorizationCodeTokenExchanger(
			AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger) {

		Assert.notNull(authorizationCodeTokenExchanger, "authorizationCodeTokenExchanger cannot be null");
		this.authorizationCodeAuthenticationFilterConfigurer.authorizationCodeTokenExchanger(authorizationCodeTokenExchanger);
		return this;
	}

	public UserInfoEndpointConfig userInfoEndpoint() {
		return this.userInfoEndpointConfig;
	}

	public class UserInfoEndpointConfig {

		private UserInfoEndpointConfig() {
		}

		public OAuth2LoginSecurityConfigurer<B> userInfoService(UserInfoUserDetailsService userInfoService) {
			Assert.notNull(userInfoService, "userInfoService cannot be null");
			OAuth2LoginSecurityConfigurer.this.authorizationCodeAuthenticationFilterConfigurer.userInfoUserDetailsService(userInfoService);
			return this.and();
		}

		public OAuth2LoginSecurityConfigurer<B> userInfoTypeMapping(Class<? extends OAuth2UserDetails> userInfoType, URI userInfoUri) {
			Assert.notNull(userInfoType, "userInfoType cannot be null");
			Assert.notNull(userInfoUri, "userInfoUri cannot be null");
			OAuth2LoginSecurityConfigurer.this.authorizationCodeAuthenticationFilterConfigurer.userInfoTypeMapping(userInfoType, userInfoUri);
			return this.and();
		}

		public OAuth2LoginSecurityConfigurer<B> and() {
			return OAuth2LoginSecurityConfigurer.this;
		}
	}

	@Override
	public void init(B http) throws Exception {
		this.authorizationCodeRequestRedirectFilterConfigurer.setBuilder(http);
		this.authorizationCodeAuthenticationFilterConfigurer.setBuilder(http);

		this.authorizationCodeRequestRedirectFilterConfigurer.init(http);
		this.authorizationCodeAuthenticationFilterConfigurer.init(http);
	}

	@Override
	public void configure(B http) throws Exception {
		this.authorizationCodeRequestRedirectFilterConfigurer.configure(http);
		this.authorizationCodeAuthenticationFilterConfigurer.configure(http);
	}

	public static OAuth2LoginSecurityConfigurer<HttpSecurity> oauth2Login() {
		return new OAuth2LoginSecurityConfigurer<>();
	}

	protected static ClientRegistrationRepository getDefaultClientRegistrationRepository(ApplicationContext context) {
		Map<String, ClientRegistration> clientRegistrations = context.getBeansOfType(ClientRegistration.class);
		ClientRegistrationRepository clientRegistrationRepository = new InMemoryClientRegistrationRepository(
				clientRegistrations.values().stream().collect(Collectors.toList()));
		return clientRegistrationRepository;
	}
}
