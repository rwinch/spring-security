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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.saml2.serviceprovider.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.serviceprovider.authentication.OpenSamlAuthenticationRequestResolver;
import org.springframework.security.saml2.serviceprovider.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.serviceprovider.provider.RelyingPartyRegistration;
import org.springframework.security.saml2.serviceprovider.provider.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2LoginPageGeneratingFilter;
import org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import static java.util.Optional.ofNullable;

/**
 * @since 5.2
 */
public class Saml2ServiceProviderConfigurer
		extends AbstractHttpConfigurer<Saml2ServiceProviderConfigurer, HttpSecurity> {

	private static final String PREFIX = "/saml2";
	private final String filterPrefix;
	private AuthenticationProvider authenticationProvider;
	private RelyingPartyRegistrationRepository providerDetailsRepository;
	private AuthenticationEntryPoint entryPoint = null;
	private Saml2AuthenticationRequestResolver authenticationRequestResolver;
	protected Saml2ServiceProviderConfigurer(String filterPrefix) {
		this.filterPrefix = filterPrefix;
	}

	public static Saml2ServiceProviderConfigurer saml2Login() {
		return saml2Login(PREFIX);
	}

	public static Saml2ServiceProviderConfigurer saml2Login(String filterPrefix) {
		return new Saml2ServiceProviderConfigurer(filterPrefix);
	}

	private static class RelyingPartyAliasUrlRequestMatcher implements RequestMatcher {

		private final AntPathRequestMatcher filterProcessesMatcher;
		private final AntPathRequestMatcher aliasExtractor;

		RelyingPartyAliasUrlRequestMatcher(String filterProcessesUrl, String aliasExtractorUrl) {
			this.filterProcessesMatcher = new AntPathRequestMatcher(filterProcessesUrl);
			this.aliasExtractor = new AntPathRequestMatcher(aliasExtractorUrl);
		}

		@Override
		public MatchResult matcher(HttpServletRequest request) {
			return aliasExtractor.matcher(request);
		}

		@Override
		public boolean matches(HttpServletRequest request) {
			return filterProcessesMatcher.matches(request);
		}
	}

	public Saml2ServiceProviderConfigurer authenticationProvider(AuthenticationProvider provider) {
		this.authenticationProvider = provider;
		return this;
	}

	public Saml2ServiceProviderConfigurer authenticationRequestResolver(Saml2AuthenticationRequestResolver resolver) {
		this.authenticationRequestResolver = resolver;
		return this;
	}

	public Saml2ServiceProviderConfigurer relyingPartyRegistrationRepository(RelyingPartyRegistrationRepository repo) {
		this.providerDetailsRepository = repo;
		return this;
	}

	public Saml2ServiceProviderConfigurer authenticationEntryPoint(AuthenticationEntryPoint ep) {
		this.entryPoint = ep;
		return this;
	}

	@Override
	public void init(HttpSecurity builder) throws Exception {
		super.init(builder);
		builder.csrf().ignoringAntMatchers(this.filterPrefix + "/**");

		this.providerDetailsRepository = getSharedObject(
				builder,
				RelyingPartyRegistrationRepository.class,
				() -> this.providerDetailsRepository,
				this.providerDetailsRepository
		);

		if (this.authenticationProvider == null) {
			this.authenticationProvider = new OpenSamlAuthenticationProvider();
		}
		builder.authenticationProvider(postProcess(this.authenticationProvider));

		if (this.entryPoint != null) {
			registerDefaultAuthenticationEntryPoint(builder, this.entryPoint);
		}
		else {
			final Map<String, String> providerUrlMap =
					getIdentityProviderUrlMap(this.filterPrefix + "/authenticate/", this.providerDetailsRepository);

			String loginUrl = (providerUrlMap.size() != 1) ?
					"/login" :
					providerUrlMap.entrySet().iterator().next().getValue();
			registerDefaultAuthenticationEntryPoint(builder, new LoginUrlAuthenticationEntryPoint(loginUrl));
		}

		this.authenticationRequestResolver = getSharedObject(
				builder,
				Saml2AuthenticationRequestResolver.class,
				() -> new OpenSamlAuthenticationRequestResolver(),
				this.authenticationRequestResolver
		);
	}

	@Override
	public void configure(HttpSecurity builder) throws Exception {
		String authNRequestUrlPrefix = this.filterPrefix + "/authenticate/";
		configureSaml2LoginPageFilter(builder, authNRequestUrlPrefix, "/login");

		String authNRequestUriMatcher = authNRequestUrlPrefix + "**";
		String authNAliasExtractor = authNRequestUrlPrefix + "{registrationId}/**";
		configureSaml2AuthenticationRequestFilter(
				builder,
				new RelyingPartyAliasUrlRequestMatcher(authNRequestUriMatcher, authNAliasExtractor)
		);

		String ssoUriPrefix = this.filterPrefix + "/SSO/";
		String ssoUriMatcher = ssoUriPrefix + "**";
		String ssoAliasExtractor = ssoUriPrefix + "{registrationId}/**";
		configureSaml2WebSsoAuthenticationFilter(
				builder,
				new RelyingPartyAliasUrlRequestMatcher(ssoUriMatcher, ssoAliasExtractor)
		);

	}

	private void configureSaml2LoginPageFilter(HttpSecurity builder, String authRequestPrefixUrl, String loginFilterUrl) {
		RelyingPartyRegistrationRepository idpRepo = this.providerDetailsRepository;
		Map<String, String> idps = getIdentityProviderUrlMap(authRequestPrefixUrl, idpRepo);
		DefaultLoginPageGeneratingFilter loginPageGeneratingFilter = builder.getSharedObject(DefaultLoginPageGeneratingFilter.class);
		if (loginPageGeneratingFilter != null) {
			loginPageGeneratingFilter.setSaml2LoginEnabled(true);
			loginPageGeneratingFilter.setSaml2AuthenticationUrlToProviderName(idps);
		}
		else {
			Filter loginPageFilter = new Saml2LoginPageGeneratingFilter(loginFilterUrl, idps);
			builder.addFilterAfter(loginPageFilter, HeaderWriterFilter.class);
		}
	}

	private void configureSaml2AuthenticationRequestFilter(HttpSecurity builder, RequestMatcher matcher) {
		Filter authenticationRequestFilter = new Saml2WebSsoAuthenticationRequestFilter(
				matcher,
				"{baseUrl}" + this.filterPrefix + "/SSO/{registrationId}",
				this.providerDetailsRepository,
				this.authenticationRequestResolver
		);
		builder.addFilterAfter(authenticationRequestFilter, HeaderWriterFilter.class);
	}

	private void configureSaml2WebSsoAuthenticationFilter(HttpSecurity builder, RequestMatcher matcher) {
		AuthenticationFailureHandler failureHandler =
				new SimpleUrlAuthenticationFailureHandler("/login?error=saml2-error");
		Saml2WebSsoAuthenticationFilter webSsoFilter =
				new Saml2WebSsoAuthenticationFilter(matcher, this.providerDetailsRepository);
		webSsoFilter.setAuthenticationFailureHandler(failureHandler);
		webSsoFilter.setAuthenticationManager(builder.getSharedObject(AuthenticationManager.class));
		builder.addFilterAfter(webSsoFilter, HeaderWriterFilter.class);
	}

	private <C> C getSharedObject(HttpSecurity http, Class<C> clazz, Supplier<? extends C> creator, Object existingInstance) {
		C result = ofNullable((C) existingInstance).orElseGet(() -> getSharedObject(http, clazz));
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
	private void registerDefaultAuthenticationEntryPoint(HttpSecurity http, AuthenticationEntryPoint entryPoint) {
		ExceptionHandlingConfigurer<HttpSecurity> exceptionHandling =
				http.getConfigurer(ExceptionHandlingConfigurer.class);

		if (exceptionHandling == null) {
			return;
		}

		exceptionHandling.authenticationEntryPoint(entryPoint);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getIdentityProviderUrlMap(String authRequestPrefixUrl, RelyingPartyRegistrationRepository idpRepo) {
		Map<String, String> idps = new LinkedHashMap<>();
		if (idpRepo instanceof Iterable) {
			Iterable<RelyingPartyRegistration> repo = (Iterable<RelyingPartyRegistration>) idpRepo;
			repo.forEach(
					p -> idps.put(p.getRegistrationId(), authRequestPrefixUrl + p.getRegistrationId())
			);
		}
		return idps;
	}

	private <C> C getSharedObject(HttpSecurity http, Class<C> clazz) {
		return http.getSharedObject(clazz);
	}

	private <C> void setSharedObject(HttpSecurity http, Class<C> clazz, C object) {
		if (http.getSharedObject(clazz) == null) {
			http.setSharedObject(clazz, object);
		}
	}


}
