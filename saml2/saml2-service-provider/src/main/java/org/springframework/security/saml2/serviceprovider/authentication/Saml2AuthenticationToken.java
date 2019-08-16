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

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.saml2.credentials.Saml2X509Credential;

public class Saml2AuthenticationToken extends AbstractAuthenticationToken {

	private final String saml2Response;
	private final String recipientUri;
	private String idpEntityId;
	private String localSpEntityId;
	private List<Saml2X509Credential> credentials;

	public Saml2AuthenticationToken(String saml2Response,
									String recipientUri,
									String idpEntityId,
									String localSpEntityId,
									List<Saml2X509Credential> credentials) {
		super(null);
		this.saml2Response = saml2Response;
		this.recipientUri = recipientUri;
		this.idpEntityId = idpEntityId;
		this.localSpEntityId = localSpEntityId;
		this.credentials = credentials;
	}

	@Override
	public Object getCredentials() {
		return getSaml2Response();
	}

	@Override
	public Object getPrincipal() {
		return null;
	}

	public String getSaml2Response() {
		return this.saml2Response;
	}

	public String getRecipientUri() {
		return this.recipientUri;
	}

	public String getLocalSpEntityId() {
		return this.localSpEntityId;
	}

	public List<Saml2X509Credential> getX509Credentials() {
		return this.credentials;
	}

	@Override
	public boolean isAuthenticated() {
		return false;
	}

	@Override
	public void setAuthenticated(boolean authenticated) {
		throw new IllegalArgumentException();
	}

	public String getIdpEntityId() {
		return this.idpEntityId;
	}
}
