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

package org.springframework.security.samples;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import javax.crypto.SecretKey;

import org.springframework.security.saml2.Saml2Exception;

import org.apache.commons.codec.binary.Base64;
import org.apache.xml.security.algorithms.JCEMapper;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.zip.Deflater.DEFLATED;
import static org.opensaml.security.crypto.KeySupport.generateKey;

final class Saml2TestUtils {

	private static Base64 UNCHUNKED_ENCODER = new Base64(0, new byte[] { '\n' });

	static String encode(byte[] b) {
		return UNCHUNKED_ENCODER.encodeToString(b);
	}

	static byte[] decode(String s) {
		return UNCHUNKED_ENCODER.decode(s);
	}

	static byte[] deflate(String s) {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DeflaterOutputStream deflater = new DeflaterOutputStream(b, new Deflater(DEFLATED, true));
			deflater.write(s.getBytes(UTF_8));
			deflater.finish();
			return b.toByteArray();
		}
		catch (IOException e) {
			throw new Saml2Exception("Unable to deflate string", e);
		}
	}

	static String inflate(byte[] b) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InflaterOutputStream iout = new InflaterOutputStream(out, new Inflater(true));
			iout.write(b);
			iout.finish();
			return new String(out.toByteArray(), UTF_8);
		}
		catch (IOException e) {
			throw new Saml2Exception("Unable to inflate string", e);
		}
	}

	static EncryptedAssertion encryptAssertion(Assertion assertion, X509Certificate certificate) {
		Encrypter encrypter = getEncrypter(certificate);
		try {
			Encrypter.KeyPlacement keyPlacement = Encrypter.KeyPlacement.valueOf("PEER");
			encrypter.setKeyPlacement(keyPlacement);
			return encrypter.encrypt(assertion);
		}
		catch (EncryptionException e) {
			throw new Saml2Exception("Unable to encrypt assertion.", e);
		}
	}

	static EncryptedID encryptNameId(NameID nameID, X509Certificate certificate) {
		Encrypter encrypter = getEncrypter(certificate);
		try {
			Encrypter.KeyPlacement keyPlacement = Encrypter.KeyPlacement.valueOf("PEER");
			encrypter.setKeyPlacement(keyPlacement);
			return encrypter.encrypt(nameID);
		}
		catch (EncryptionException e) {
			throw new Saml2Exception("Unable to encrypt nameID.", e);
		}
	}

	static Encrypter getEncrypter(X509Certificate certificate) {
		Credential credential = CredentialSupport.getSimpleCredential(certificate, null);
		final String dataAlgorithm = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";
		final String keyAlgorithm = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
		SecretKey secretKey = generateKeyFromURI(dataAlgorithm);
		BasicCredential dataCredential = new BasicCredential(secretKey);
		DataEncryptionParameters dataEncryptionParameters = new DataEncryptionParameters();
		dataEncryptionParameters.setEncryptionCredential(dataCredential);
		dataEncryptionParameters.setAlgorithm(dataAlgorithm);

		KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
		keyEncryptionParameters.setEncryptionCredential(credential);
		keyEncryptionParameters.setAlgorithm(keyAlgorithm);

		Encrypter encrypter = new Encrypter(dataEncryptionParameters, asList(keyEncryptionParameters));

		return encrypter;
	}

	static SecretKey generateKeyFromURI(String algoURI) {
		try {
			String jceAlgorithmName = JCEMapper.getJCEKeyAlgorithmFromURI(algoURI);
			int keyLength = JCEMapper.getKeyLengthFromURI(algoURI);
			return generateKey(jceAlgorithmName, keyLength, null);
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new Saml2Exception(e);
		}
	}

}
