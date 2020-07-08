package example;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;

public class X509Tests {
	@Test
	void run()  throws Exception {
		KeyStore truststore = KeyStore.getInstance("PKCS12");
		truststore.load(new FileInputStream("config/client.keystore"), "password".toCharArray());

		SSLContext sslContext = SSLContexts
				.custom()
				.loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
				.loadKeyMaterial(truststore, "password".toCharArray())
				.build();

		SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
				new DefaultHostnameVerifier());

		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("https", sslConnectionFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		HttpClientBuilder builder = HttpClientBuilder.create();
		builder.setSSLSocketFactory(sslConnectionFactory);
		builder.setConnectionManager(new PoolingHttpClientConnectionManager(registry));

		CloseableHttpClient httpClient = builder.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		requestFactory.setConnectTimeout(10000); // 10 seconds
		requestFactory.setReadTimeout(10000); // 10 seconds
		RestTemplate rest = new RestTemplate(requestFactory);

		String body = rest.getForObject("https://localhost:8443/", String.class);

		assertThat(body).isEqualTo("Hello Spring!");
	}
}
