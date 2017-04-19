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
package org.springframework.security.oauth2.core.protocol.message;

import org.springframework.security.oauth2.core.AccessToken;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Joe Grandja
 */
public final class TokenResponseAttributes {
	private final String accessToken;
	private final AccessToken.TokenType accessTokenType;
	private final Instant issuedAt;
	private final Instant expiresAt;
	private final Set<String> scopes;
	private final Map<String,String> additionalParameters;

	public TokenResponseAttributes(String accessToken, AccessToken.TokenType accessTokenType, long expiresIn) {
		this(accessToken, accessTokenType, expiresIn, Collections.emptySet());
	}

	public TokenResponseAttributes(String accessToken, AccessToken.TokenType accessTokenType, long expiresIn, Set<String> scopes) {
		this(accessToken, accessTokenType, expiresIn, scopes, Collections.emptyMap());
	}

	public TokenResponseAttributes(String accessToken, AccessToken.TokenType accessTokenType, long expiresIn,
									Set<String> scopes, Map<String,String> additionalParameters) {

		Assert.notNull(accessToken, "accessToken cannot be null");
		Assert.notNull(accessTokenType, "accessTokenType cannot be null");
		Assert.isTrue(expiresIn >= 0, "expiresIn must be a positive number");
		this.accessToken = accessToken;
		this.accessTokenType = accessTokenType;
		this.issuedAt = Instant.now();
		this.expiresAt = this.issuedAt.plusSeconds(expiresIn);
		this.scopes = Collections.unmodifiableSet(scopes != null ? scopes : Collections.emptySet());
		this.additionalParameters = Collections.unmodifiableMap(additionalParameters != null ?
				additionalParameters : Collections.emptyMap());
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public AccessToken.TokenType getAccessTokenType() {
		return this.accessTokenType;
	}

	public Instant getIssuedAt() {
		return this.issuedAt;
	}

	public Instant getExpiresAt() {
		return this.expiresAt;
	}

	public Set<String> getScopes() {
		return this.scopes;
	}

	public Map<String, String> getAdditionalParameters() {
		return this.additionalParameters;
	}
}
