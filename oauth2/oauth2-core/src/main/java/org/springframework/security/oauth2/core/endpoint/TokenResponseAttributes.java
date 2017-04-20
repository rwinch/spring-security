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
package org.springframework.security.oauth2.core.endpoint;

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
	private final AccessToken accessToken;

	public TokenResponseAttributes(String tokenValue, AccessToken.TokenType tokenType, long expiresIn) {
		this(tokenValue, tokenType, expiresIn, Collections.emptySet());
	}

	public TokenResponseAttributes(String tokenValue, AccessToken.TokenType tokenType, long expiresIn, Set<String> scopes) {
		this(tokenValue, tokenType, expiresIn, scopes, Collections.emptyMap());
	}

	public TokenResponseAttributes(String tokenValue, AccessToken.TokenType tokenType, long expiresIn,
									Set<String> scopes, Map<String,Object> additionalParameters) {

		Assert.isTrue(expiresIn >= 0, "expiresIn must be a positive number");
		Instant issuedAt = Instant.now();
		this.accessToken = new AccessToken(tokenType, tokenValue, issuedAt,
			issuedAt.plusSeconds(expiresIn), scopes, additionalParameters);
	}

	public String getTokenValue() {
		return this.accessToken.getTokenValue();
	}

	public AccessToken.TokenType getTokenType() {
		return this.accessToken.getTokenType();
	}

	public Instant getIssuedAt() {
		return this.accessToken.getIssuedAt();
	}

	public Instant getExpiresAt() {
		return this.accessToken.getExpiresAt();
	}

	public Set<String> getScopes() {
		return this.accessToken.getScopes();
	}

	public Map<String, Object> getAdditionalParameters() {
		return this.accessToken.getAdditionalParameters();
	}
}
