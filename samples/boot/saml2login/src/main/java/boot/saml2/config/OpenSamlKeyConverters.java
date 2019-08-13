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

import java.io.CharArrayReader;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import boot.saml2.config.Saml2SampleBootConfiguration.StringX509Credential;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.opensaml.security.x509.X509Support;

import static java.util.Optional.ofNullable;
import static org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialUsage.DECRYPTION;
import static org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialUsage.SIGNING;

@Configuration
class OpenSamlKeyConverters {

	@Component
	@ConfigurationPropertiesBinding
	static class X509CertificateConverter implements Converter<String, X509Certificate> {

		@Override
		public X509Certificate convert(String source) {
			try {
				return X509Support.decodeCertificate(source);
			}
			catch (CertificateException e) {
				throw new IllegalArgumentException(e);
			}
		}

	}

	@Component
	@ConfigurationPropertiesBinding
	static class Saml2X509CredentialConverter implements Converter<StringX509Credential, Saml2X509Credential> {

		@Override
		public Saml2X509Credential convert(StringX509Credential source) {
			final PrivateKey privateKey = getPrivateKey(source.getPrivateKey(), source.getPassphrase());
			final X509Certificate certificate = new X509CertificateConverter().convert(source.getCertificate());
			return new Saml2X509Credential(privateKey, certificate, SIGNING, DECRYPTION);
		}

		private PrivateKey getPrivateKey(String key, String passphrase) {
			final String password = ofNullable(passphrase).orElse("");
			Assert.hasText(key, "private key cannot be empty");
			try {
				PEMParser parser = new PEMParser(new CharArrayReader(key.toCharArray()));
				Object obj = parser.readObject();
				parser.close();
				JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
				KeyPair kp;
				if (obj == null) {
					throw new IllegalArgumentException("Unable to decode PEM key:" + key);
				}
				else if (obj instanceof PEMEncryptedKeyPair) {
					// Encrypted key - we will use provided password
					PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) obj;
					PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
					kp = converter.getKeyPair(ckp.decryptKeyPair(decProv));
				}
				else {
					// Unencrypted key - no password needed
					PEMKeyPair ukp = (PEMKeyPair) obj;
					kp = converter.getKeyPair(ukp);
				}

				return kp.getPrivate();
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}

	}

}
