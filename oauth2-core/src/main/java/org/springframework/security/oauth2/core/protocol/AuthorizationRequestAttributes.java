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
package org.springframework.security.oauth2.core.protocol;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ResponseType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Joe Grandja
 */
public final class AuthorizationRequestAttributes implements Serializable {
	private URI authorizeUri;
	private AuthorizationGrantType authorizationGrantType;
	private ResponseType responseType;
	private String clientId;
	private URI redirectUri;
	private Set<String> scopes;
	private String state;

	private AuthorizationRequestAttributes() {
	}

	public URI getAuthorizeUri() {
		return this.authorizeUri;
	}

	public AuthorizationGrantType getGrantType() {
		return this.authorizationGrantType;
	}

	public ResponseType getResponseType() {
		return this.responseType;
	}

	public String getClientId() {
		return this.clientId;
	}

	public URI getRedirectUri() {
		return this.redirectUri;
	}

	public Set<String> getScopes() {
		return this.scopes;
	}

	public String getState() {
		return this.state;
	}

	public static Builder authorizationCode(String clientId, URI authorizeUri, URI redirectUri) {
		return new Builder()
			.clientId(clientId)
			.authorizeUri(authorizeUri)
			.redirectUri(redirectUri)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.responseType(ResponseType.CODE);
	}

	public static class Builder {
		private final AuthorizationRequestAttributes authorizationRequest;

		public Builder() {
			this.authorizationRequest = new AuthorizationRequestAttributes();
		}

		public Builder authorizeUri(URI authorizeUri) {
			Assert.notNull(authorizeUri, "authorizeUri cannot be null");
			this.authorizationRequest.authorizeUri = authorizeUri;
			return this;
		}

		public Builder authorizationGrantType(AuthorizationGrantType authorizationGrantType) {
			Assert.notNull(authorizationGrantType, "authorizationGrantType cannot be null");
			this.authorizationRequest.authorizationGrantType = authorizationGrantType;
			return this;
		}

		public Builder responseType(ResponseType responseType) {
			Assert.notNull(responseType, "responseType cannot be null");
			this.authorizationRequest.responseType = responseType;
			return this;
		}

		public Builder clientId(String clientId) {
			Assert.hasText(clientId, "clientId cannot be empty");
			this.authorizationRequest.clientId = clientId;
			return this;
		}

		public Builder redirectUri(URI redirectUri) {
			Assert.notNull(redirectUri, "redirectUri cannot be null");
			this.authorizationRequest.redirectUri = redirectUri;
			return this;
		}

		public Builder scopes(Set<String> scopes) {
			this.authorizationRequest.scopes = Collections.unmodifiableSet(
				CollectionUtils.isEmpty(scopes) ? Collections.emptySet() : new LinkedHashSet<>(scopes));
			return this;
		}

		public Builder state(String state) {
			this.authorizationRequest.state = state;
			return this;
		}

		public AuthorizationRequestAttributes build() {
			return this.authorizationRequest;
		}
	}
}
