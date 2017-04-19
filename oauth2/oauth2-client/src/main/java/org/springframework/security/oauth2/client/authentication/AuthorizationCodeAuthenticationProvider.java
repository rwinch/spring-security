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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.oauth2.client.user.OAuth2UserService;
import org.springframework.security.oauth2.core.AccessToken;
import org.springframework.security.oauth2.core.protocol.message.TokenResponseAttributes;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * @author Joe Grandja
 */
public class AuthorizationCodeAuthenticationProvider implements AuthenticationProvider {
	private final AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger;
	private final OAuth2UserService userInfoService;
	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

	public AuthorizationCodeAuthenticationProvider(
			AuthorizationGrantTokenExchanger<AuthorizationCodeAuthenticationToken> authorizationCodeTokenExchanger,
			OAuth2UserService userInfoService) {

		Assert.notNull(authorizationCodeTokenExchanger, "authorizationCodeTokenExchanger cannot be null");
		Assert.notNull(userInfoService, "userInfoService cannot be null");
		this.authorizationCodeTokenExchanger = authorizationCodeTokenExchanger;
		this.userInfoService = userInfoService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		AuthorizationCodeAuthenticationToken authorizationCodeAuthentication =
				(AuthorizationCodeAuthenticationToken) authentication;

		TokenResponseAttributes tokenResponse =
				this.authorizationCodeTokenExchanger.exchange(authorizationCodeAuthentication);

		AccessToken accessToken = new AccessToken(tokenResponse.getAccessTokenType(),
				tokenResponse.getAccessToken(), tokenResponse.getIssuedAt(),
				tokenResponse.getExpiresAt(), tokenResponse.getScopes());
		OAuth2AuthenticationToken accessTokenAuthentication = new OAuth2AuthenticationToken(
				authorizationCodeAuthentication.getClientRegistration(), accessToken);
		accessTokenAuthentication.setDetails(authorizationCodeAuthentication.getDetails());

		OAuth2User user = this.userInfoService.loadUser(accessTokenAuthentication);

		Collection<? extends GrantedAuthority> authorities =
				this.authoritiesMapper.mapAuthorities(user.getAuthorities());

		OAuth2AuthenticationToken authenticationResult = new OAuth2AuthenticationToken(user, authorities,
				accessTokenAuthentication.getClientRegistration(), accessTokenAuthentication.getAccessToken());
		authenticationResult.setDetails(accessTokenAuthentication.getDetails());

		return authenticationResult;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return AuthorizationCodeAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public final void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		Assert.notNull(authoritiesMapper, "authoritiesMapper cannot be null");
		this.authoritiesMapper = authoritiesMapper;
	}
}
