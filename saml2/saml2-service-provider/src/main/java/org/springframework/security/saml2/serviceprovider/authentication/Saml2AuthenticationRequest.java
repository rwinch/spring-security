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
import java.util.stream.Collectors;

import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.util.Assert;

public class Saml2AuthenticationRequest {
	private final String localSpEntityId;
	private final List<Saml2X509Credential> credentials;
	private String webSsoUri;

	public Saml2AuthenticationRequest(String localSpEntityId, String webSsoUri, List<Saml2X509Credential> credentials) {
		Assert.hasText(localSpEntityId, "localSpEntityId is required");
		Assert.hasText(localSpEntityId, "webSsoUri is required");
		this.localSpEntityId = localSpEntityId;
		this.webSsoUri = webSsoUri;
		this.credentials = credentials.stream()
				.filter(Saml2X509Credential::isSigningCredential)
				.collect(Collectors.toList());
		Assert.notEmpty(credentials, "at least one SIGNING credential must be present");
	}


	public String getLocalSpEntityId() {
		return localSpEntityId;
	}

	public String getWebSsoUri() {
		return webSsoUri;
	}

	public List<Saml2X509Credential> getCredentials() {
		return credentials;
	}
}
