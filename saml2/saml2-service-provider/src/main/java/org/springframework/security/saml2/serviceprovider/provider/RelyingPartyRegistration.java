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

package org.springframework.security.saml2.serviceprovider.provider;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialUsage;

import static java.util.Arrays.asList;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

/**
 * Represents a configured service provider and a remote identity provider pair.
 * @since 5.2
 */
public class RelyingPartyRegistration {

	private final String registrationId;
	private final String remoteIdpEntityId;
	private final URI idpWebSsoUrl;
	private final List<Saml2X509Credential> credentials;
	private final String localEntityIdTemplate;

	public RelyingPartyRegistration(String idpEntityId, String registrationId, URI idpWebSsoUri, List<Saml2X509Credential> credentials) {
		this(idpEntityId, registrationId, idpWebSsoUri, credentials, "{baseUrl}/saml2/service-provider-metadata/{registrationId}");
	}

	public RelyingPartyRegistration(String idpEntityId, String registrationId, URI idpWebSsoUri, List<Saml2X509Credential> credentials, String localEntityIdTemplate) {
		hasText(idpEntityId, "idpEntityId is required");
		hasText(registrationId, "registrationId is required");
		hasText(localEntityIdTemplate, "localEntityIdTemplate is required");
		notEmpty(credentials, "credentials are required");
		notNull(idpWebSsoUri, "idpWebSsoUri is required");
		for (Saml2X509Credential c : credentials) {
			notNull(c, "credentials cannot contain null elements");
		}
		this.remoteIdpEntityId = idpEntityId;
		this.registrationId = registrationId;
		this.credentials = credentials;
		this.idpWebSsoUrl = idpWebSsoUri;
		this.localEntityIdTemplate = localEntityIdTemplate;
	}

	public String getRemoteIdpEntityId() {
		return this.remoteIdpEntityId;
	}

	public String getRegistrationId() {
		return this.registrationId;
	}

	public URI getIdpWebSsoUrl() {
		return this.idpWebSsoUrl;
	}

	public String getLocalEntityIdTemplate() {
		return this.localEntityIdTemplate;
	}

	public List<Saml2X509Credential> getCredentialsForUsage(Saml2X509CredentialUsage... types) {
		if (types == null || types.length == 0) {
			return this.credentials;
		}
		Set<Saml2X509CredentialUsage> typeset = new HashSet<>(asList(types));
		List<Saml2X509Credential> result = new LinkedList<>();
		for (Saml2X509Credential c : this.credentials) {
			if (containsCredentialForTypes(c.getSaml2X509CredentialUsages(), typeset)) {
				result.add(c);
			}
		}
		return result;
	}

	private boolean containsCredentialForTypes(Set<Saml2X509CredentialUsage> existing, Set<Saml2X509CredentialUsage> requested) {
		for (Saml2X509CredentialUsage u : requested) {
			if (existing.contains(u)) {
				return true;
			}
		}
		return false;
	}


}
