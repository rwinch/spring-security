package org.springframework.security.web.session;

/**
 * @author Rob Winch
 */
public interface SessionRepository {
    void save(MapSession session);

    MapSession getSession(String id);

    void delete(String id);
}
