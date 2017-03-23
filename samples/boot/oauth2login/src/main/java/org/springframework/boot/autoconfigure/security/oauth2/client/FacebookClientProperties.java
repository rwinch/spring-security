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
package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistrationProperties;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Joe Grandja
 */
@ConfigurationProperties(prefix = "security.oauth2.client.facebook")
public class FacebookClientProperties extends ClientRegistrationProperties {
	public static final String CLIENT_NAME = "Facebook";
	public static final String CLIENT_ALIAS = "facebook";
	public static final String AUTHORIZATION_URI = "https://www.facebook.com/v2.8/dialog/oauth";
	public static final String TOKEN_URI = "https://graph.facebook.com/v2.8/oauth/access_token";
	public static final String USER_INFO_URI = "https://graph.facebook.com/me";
	public static final Set<String> SCOPES = new LinkedHashSet<>(Arrays.asList("public_profile", "email"));

	public FacebookClientProperties() {
		// Apply defaults
		this.setClientAuthenticationMethod(ClientAuthenticationMethod.FORM);
		this.setAuthorizedGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
		this.setScopes(SCOPES);
		this.setAuthorizationUri(AUTHORIZATION_URI);
		this.setTokenUri(TOKEN_URI);
		this.setUserInfoUri(USER_INFO_URI);
		this.setOpenIdProvider(false);
		this.setClientName(CLIENT_NAME);
		this.setClientAlias(CLIENT_ALIAS);
	}
}
