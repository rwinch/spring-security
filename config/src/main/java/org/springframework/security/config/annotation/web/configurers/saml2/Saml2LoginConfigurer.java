/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.security.config.annotation.web.configurers.saml2;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.saml2.serviceprovider.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.serviceprovider.authentication.OpenSamlAuthenticationRequestResolver;
import org.springframework.security.saml2.serviceprovider.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.serviceprovider.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.serviceprovider.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.util.StringUtils.hasText;

/**
 * @since 5.2
 */
public final class Saml2LoginConfigurer<B extends HttpSecurityBuilder<B>> extends
		AbstractAuthenticationFilterConfigurer<B, Saml2LoginConfigurer<B>, Saml2WebSsoAuthenticationFilter> {

	private String loginPage;
	private String loginProcessingUrl = Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI;

	private AuthenticationRequestEndpointConfig authenticationRequestEndpoint = new AuthenticationRequestEndpointConfig();

	private RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

	public Saml2LoginConfigurer() {
	}

	@Override
	public Saml2LoginConfigurer<B> loginPage(String loginPage) {
		Assert.hasText(loginPage, "loginPage cannot be empty");
		this.loginPage = loginPage;
		return this;
	}

	@Override
	public Saml2LoginConfigurer<B> loginProcessingUrl(String loginProcessingUrl) {
		Assert.hasText(loginProcessingUrl, "loginProcessingUrl cannot be empty");
		Assert.state(loginProcessingUrl.contains("{registrationId}"), "{registrationId} path variable is required");
		this.loginProcessingUrl = loginProcessingUrl;
		return this;
	}

	public AuthenticationRequestEndpointConfig authenticationRequestEndpoint() {
		return this.authenticationRequestEndpoint;
	}

	public Saml2LoginConfigurer relyingPartyRegistrationRepository(RelyingPartyRegistrationRepository repo) {
		this.relyingPartyRegistrationRepository = repo;
		return this;
	}

	@Override
	protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
		return new AntPathRequestMatcher(loginProcessingUrl);
	}

	@Override
	public void init(B http) throws Exception {
		registerDefaultCsrfOverride(http);
		this.relyingPartyRegistrationRepository = getSharedObject(
				http,
				RelyingPartyRegistrationRepository.class,
				() -> this.relyingPartyRegistrationRepository,
				this.relyingPartyRegistrationRepository
		);

		Saml2WebSsoAuthenticationFilter webSsoFilter = new Saml2WebSsoAuthenticationFilter(this.relyingPartyRegistrationRepository);
		this.setAuthenticationFilter(webSsoFilter);
		super.loginProcessingUrl(this.loginProcessingUrl);

		if (hasText(this.loginPage)) {
			// Set custom login page
			super.loginPage(this.loginPage);
			super.init(http);
		} else {
			final Map<String, String> providerUrlMap =
					getIdentityProviderUrlMap(
							this.authenticationRequestEndpoint.filterProcessingUrl,
							this.relyingPartyRegistrationRepository
					);

			boolean singleProvider = providerUrlMap.size() == 1;
			if (singleProvider) {
				// Setup auto-redirect to provider login page
				// when only 1 IDP is configured
				this.updateAuthenticationDefaults();
				this.updateAccessDefaults(http);

				String loginUrl = providerUrlMap.entrySet().iterator().next().getKey();
				final LoginUrlAuthenticationEntryPoint entryPoint = new LoginUrlAuthenticationEntryPoint(loginUrl);
				registerAuthenticationEntryPoint(http, entryPoint);
			}
			else {
				super.init(http);
			}
		}
		http.authenticationProvider(getAuthenticationProvider());
		this.initDefaultLoginFilter(http);
	}

	private AuthenticationProvider getAuthenticationProvider() {
		AuthenticationProvider provider = new OpenSamlAuthenticationProvider();
		return postProcess(provider);
	}

	@Override
	public void configure(B http) throws Exception {
		http.addFilter(this.authenticationRequestEndpoint.build(http, this.loginProcessingUrl));
		super.configure(http);
	}

	private void registerDefaultCsrfOverride(B http) {
		CsrfConfigurer<B> csrf = http.getConfigurer(CsrfConfigurer.class);
		if (csrf == null) {
			return;
		}

		csrf.ignoringRequestMatchers(
				new AntPathRequestMatcher("/login/saml2/**")
		);
	}

	private void initDefaultLoginFilter(B http) {
		DefaultLoginPageGeneratingFilter loginPageGeneratingFilter = http.getSharedObject(DefaultLoginPageGeneratingFilter.class);
		if (loginPageGeneratingFilter == null || this.isCustomLoginPage()) {
			return;
		}

		loginPageGeneratingFilter.setOauth2LoginEnabled(true);
		loginPageGeneratingFilter.setOauth2AuthenticationUrlToClientName(
				this.getIdentityProviderUrlMap(
						this.authenticationRequestEndpoint.filterProcessingUrl,
						this.relyingPartyRegistrationRepository
				)
		);
		loginPageGeneratingFilter.setLoginPageUrl(this.getLoginPage());
		loginPageGeneratingFilter.setFailureUrl(this.getFailureUrl());
	}

	private <C> C getSharedObject(B http, Class<C> clazz, Supplier<? extends C> creator, Object existingInstance) {
		C result = (C) existingInstance;
		if (result == null) {
			result = getSharedObject(http, clazz);
		}
		if (result == null) {
			ApplicationContext context = getSharedObject(http, ApplicationContext.class);
			try {
				result = context.getBean(clazz);
			}
			catch (NoSuchBeanDefinitionException e) {
				if (creator != null) {
					result = creator.get();
				}
				else {
					return null;
				}
			}
		}
		setSharedObject(http, clazz, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getIdentityProviderUrlMap(
			String authRequestPrefixUrl,
			RelyingPartyRegistrationRepository idpRepo
	) {
		Map<String, String> idps = new LinkedHashMap<>();
		if (idpRepo instanceof Iterable) {
			Iterable<RelyingPartyRegistration> repo = (Iterable<RelyingPartyRegistration>) idpRepo;
			repo.forEach(
					p ->
						idps.put(
								authRequestPrefixUrl.replace("{registrationId}", p.getRegistrationId()),
								p.getRegistrationId()
						)
			);
		}
		return idps;
	}

	private <C> C getSharedObject(B http, Class<C> clazz) {
		return http.getSharedObject(clazz);
	}

	private <C> void setSharedObject(B http, Class<C> clazz, C object) {
		if (http.getSharedObject(clazz) == null) {
			http.setSharedObject(clazz, object);
		}
	}

	public final class AuthenticationRequestEndpointConfig {
		private Saml2AuthenticationRequestResolver authenticationRequestResolver;
		private String filterProcessingUrl = "/saml2/authenticate/{registrationId}";
		private AuthenticationRequestEndpointConfig() {
		}

		public AuthenticationRequestEndpointConfig authenticationRequestResolver(
				Saml2AuthenticationRequestResolver authenticationRequestResolver
		) {
			Assert.notNull(authenticationRequestResolver, "authenticationRequestResolver cannot be null");
			this.authenticationRequestResolver = authenticationRequestResolver;
			return this;
		}

		public AuthenticationRequestEndpointConfig filterProcessingUrl(
				String filterProcessingUrl
		) {
			Assert.hasText(filterProcessingUrl, "filterProcessingUrl cannot be empty");
			Assert.state(filterProcessingUrl.contains("{registrationId}"), "{registrationId} path variable is required");
			this.filterProcessingUrl = filterProcessingUrl;
			return this;
		}

		public Saml2LoginConfigurer<B> and() {
			return Saml2LoginConfigurer.this;
		}

		private Filter build(B http, String webSsoUrl) {
			this.authenticationRequestResolver = getSharedObject(
					http,
					Saml2AuthenticationRequestResolver.class,
					OpenSamlAuthenticationRequestResolver::new,
					this.authenticationRequestResolver
			);

			Filter authenticationRequestFilter = new Saml2WebSsoAuthenticationRequestFilter(
					new AntPathRequestMatcher(this.filterProcessingUrl),
					"{baseUrl}" + webSsoUrl,
					Saml2LoginConfigurer.this.relyingPartyRegistrationRepository,
					this.authenticationRequestResolver
			);

			return authenticationRequestFilter;
		}
	}


}
