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

package org.springframework.security.samples.config;

import java.io.CharArrayReader;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.springframework.util.Assert;

import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.opensaml.security.x509.X509Support;

import static java.util.Optional.ofNullable;

class BCKeyUtils {

	static {
		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	static X509Certificate certificate(String source) {
		try {
			return X509Support.decodeCertificate(source);
		}
		catch (CertificateException e) {
			throw new IllegalArgumentException(e);
		}
	}

	static PrivateKey privateKey(String key, String passphrase) {
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
