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

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AccessToken;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * @author Joe Grandja
 */
public class OAuth2AuthenticationToken extends AbstractAuthenticationToken {
	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;
	private final UserDetails principal;
	private final ClientRegistration clientRegistration;
	private final AccessToken accessToken;

	public OAuth2AuthenticationToken(ClientRegistration clientRegistration, AccessToken accessToken) {
		this(null, AuthorityUtils.NO_AUTHORITIES, clientRegistration, accessToken);
	}

	public OAuth2AuthenticationToken(UserDetails principal, Collection<? extends GrantedAuthority> authorities,
										ClientRegistration clientRegistration, AccessToken accessToken) {

		super(authorities);
		Assert.notNull(clientRegistration, "clientRegistration cannot be null");
		Assert.notNull(accessToken, "accessToken cannot be null");
		this.principal = principal;
		this.clientRegistration = clientRegistration;
		this.accessToken = accessToken;
		this.setAuthenticated(principal != null);
	}

	@Override
	public Object getPrincipal() {
		return this.principal;
	}

	@Override
	public Object getCredentials() {
		return (this.principal != null ? this.principal.getPassword() : null);
	}

	public ClientRegistration getClientRegistration() {
		return this.clientRegistration;
	}

	public AccessToken getAccessToken() {
		return this.accessToken;
	}
}
