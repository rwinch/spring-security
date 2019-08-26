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

package boot.saml2.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.security.saml2.serviceprovider.provider.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.serviceprovider.provider.RelyingPartyRegistration;
import org.springframework.security.saml2.serviceprovider.provider.RelyingPartyRegistrationRepository;
import org.springframework.util.StringUtils;

import boot.saml2.config.OpenSamlKeyConverters.Saml2X509CredentialConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialUsage.ENCRYPTION;
import static org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialUsage.VERIFICATION;

@Configuration
@ConfigurationProperties(prefix = "spring.security.saml2.login")
@Import(OpenSamlKeyConverters.class)
public class Saml2SampleBootConfiguration {

	private List<SampleRelyingParty> relyingParties;

	@Bean
	@ConditionalOnMissingBean
	public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
		return new InMemoryRelyingPartyRegistrationRepository(getRelyingParties(relyingParties));
	}

	public void setRelyingParties(List<SampleRelyingParty> providers) {
		this.relyingParties = providers;
	}

	private List<RelyingPartyRegistration> getRelyingParties(List<SampleRelyingParty> sampleRelyingParties) {
		return sampleRelyingParties.stream()
				.map(
					p -> StringUtils.hasText(p.getLocalSpEntityIdTemplate()) ?
							new RelyingPartyRegistration(
								p.getEntityId(),
								p.getRegistrationId(),
								p.getWebSsoUrlAsURI(),
								p.getProviderCredentials(),
								p.getLocalSpEntityIdTemplate()
							) :
							new RelyingPartyRegistration(
								p.getEntityId(),
								p.getRegistrationId(),
								p.getWebSsoUrlAsURI(),
								p.getProviderCredentials()
							)
				)
				.collect(Collectors.toList());
	}

	public static class SampleRelyingParty {

		private String entityId;
		private List<Saml2X509Credential> signingCredentials = emptyList();
		private List<X509Certificate> verificationCredentials = emptyList();
		private String registrationId;
		private String webSsoUrl;
		private String localSpEntityIdTemplate;

		public String getEntityId() {
			return entityId;
		}

		public String getLocalSpEntityIdTemplate() {
			return localSpEntityIdTemplate;
		}

		public void setEntityId(String entityId) {
			this.entityId = entityId;
		}

		public List<Saml2X509Credential> getSigningCredentials() {
			return signingCredentials;
		}

		public void setSigningCredentials(List<StringX509Credential> credentials) {
			final Saml2X509CredentialConverter converter = new Saml2X509CredentialConverter();
			this.signingCredentials = credentials.stream().map(c -> converter.convert(c)).collect(Collectors.toList());
		}

		public void setVerificationCredentials(List<X509Certificate> credentials) {
			this.verificationCredentials = new LinkedList<>(credentials);
		}

		public List<X509Certificate> getVerificationCredentials() {
			return verificationCredentials;
		}

		public List<Saml2X509Credential> getProviderCredentials() {
			LinkedList<Saml2X509Credential> result = new LinkedList<>(getSigningCredentials());
			for (X509Certificate c : getVerificationCredentials()) {
				result.add(new Saml2X509Credential(null, c, ENCRYPTION, VERIFICATION));
			}
			return result;
		}

		public String getRegistrationId() {
			return registrationId;
		}

		public SampleRelyingParty setRegistrationId(String registrationId) {
			this.registrationId = registrationId;
			return this;
		}

		public String getWebSsoUrl() {
			return webSsoUrl;
		}

		public String getWebSsoUrlAsURI() {
			try {
				return new URI(webSsoUrl);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public SampleRelyingParty setWebSsoUrl(String webSsoUrl) {
			this.webSsoUrl = webSsoUrl;
			return this;
		}

		public void setLocalSpEntityIdTemplate(String localSpEntityIdTemplate) {
			this.localSpEntityIdTemplate = localSpEntityIdTemplate;
		}
	}

	public static class StringX509Credential {

		private String privateKey;
		private String passphrase;
		private String certificate;

		public String getPrivateKey() {
			return privateKey;
		}

		public void setPrivateKey(String privateKey) {
			this.privateKey = privateKey;
		}

		public String getPassphrase() {
			return passphrase;
		}

		public void setPassphrase(String passphrase) {
			this.passphrase = passphrase;
		}

		public String getCertificate() {
			return certificate;
		}

		public void setCertificate(String certificate) {
			this.certificate = certificate;
		}

	}

}
