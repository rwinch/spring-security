package org.springframework.security.web.session.redis;

import static org.fest.assertions.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.session.Session;
import org.springframework.security.web.session.SessionRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RedisSessionRepositoryTests {

    @Autowired
    private SessionRepository repository;

    @Test
    public void saves() {
        Session toSave = repository.createSession();
        toSave.setAttribute("a", "b");
        Authentication toSaveToken = new UsernamePasswordAuthenticationToken("user","password", AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContext toSaveContext = SecurityContextHolder.createEmptyContext();
        toSaveContext.setAuthentication(toSaveToken);
        toSave.setAttribute("SPRING_SECURITY_CONTEXT", toSaveContext);

        repository.save(toSave);

        Session session = repository.getSession(toSave.getId());

        assertThat(session.getId()).isEqualTo(toSave.getId());
        assertThat(session.getAttributeNames()).isEqualTo(session.getAttributeNames());
        assertThat(session.getAttribute("a")).isEqualTo(toSave.getAttribute("a"));

        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");

        repository.delete(toSave.getId());

        assertThat(repository.getSession(toSave.getId())).isNull();
    }

    @Configuration
    static class Config {
        @Bean
        public JedisConnectionFactory connectionFactory() {
            return new JedisConnectionFactory();
        }

        @Bean
        public RedisTemplate<String,Session> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Session> template = new RedisTemplate<String, Session>();
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setConnectionFactory(connectionFactory);
            return template;
        }

        @Bean
        public RedisSessionRepository sessionRepository(RedisTemplate<String, Session> redisTemplate) {
            return new RedisSessionRepository(redisTemplate);
        }
    }
}