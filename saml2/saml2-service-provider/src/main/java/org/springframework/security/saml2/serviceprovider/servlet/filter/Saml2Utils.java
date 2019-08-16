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

package org.springframework.security.saml2.serviceprovider.servlet.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.serviceprovider.provider.Saml2RelyingPartyRegistration;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import org.apache.commons.codec.binary.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.zip.Deflater.DEFLATED;
import static org.springframework.security.web.util.UrlUtils.buildFullRequestUrl;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

/**
 * @since 5.2
 */
final class Saml2Utils {

	private static final char PATH_DELIMITER = '/';
	private static Base64 UNCHUNKED_ENCODER = new Base64(0, new byte[]{'\n'});

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

	static String getServiceProviderEntityId(Saml2RelyingPartyRegistration idp, HttpServletRequest request) {
		return resolveUrlTemplate(
				idp.getLocalEntityIdTemplate(),
				getApplicationUri(request),
				idp.getRemoteIdpEntityId(),
				idp.getAlias()
		);
	}

	static String resolveUrlTemplate(String template, String baseUrl, String entityId, String alias) {
		if (!StringUtils.hasText(template)) {
			return baseUrl;
		}

		Map<String, String> uriVariables = new HashMap<>();
		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(baseUrl)
				.replaceQuery(null)
				.fragment(null)
				.build();
		String scheme = uriComponents.getScheme();
		uriVariables.put("baseScheme", scheme == null ? "" : scheme);
		String host = uriComponents.getHost();
		uriVariables.put("baseHost", host == null ? "" : host);
		// following logic is based on HierarchicalUriComponents#toUriString()
		int port = uriComponents.getPort();
		uriVariables.put("basePort", port == -1 ? "" : ":" + port);
		String path = uriComponents.getPath();
		if (StringUtils.hasLength(path)) {
			if (path.charAt(0) != PATH_DELIMITER) {
				path = PATH_DELIMITER + path;
			}
		}
		uriVariables.put("basePath", path == null ? "" : path);
		uriVariables.put("baseUrl", uriComponents.toUriString());
		uriVariables.put("entityId", StringUtils.hasText(entityId) ? entityId : "");
		uriVariables.put("alias", StringUtils.hasText(alias) ? alias : "");

		return UriComponentsBuilder.fromUriString(template)
				.buildAndExpand(uriVariables)
				.toUriString();
	}

	static String getApplicationUri(HttpServletRequest request) {
		UriComponents uriComponents = fromHttpUrl(buildFullRequestUrl(request))
				.replacePath(request.getContextPath())
				.replaceQuery(null)
				.fragment(null)
				.build();
		return uriComponents.toUriString();
	}
}
