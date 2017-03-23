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
package org.springframework.security.oauth2.client.registration;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @author Joe Grandja
 */
public class ClientRegistration {
	private String clientId;
	private String clientSecret;
	private ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.HEADER;
	private AuthorizationGrantType authorizedGrantType;
	private URI redirectUri;
	private Set<String> scopes = Collections.emptySet();
	private ProviderDetails providerDetails = new ProviderDetails();
	private String clientName;
	private String clientAlias;

	protected ClientRegistration() {
	}

	public String getClientId() {
		return this.clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}

	public ClientAuthenticationMethod getClientAuthenticationMethod() {
		return this.clientAuthenticationMethod;
	}

	public AuthorizationGrantType getAuthorizedGrantType() {
		return this.authorizedGrantType;
	}

	public URI getRedirectUri() {
		return this.redirectUri;
	}

	public Set<String> getScopes() {
		return this.scopes;
	}

	public ProviderDetails getProviderDetails() {
		return this.providerDetails;
	}

	public String getClientName() {
		return this.clientName;
	}

	public String getClientAlias() {
		return this.clientAlias;
	}

	public class ProviderDetails {
		private URI authorizationUri;
		private URI tokenUri;
		private URI userInfoUri;
		private boolean openIdProvider;

		protected ProviderDetails() {
		}

		public URI getAuthorizationUri() {
			return this.authorizationUri;
		}

		public URI getTokenUri() {
			return this.tokenUri;
		}

		public URI getUserInfoUri() {
			return this.userInfoUri;
		}

		public boolean isOpenIdProvider() {
			return this.openIdProvider;
		}
	}

	public static class Builder {
		private final ClientRegistration clientRegistration;

		public Builder(String clientId) {
			this.clientRegistration = new ClientRegistration();
			this.clientRegistration.clientId = clientId;
		}

		public Builder(ClientRegistrationProperties clientRegistrationProperties) {
			this(clientRegistrationProperties.getClientId());
			this.clientSecret(clientRegistrationProperties.getClientSecret());
			this.clientAuthenticationMethod(clientRegistrationProperties.getClientAuthenticationMethod());
			this.authorizedGrantType(clientRegistrationProperties.getAuthorizedGrantType());
			this.redirectUri(clientRegistrationProperties.getRedirectUri());
			if (!CollectionUtils.isEmpty(clientRegistrationProperties.getScopes())) {
				this.scopes(clientRegistrationProperties.getScopes().stream().toArray(String[]::new));
			}
			this.authorizationUri(clientRegistrationProperties.getAuthorizationUri());
			this.tokenUri(clientRegistrationProperties.getTokenUri());
			this.userInfoUri(clientRegistrationProperties.getUserInfoUri());
			if (clientRegistrationProperties.isOpenIdProvider()) {
				this.openIdProvider();
			}
			this.clientName(clientRegistrationProperties.getClientName());
			this.clientAlias(clientRegistrationProperties.getClientAlias());
		}

		public Builder clientSecret(String clientSecret) {
			this.clientRegistration.clientSecret = clientSecret;
			return this;
		}

		public Builder clientAuthenticationMethod(ClientAuthenticationMethod clientAuthenticationMethod) {
			this.clientRegistration.clientAuthenticationMethod = clientAuthenticationMethod;
			return this;
		}

		public Builder authorizedGrantType(AuthorizationGrantType authorizedGrantType) {
			this.clientRegistration.authorizedGrantType = authorizedGrantType;
			return this;
		}

		public Builder redirectUri(String redirectUri) {
			this.clientRegistration.redirectUri = this.toURI(redirectUri);
			return this;
		}

		public Builder scopes(String... scopes) {
			if (scopes != null && scopes.length > 0) {
				this.clientRegistration.scopes = Collections.unmodifiableSet(
						new LinkedHashSet<>(Arrays.asList(scopes)));
			}
			return this;
		}

		public Builder authorizationUri(String authorizationUri) {
			this.clientRegistration.providerDetails.authorizationUri = this.toURI(authorizationUri);
			return this;
		}

		public Builder tokenUri(String tokenUri) {
			this.clientRegistration.providerDetails.tokenUri = this.toURI(tokenUri);
			return this;
		}

		public Builder userInfoUri(String userInfoUri) {
			this.clientRegistration.providerDetails.userInfoUri = this.toURI(userInfoUri);
			return this;
		}

		public Builder openIdProvider() {
			this.clientRegistration.providerDetails.openIdProvider = true;
			return this;
		}

		public Builder clientName(String clientName) {
			this.clientRegistration.clientName = clientName;
			return this;
		}

		public Builder clientAlias(String clientAlias) {
			this.clientRegistration.clientAlias = clientAlias;
			return this;
		}

		public ClientRegistration build() {
			if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(this.clientRegistration.getAuthorizedGrantType())) {
				throw new UnsupportedOperationException((this.clientRegistration.getAuthorizedGrantType() != null ?
						this.clientRegistration.getAuthorizedGrantType().value() :
						"null") + " authorization grant type is currently not supported");
			}
			this.validateClientWithAuthorizationCodeGrantType();
			return this.clientRegistration;
		}

		private void validateClientWithAuthorizationCodeGrantType() {
			Assert.hasText(this.clientRegistration.clientId, "clientId cannot be empty");
			Assert.hasText(this.clientRegistration.clientSecret, "clientSecret cannot be empty");
			Assert.notNull(this.clientRegistration.clientAuthenticationMethod, "clientAuthenticationMethod cannot be null");
			Assert.notNull(this.clientRegistration.redirectUri, "redirectUri cannot be null");
			Assert.notEmpty(this.clientRegistration.scopes, "scopes cannot be empty");
			Assert.notNull(this.clientRegistration.providerDetails.authorizationUri, "authorizationUri cannot be null");
			Assert.notNull(this.clientRegistration.providerDetails.tokenUri, "tokenUri cannot be null");
			Assert.notNull(this.clientRegistration.providerDetails.userInfoUri, "userInfoUri cannot be null");
			Assert.hasText(this.clientRegistration.clientName, "clientName cannot be empty");
			Assert.hasText(this.clientRegistration.clientAlias, "clientAlias cannot be empty");
		}

		private URI toURI(String uriStr) {
			try {
				return new URI(uriStr);
			} catch (Exception ex) {
				throw new IllegalArgumentException("An error occurred parsing URI: " + uriStr, ex);
			}
		}
	}
}
