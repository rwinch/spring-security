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

package org.springframework.security.saml2.serviceprovider.authentication;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * @since 5.2
 */
public class Saml2Authentication extends AbstractAuthenticationToken {

	private final AuthenticatedPrincipal principal;
	private final String saml2Response;

	public Saml2Authentication(String saml2Response, AuthenticatedPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		Assert.notNull(principal, "principal cannot be null");
		Assert.hasText(saml2Response, "saml2Response cannot be null");
		this.principal = principal;
		this.saml2Response = saml2Response;
		setAuthenticated(true);
	}

	@Override
	public Object getPrincipal() {
		return this.principal;
	}

	public String getSaml2Response() {
		return this.saml2Response;
	}

	@Override
	public Object getCredentials() {
		return getSaml2Response();
	}

}
