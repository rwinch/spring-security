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
import org.springframework.security.oauth2.client.authorization.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.authorization.HttpSessionAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2Attributes;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.protocol.AuthorizationCodeAuthorizationResponseAttributes;
import org.springframework.security.oauth2.core.protocol.AuthorizationRequestAttributes;
import org.springframework.security.oauth2.core.protocol.ErrorResponseAttributes;
import org.springframework.security.oauth2.core.protocol.ResponseAttributesExtractor;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;


/**
 * Handles an OAuth 2.0 Authorization Response for the Authorization Code Grant flow.
 *
 * @author Joe Grandja
 */
public class AuthorizationCodeAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
	private static final String AUTHORIZE_BASE_URI = "/oauth2/authorize/code";
	private static final String CLIENT_ALIAS_VARIABLE_NAME = "clientAlias";
	private static final String AUTHORIZE_URI = AUTHORIZE_BASE_URI + "/{" + CLIENT_ALIAS_VARIABLE_NAME + "}";
	private final RequestMatcher authorizeRequestMatcher = new AntPathRequestMatcher(AUTHORIZE_URI);
	private ClientRegistrationRepository clientRegistrationRepository;
	private AuthorizationRequestRepository authorizationRequestRepository = new HttpSessionAuthorizationRequestRepository();

	public AuthorizationCodeAuthenticationProcessingFilter() {
		super(AUTHORIZE_URI);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		if (this.isAuthorizationCodeErrorResponse(request)) {
			ErrorResponseAttributes authorizationError = ResponseAttributesExtractor.extractErrorResponse(request);
			OAuth2Error oauth2Error = OAuth2Error.valueOf(authorizationError.getErrorCode(),
					authorizationError.getErrorDescription(), authorizationError.getErrorUri());
			this.getAuthorizationRequestRepository().removeAuthorizationRequest(request);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}

		AuthorizationRequestAttributes matchingAuthorizationRequest = this.resolveAuthorizationRequest(request);

		ClientRegistration clientRegistration = this.getClientRegistrationRepository().getRegistrationByClientId(
				matchingAuthorizationRequest.getClientId());

		AuthorizationCodeAuthorizationResponseAttributes authorizationCodeResponseAttributes =
				ResponseAttributesExtractor.extractAuthorizationCodeResponse(request);

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
		String state = request.getParameter(OAuth2Attributes.STATE);
		if (!authorizationRequest.getState().equals(state)) {
			OAuth2Error oauth2Error = OAuth2Error.invalidStateParameter();
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}

		URI redirectUri = authorizationRequest.getRedirectUri();
		if (!request.getRequestURI().equals(redirectUri.getPath())) {
			OAuth2Error oauth2Error = OAuth2Error.invalidRedirectUriParameter();
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getErrorMessage());
		}
	}

	private boolean isAuthorizationCodeErrorResponse(HttpServletRequest request) {
		return !StringUtils.isEmpty(request.getParameter(OAuth2Attributes.ERROR)) &&
			!StringUtils.isEmpty(request.getParameter(OAuth2Attributes.STATE));
	}
}
