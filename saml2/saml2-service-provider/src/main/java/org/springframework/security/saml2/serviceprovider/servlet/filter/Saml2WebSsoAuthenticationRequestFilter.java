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

package org.springframework.security.saml2.serviceprovider.servlet.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.saml2.serviceprovider.authentication.Saml2AuthenticationRequest;
import org.springframework.security.saml2.serviceprovider.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.serviceprovider.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.serviceprovider.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher.MatchResult;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import static java.lang.String.format;
import static org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialUsage.SIGNING;
import static org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2Utils.deflate;
import static org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2Utils.encode;

/**
 * @since 5.2
 */
public class Saml2WebSsoAuthenticationRequestFilter extends OncePerRequestFilter {

	private final RequestMatcher matcher;
	private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
	private Saml2AuthenticationRequestResolver authenticationRequestResolver;

	public Saml2WebSsoAuthenticationRequestFilter(RequestMatcher matcher, RelyingPartyRegistrationRepository relyingPartyRegistrationRepository, Saml2AuthenticationRequestResolver authenticationRequestResolver) {
		Assert.notNull(matcher, "matcher cannot be null");
		Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
		Assert.notNull(authenticationRequestResolver, "authenticationRequestResolver cannot be null");
		this.matcher = matcher;
		this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
		this.authenticationRequestResolver = authenticationRequestResolver;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		MatchResult matcher = this.matcher.matcher(request);
		if (!matcher.isMatch()) {
			filterChain.doFilter(request, response);
			return;
		}

		String registrationId = matcher.getVariables().get("registrationId");
		sendRedirect(request, response, registrationId);
	}

	private void sendRedirect(HttpServletRequest request, HttpServletResponse response, String registrationId)
			throws IOException {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(format("Creating SAML2 SP Authentication Request for IDP[%s]", registrationId));
		}
		RelyingPartyRegistration relyingParty = this.relyingPartyRegistrationRepository.findByRegistrationId(registrationId);
		String redirectUrl = createSamlRequestRedirectUrl(request, relyingParty);
		response.sendRedirect(redirectUrl);
	}

	private String createSamlRequestRedirectUrl(HttpServletRequest request, RelyingPartyRegistration relyingParty) {
		Saml2AuthenticationRequest authNRequest = createAuthenticationRequest(relyingParty, request);
		String xml = this.authenticationRequestResolver.resolveAuthenticationRequest(authNRequest);
		String encoded = encode(deflate(xml));
		String relayState = request.getParameter("RelayState");
		String redirect = UriComponentsBuilder
				.fromUriString(relyingParty.getIdpWebSsoUrl())
				.queryParam("SAMLRequest", UriUtils.encode(encoded, StandardCharsets.ISO_8859_1))
				.queryParam("RelayState", UriUtils.encode(relayState, StandardCharsets.ISO_8859_1))
				.build(true)
				.toUriString();
		return redirect;
	}

	private Saml2AuthenticationRequest createAuthenticationRequest(RelyingPartyRegistration relyingParty, HttpServletRequest request) {
		String localSpEntityId = Saml2Utils.getServiceProviderEntityId(relyingParty, request);
		return new Saml2AuthenticationRequest(
				localSpEntityId,
				Saml2Utils.resolveUrlTemplate(
						relyingParty.getAssertionConsumerServiceUrlTemplate(),
						Saml2Utils.getApplicationUri(request),
						relyingParty.getRemoteIdpEntityId(),
						relyingParty.getRegistrationId()
				),
				relyingParty.getCredentialsForUsage(SIGNING)
		);
	}

}
