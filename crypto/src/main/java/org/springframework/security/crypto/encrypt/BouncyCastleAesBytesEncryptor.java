/*
 * Copyright 2012-2016 the original author or authors.
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
package org.springframework.security.crypto.encrypt;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;

import static org.springframework.security.crypto.util.EncodingUtils.concatenate;
import static org.springframework.security.crypto.util.EncodingUtils.subArray;

/**
 * An Encryptor equivalent to {@link AesBytesEncryptor} that uses BouncyCastle instead of
 * JCE. Only CBC is supported. The algorithm is equivalent to "AES/CBC/PKCS5Padding".
 *
 * @author William Tran
 *
 */
public class BouncyCastleAesBytesEncryptor implements BytesEncryptor {
	private final PaddedBufferedBlockCipher blockCipher;
	private final CipherParameters secretKey;
	private final BytesKeyGenerator ivGenerator;

	public BouncyCastleAesBytesEncryptor(String password, CharSequence salt,
			BytesKeyGenerator ivGenerator) {
		this.blockCipher = new PaddedBufferedBlockCipher(
				new CBCBlockCipher(new AESFastEngine()), new PKCS7Padding());
		PBEParametersGenerator keyGenerator = new PKCS5S2ParametersGenerator();
		final byte[] pkcs12PasswordBytes = PBEParametersGenerator
				.PKCS5PasswordToUTF8Bytes(password.toCharArray());
		keyGenerator.init(pkcs12PasswordBytes, Hex.decode(salt), 1024);
		this.secretKey = keyGenerator.generateDerivedParameters(256);
		this.ivGenerator = ivGenerator;
	}

	public BouncyCastleAesBytesEncryptor(String password, CharSequence salt) {
		this(password, salt, null);
	}

	@Override
	public byte[] encrypt(byte[] bytes) {
		synchronized (this.blockCipher) {
			byte[] iv = null;
			CipherParameters key = this.secretKey;
			if (this.ivGenerator != null) {
				iv = this.ivGenerator.generateKey();
				key = new ParametersWithIV(this.secretKey, iv);
			}
			this.blockCipher.init(true, key);
			byte[] encrypted = process(bytes);
			return iv != null ? concatenate(iv, encrypted) : encrypted;
		}
	}

	@Override
	public byte[] decrypt(byte[] encryptedBytes) {
		synchronized (this.blockCipher) {
			byte[] iv = null;
			CipherParameters key = this.secretKey;
			if (this.ivGenerator != null) {
				iv = subArray(encryptedBytes, 0, this.ivGenerator.getKeyLength());
				key = new ParametersWithIV(this.secretKey, iv);
				encryptedBytes = subArray(encryptedBytes, this.ivGenerator.getKeyLength(),
						encryptedBytes.length);
			}
			this.blockCipher.init(false, key);
			return process(encryptedBytes);
		}
	}

	private byte[] process(byte[] bytes) {
		try {
			byte[] temp = new byte[this.blockCipher.getOutputSize(bytes.length)];
			int offset = this.blockCipher.processBytes(bytes, 0, bytes.length, temp, 0);
			int last = this.blockCipher.doFinal(temp, offset);
			final byte[] output = new byte[offset + last];
			System.arraycopy(temp, 0, output, 0, output.length);
			return output;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
