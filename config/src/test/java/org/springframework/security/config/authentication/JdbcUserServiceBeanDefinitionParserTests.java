package org.springframework.security.config.authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.authentication.AbstractUserDetailsServiceBeanDefinitionParser;
import org.springframework.security.config.authentication.CachingUserDetailsService;
import org.springframework.security.config.authentication.JdbcUserServiceBeanDefinitionParser;
import org.springframework.security.config.util.InMemoryXmlApplicationContext;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.util.FieldUtils;
import org.w3c.dom.Element;

/**
 * @author Ben Alex
 * @author Luke Taylor
 */
public class JdbcUserServiceBeanDefinitionParserTests {
	private static String USER_CACHE_XML = "<b:bean id='userCache' class='org.springframework.security.authentication.dao.MockUserCache'/>";

	private static String DATA_SOURCE = "    <b:bean id='populator' class='org.springframework.security.config.DataSourcePopulator'>"
			+ "        <b:property name='dataSource' ref='dataSource'/>"
			+ "    </b:bean>"
			+

			"    <b:bean id='dataSource' class='org.springframework.security.TestDataSource'>"
			+ "        <b:constructor-arg value='jdbcnamespaces'/>" + "    </b:bean>";

	private InMemoryXmlApplicationContext appContext;

	@After
	public void closeAppContext() {
		if (appContext != null) {
			appContext.close();
		}
	}

	@Test
	public void beanNameIsCorrect() throws Exception {
		assertThat(JdbcUserDetailsManager.class.getName()).isEqualTo(
				new JdbcUserServiceBeanDefinitionParser()
						.getBeanClassName(mock(Element.class)));
	}

	@Test
	public void validUsernameIsFound() {
		setContext("<jdbc-user-service data-source-ref='dataSource'/>" + DATA_SOURCE);
		JdbcUserDetailsManager mgr = (JdbcUserDetailsManager) appContext
				.getBean(BeanIds.USER_DETAILS_SERVICE);
		assertThat(mgr.loadUserByUsername("rod")).isNotNull();
	}

	@Test
	public void beanIdIsParsedCorrectly() {
		setContext("<jdbc-user-service id='myUserService' data-source-ref='dataSource'/>"
				+ DATA_SOURCE);
		assertThat(appContext.getBean("myUserService") instanceof JdbcUserDetailsManager).isTrue();
	}

	@Test
	public void usernameAndAuthorityQueriesAreParsedCorrectly() throws Exception {
		String userQuery = "select username, password, true from users where username = ?";
		String authoritiesQuery = "select username, authority from authorities where username = ? and 1 = 1";
		setContext("<jdbc-user-service id='myUserService' "
				+ "data-source-ref='dataSource' " + "users-by-username-query='"
				+ userQuery + "' " + "authorities-by-username-query='" + authoritiesQuery
				+ "'/>" + DATA_SOURCE);
		JdbcUserDetailsManager mgr = (JdbcUserDetailsManager) appContext
				.getBean("myUserService");
		assertThat(FieldUtils.getFieldValue(mgr,"usersByUsernameQuery")).isEqualTo(userQuery);
		assertThat(FieldUtils.getFieldValue(mgr, "authoritiesByUsernameQuery")).isEqualTo(authoritiesQuery);
		assertThat(mgr.loadUserByUsername("rod") != null).isTrue();
	}

	@Test
	public void groupQueryIsParsedCorrectly() throws Exception {
		setContext("<jdbc-user-service id='myUserService' "
				+ "data-source-ref='dataSource' "
				+ "group-authorities-by-username-query='blah blah'/>" + DATA_SOURCE);
		JdbcUserDetailsManager mgr = (JdbcUserDetailsManager) appContext
				.getBean("myUserService");
		assertThat(FieldUtils.getFieldValue(mgr, "groupAuthoritiesByUsernameQuery")).isEqualTo("blah blah");
		assertThat((Boolean) FieldUtils.getFieldValue(mgr, "enableGroups")).isTrue();
	}

	@Test
	public void cacheRefIsparsedCorrectly() {
		setContext("<jdbc-user-service id='myUserService' cache-ref='userCache' data-source-ref='dataSource'/>"
				+ DATA_SOURCE + USER_CACHE_XML);
		CachingUserDetailsService cachingUserService = (CachingUserDetailsService) appContext
				.getBean("myUserService"
						+ AbstractUserDetailsServiceBeanDefinitionParser.CACHING_SUFFIX);
		assertThat(appContext.getBean("userCache")).isSameAs(cachingUserService.getUserCache());
		assertThat(cachingUserService.loadUserByUsername("rod")).isNotNull();
		assertThat(cachingUserService.loadUserByUsername("rod")).isNotNull();
	}

	@Test
	public void isSupportedByAuthenticationProviderElement() {
		setContext("<authentication-manager>" + "  <authentication-provider>"
				+ "    <jdbc-user-service data-source-ref='dataSource'/>"
				+ "  </authentication-provider>" + "</authentication-manager>"
				+ DATA_SOURCE);
		AuthenticationManager mgr = (AuthenticationManager) appContext
				.getBean(BeanIds.AUTHENTICATION_MANAGER);
		mgr.authenticate(new UsernamePasswordAuthenticationToken("rod", "koala"));
	}

	@Test
	public void cacheIsInjectedIntoAuthenticationProvider() {
		setContext("<authentication-manager>"
				+ "  <authentication-provider>"
				+ "    <jdbc-user-service cache-ref='userCache' data-source-ref='dataSource'/>"
				+ "  </authentication-provider>" + "</authentication-manager>"
				+ DATA_SOURCE + USER_CACHE_XML);
		ProviderManager mgr = (ProviderManager) appContext
				.getBean(BeanIds.AUTHENTICATION_MANAGER);
		DaoAuthenticationProvider provider = (DaoAuthenticationProvider) mgr
				.getProviders().get(0);
		assertThat(appContext.getBean("userCache")).isSameAs(provider.getUserCache());
		provider.authenticate(new UsernamePasswordAuthenticationToken("rod", "koala"));
		assertThat(provider
				.getUserCache().getUserFromCache("rod")).isNotNull().withFailMessage("Cache should contain user after authentication");
	}

	@Test
	public void rolePrefixIsUsedWhenSet() {
		setContext("<jdbc-user-service id='myUserService' role-prefix='PREFIX_' data-source-ref='dataSource'/>"
				+ DATA_SOURCE);
		JdbcUserDetailsManager mgr = (JdbcUserDetailsManager) appContext
				.getBean("myUserService");
		UserDetails rod = mgr.loadUserByUsername("rod");
		assertThat(AuthorityUtils.authorityListToSet(rod.getAuthorities()).contains(
				"PREFIX_ROLE_SUPERVISOR"));
	}

	private void setContext(String context) {
		appContext = new InMemoryXmlApplicationContext(context);
	}
}
