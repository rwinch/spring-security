package org.springframework.security.web.session.redis;

import static org.fest.assertions.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.web.session.MapSession;
import org.springframework.security.web.session.Session;
import org.springframework.security.web.session.SessionRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RedisSessionRepositoryTests {

    @Autowired
    private SessionRepository repository;

    @Test
    public void saves() {
        MapSession toSave = new MapSession();
        toSave.setAttribute("a", "b");

        repository.save(toSave);

        Session session = repository.getSession(toSave.getId());

        assertThat(session.getId()).isEqualTo(toSave.getId());
        assertThat(session.getAttributeNames()).isEqualTo(session.getAttributeNames());
        assertThat(session.getAttribute("a")).isEqualTo(toSave.getAttribute("a"));
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
            Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Session.class);
            template.setHashValueSerializer(serializer);
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