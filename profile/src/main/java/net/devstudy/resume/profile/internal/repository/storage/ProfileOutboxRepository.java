package net.devstudy.resume.profile.internal.repository.storage;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.devstudy.resume.profile.api.model.ProfileOutboxEvent;

public interface ProfileOutboxRepository extends JpaRepository<ProfileOutboxEvent, Long> {

    @Query(value = """
            select *
            from profile_outbox
            where status in ('NEW', 'ERROR')
              and available_at <= :now
              and attempts < :maxAttempts
            order by id
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<ProfileOutboxEvent> lockNextBatch(@Param("now") Instant now,
            @Param("limit") int limit,
            @Param("maxAttempts") int maxAttempts);
}
