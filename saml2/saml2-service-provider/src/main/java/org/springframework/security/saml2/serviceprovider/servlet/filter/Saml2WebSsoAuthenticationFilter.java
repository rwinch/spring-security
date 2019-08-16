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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.serviceprovider.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.serviceprovider.provider.Saml2RelyingPartyRegistration;
import org.springframework.security.saml2.serviceprovider.provider.Saml2RelyingPartyRepository;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2Utils.decode;
import static org.springframework.security.saml2.serviceprovider.servlet.filter.Saml2Utils.inflate;
import static org.springframework.util.StringUtils.hasText;

/**
 * @since 5.2
 */
public class Saml2WebSsoAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private final RequestMatcher matcher;
	private final Saml2RelyingPartyRepository relyingPartyRepository;

	public Saml2WebSsoAuthenticationFilter(RequestMatcher matcher, Saml2RelyingPartyRepository relyingPartyRepository) {
		super(matcher);
		this.matcher = matcher;
		this.relyingPartyRepository = relyingPartyRepository;
		setAllowSessionCreation(true);
		setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
	}

	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		return (super.requiresAuthentication(request, response) && hasText(request.getParameter("SAMLResponse")));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		if (!requiresAuthentication(request, response)) {
			throw new BadCredentialsException("Missing SAML2 response data");
		}
		String saml2Response = request.getParameter("SAMLResponse");
		byte[] b = decode(saml2Response);

		String responseXml = inflateIfRequired(request, b);
		Saml2RelyingPartyRegistration rp =
				this.relyingPartyRepository.findByAlias(this.matcher.matcher(request).getVariables().get("alias"));
		String localSpEntityId = Saml2Utils.getServiceProviderEntityId(rp, request);
		final Saml2AuthenticationToken authentication = new Saml2AuthenticationToken(
				responseXml,
				request.getRequestURL().toString(),
				rp.getRemoteIdpEntityId(),
				localSpEntityId,
				rp.getCredentialsForUsage()
		);
		return getAuthenticationManager().authenticate(authentication);
	}


	private String inflateIfRequired(HttpServletRequest request, byte[] b) {
		if (HttpMethod.GET.matches(request.getMethod())) {
			return inflate(b);
		}
		else {
			return new String(b, UTF_8);
		}
	}


}
