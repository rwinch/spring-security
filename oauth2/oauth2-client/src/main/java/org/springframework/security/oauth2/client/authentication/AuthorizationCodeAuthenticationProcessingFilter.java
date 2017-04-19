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
package org.springframework.security.oauth2.client.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.converter.AuthorizationCodeAuthorizationResponseAttributesConverter;
import org.springframework.security.oauth2.client.web.converter.ErrorResponseAttributesConverter;
import org.springframework.security.oauth2.core.protocol.message.OAuth2Parameter;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.protocol.message.AuthorizationCodeAuthorizationResponseAttributes;
import org.springframework.security.oauth2.core.protocol.message.AuthorizationRequestAttributes;
import org.springframework.security.oauth2.core.protocol.message.ErrorResponseAttributes;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.security.oauth2.client.authentication.AuthorizationCodeRequestRedirectFilter.isDefaultRedirectUri;


/**
 * Handles an OAuth 2.0 Authorization Response for the Authorization Code Grant flow.
 *
 * @author Joe Grandja
 */
public class AuthorizationCodeAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
	public static final String AUTHORIZE_BASE_URI = "/oauth2/authorize/code";
	private static final String CLIENT_ALIAS_VARIABLE_NAME = "clientAlias";
	private static final String AUTHORIZE_URI = AUTHORIZE_BASE_URI + "/{" + CLIENT_ALIAS_VARIABLE_NAME + "}";
	private final ErrorResponseAttributesConverter errorResponseConverter = new ErrorResponseAttributesConverter();
	private final AuthorizationCodeAuthorizationResponseAttributesConverter authorizationCodeResponseConverter =
		new AuthorizationCodeAuthorizationResponseAttributesConverter();
	private final RequestMatcher authorizeRequestMatcher = new AntPathRequestMatcher(AUTHORIZE_URI);
	private ClientRegistrationRepository clientRegistrationRepository;
	private AuthorizationRequestRepository authorizationRequestRepository = new HttpSessionAuthorizationRequestRepository();

	public AuthorizationCodeAuthenticationProcessingFilter() {
		super(AUTHORIZE_URI);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		ErrorResponseAttributes authorizationError = this.errorResponseConverter.convert(request);
		if (authorizationError != null) {
			OAuth2Error oauth2Error = OAuth2Error.valueOf(authorizationError.getErrorCode(),
					authorizationError.getErrorDescription(), authorizationError.getErrorUri());
			this.getAuthorizationRequestRepository().removeAuthorizationRequest(request);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}

		AuthorizationRequestAttributes matchingAuthorizationRequest = this.resolveAuthorizationRequest(request);

		ClientRegistration clientRegistration = this.getClientRegistrationRepository().getRegistrationByClientId(
				matchingAuthorizationRequest.getClientId());

		// If clientRegistration.redirectUri is the default one (with Uri template variables)
		// then use matchingAuthorizationRequest.redirectUri instead
		if (isDefaultRedirectUri(clientRegistration)) {
			clientRegistration = new ClientRegistrationBuilderWithUriOverrides(
				clientRegistration, matchingAuthorizationRequest.getRedirectUri()).build();
		}

		AuthorizationCodeAuthorizationResponseAttributes authorizationCodeResponseAttributes =
				this.authorizationCodeResponseConverter.convert(request);

		AuthorizationCodeAuthenticationToken authRequest = new AuthorizationCodeAuthenticationToken(
				authorizationCodeResponseAttributes.getCode(), clientRegistration);

		authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));

		Authentication authenticated = this.getAuthenticationManager().authenticate(authRequest);

		return authenticated;
	}

	public RequestMatcher getAuthorizeRequestMatcher() {
		return this.authorizeRequestMatcher;
	}

	protected ClientRegistrationRepository getClientRegistrationRepository() {
		return this.clientRegistrationRepository;
	}

	public final void setClientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
		Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
		Assert.notEmpty(clientRegistrationRepository.getRegistrations(), "clientRegistrationRepository cannot be empty");
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	protected AuthorizationRequestRepository getAuthorizationRequestRepository() {
		return this.authorizationRequestRepository;
	}

	public final void setAuthorizationRequestRepository(AuthorizationRequestRepository authorizationRequestRepository) {
		Assert.notNull(authorizationRequestRepository, "authorizationRequestRepository cannot be null");
		this.authorizationRequestRepository = authorizationRequestRepository;
	}

	private AuthorizationRequestAttributes resolveAuthorizationRequest(HttpServletRequest request) {
		AuthorizationRequestAttributes authorizationRequest =
				this.getAuthorizationRequestRepository().loadAuthorizationRequest(request);
		if (authorizationRequest == null) {
			OAuth2Error oauth2Error = OAuth2Error.authorizationRequestNotFound();
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}
		this.getAuthorizationRequestRepository().removeAuthorizationRequest(request);
		this.assertMatchingAuthorizationRequest(request, authorizationRequest);
		return authorizationRequest;
	}

	private void assertMatchingAuthorizationRequest(HttpServletRequest request, AuthorizationRequestAttributes authorizationRequest) {
		String state = request.getParameter(OAuth2Parameter.STATE);
		if (!authorizationRequest.getState().equals(state)) {
			OAuth2Error oauth2Error = OAuth2Error.invalidStateParameter();
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}

		if (!request.getRequestURL().toString().equals(authorizationRequest.getRedirectUri())) {
			OAuth2Error oauth2Error = OAuth2Error.invalidRedirectUriParameter();
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}
	}

	private static class ClientRegistrationBuilderWithUriOverrides extends ClientRegistration.Builder {

		private ClientRegistrationBuilderWithUriOverrides(ClientRegistration clientRegistration, String redirectUri) {
			super(clientRegistration.getClientId());
			this.clientSecret(clientRegistration.getClientSecret());
			this.clientAuthenticationMethod(clientRegistration.getClientAuthenticationMethod());
			this.authorizedGrantType(clientRegistration.getAuthorizedGrantType());
			this.redirectUri(redirectUri);
			if (!CollectionUtils.isEmpty(clientRegistration.getScopes())) {
				this.scopes(clientRegistration.getScopes().stream().toArray(String[]::new));
			}
			this.authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri());
			this.tokenUri(clientRegistration.getProviderDetails().getTokenUri());
			this.userInfoUri(clientRegistration.getProviderDetails().getUserInfoUri());
			this.clientName(clientRegistration.getClientName());
			this.clientAlias(clientRegistration.getClientAlias());
		}
	}
}
