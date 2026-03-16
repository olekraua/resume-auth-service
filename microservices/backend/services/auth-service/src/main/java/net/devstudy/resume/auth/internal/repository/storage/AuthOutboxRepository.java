package net.devstudy.resume.auth.internal.repository.storage;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.devstudy.resume.auth.internal.entity.AuthOutboxEvent;

public interface AuthOutboxRepository extends JpaRepository<AuthOutboxEvent, Long> {

    @Query(value = """
            select *
            from auth_outbox
            where status in ('NEW', 'ERROR')
              and available_at <= :now
              and attempts < :maxAttempts
            order by id
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<AuthOutboxEvent> lockNextBatch(@Param("now") Instant now,
            @Param("limit") int limit,
            @Param("maxAttempts") int maxAttempts);
}
