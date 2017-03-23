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
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.AuthorizationGrantTokenExchanger;
import org.springframework.security.oauth2.client.authentication.nimbus.NimbusAuthorizationCodeTokenExchanger;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userdetails.UserInfoUserDetailsService;
import org.springframework.security.oauth2.client.userdetails.nimbus.NimbusUserInfoUserDetailsService;
import org.springframework.security.oauth2.core.userdetails.OAuth2UserDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.oauth2.client.config.annotation.web.configurers.OAuth2LoginSecurityConfigurer.getDefaultClientRegistrationRepository;

/**
 * @author Joe Grandja
 */
final class AuthorizationCodeAuthenticationFilterConfigurer<H extends HttpSecurityBuilder<H>> extends
		AbstractAuthenticationFilterConfigurer<H, AuthorizationCodeAuthenticationFilterConfigurer<H>, AuthorizationCodeAuthenticationProcessingFilter> {

	private static final String DEFAULT_CLIENTS_PAGE_URI = "/oauth2/clients";
	private AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger;
	private UserInfoUserDetailsService userInfoUserDetailsService;
	private Map<URI, Class<? extends OAuth2UserDetails>> userInfoTypeMapping = new HashMap<>();

	AuthorizationCodeAuthenticationFilterConfigurer() {
		super(new AuthorizationCodeAuthenticationProcessingFilter(), null);
	}

	AuthorizationCodeAuthenticationFilterConfigurer<H> clientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		Assert.notEmpty(clientRegistrationRepository.getRegistrations(), "clientRegistrationRepository cannot be empty");
		this.getBuilder().setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
		return this;
	}

	AuthorizationCodeAuthenticationFilterConfigurer<H> clientsPage(String clientsPage) {
		Assert.notNull(clientsPage, "clientsPage cannot be null");
		this.loginPage(clientsPage);
		return this;
	}

	AuthorizationCodeAuthenticationFilterConfigurer<H> authorizationCodeTokenExchanger(
			AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger) {

		Assert.notNull(authorizationCodeTokenExchanger, "authorizationCodeTokenExchanger cannot be null");
		this.authorizationCodeTokenExchanger = authorizationCodeTokenExchanger;
		return this;
	}

	AuthorizationCodeAuthenticationFilterConfigurer<H> userInfoUserDetailsService(UserInfoUserDetailsService userInfoUserDetailsService) {
		Assert.notNull(userInfoUserDetailsService, "userInfoUserDetailsService cannot be null");
		this.userInfoUserDetailsService = userInfoUserDetailsService;
		return this;
	}

	AuthorizationCodeAuthenticationFilterConfigurer<H> userInfoTypeMapping(Class<? extends OAuth2UserDetails> userInfoType, URI userInfoUri) {
		Assert.notNull(userInfoType, "userInfoType cannot be null");
		Assert.notNull(userInfoUri, "userInfoUri cannot be null");
		this.userInfoTypeMapping.put(userInfoUri, userInfoType);
		return this;
	}

	@Override
	public void init(H http) throws Exception {
		if (!this.isCustomLoginPage()) {
			// Override the default login page /login (if not already configured by the user)
			//	NOTE:
			// 		This is not really a login page per se, rather a page that displays
			// 		all the registered clients with a link that initiates the 'Authorization Request' flow
			this.loginPage(DEFAULT_CLIENTS_PAGE_URI);
			this.permitAll();
		}

		AuthorizationCodeAuthenticationProvider authenticationProvider = new AuthorizationCodeAuthenticationProvider(
				this.getAuthorizationCodeTokenExchanger(), this.getUserInfoUserDetailsService());
		authenticationProvider = this.postProcess(authenticationProvider);
		http.authenticationProvider(authenticationProvider);

		super.init(http);
	}

	@Override
	public void configure(H http) throws Exception {
		AuthorizationCodeAuthenticationProcessingFilter authFilter = this.getAuthenticationFilter();
		authFilter.setClientRegistrationRepository(this.getClientRegistrationRepository());
		super.configure(http);
	}

	@Override
	protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
		// NOTE: loginProcessingUrl is purposely ignored as the matcher depends
		// 			on specific request parameters instead of the requestUri
		return AuthorizationCodeAuthenticationProcessingFilter::isAuthorizationCodeResponse;
	}

	String getClientsPage() {
		return this.getLoginPage();
	}

	private ClientRegistrationRepository getClientRegistrationRepository() {
		ClientRegistrationRepository clientRegistrationRepository = this.getBuilder().getSharedObject(ClientRegistrationRepository.class);
		if (clientRegistrationRepository == null) {
			clientRegistrationRepository = getDefaultClientRegistrationRepository(this.getBuilder().getSharedObject(ApplicationContext.class));
			this.getBuilder().setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
		}
		return clientRegistrationRepository;
	}

	private AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> getAuthorizationCodeTokenExchanger() {
		if (this.authorizationCodeTokenExchanger == null) {
			this.authorizationCodeTokenExchanger = new NimbusAuthorizationCodeTokenExchanger();
		}
		return this.authorizationCodeTokenExchanger;
	}

	private UserInfoUserDetailsService getUserInfoUserDetailsService() {
		if (this.userInfoUserDetailsService == null) {
			this.userInfoUserDetailsService = new NimbusUserInfoUserDetailsService();
		}
		if (!this.userInfoTypeMapping.isEmpty()) {
			this.userInfoTypeMapping.entrySet().stream()
					.forEach(e -> this.userInfoUserDetailsService.mapUserInfoType(e.getValue(), e.getKey()));
		}
		return this.userInfoUserDetailsService;
	}
}
