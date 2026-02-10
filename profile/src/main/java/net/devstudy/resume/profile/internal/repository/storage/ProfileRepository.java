package net.devstudy.resume.profile.internal.repository.storage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.devstudy.resume.profile.api.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUid(String uid);

    Optional<Profile> findWithAllByUid(String uid);

    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByPhone(String phone);

    int countByUid(String uid);

    Page<Profile> findAllByCompletedTrue(Pageable pageable);

    List<Profile> findByCompletedFalseAndCreatedBefore(Timestamp oldDate);

    @Query("""
            select p from Profile p
            where (
               lower(p.firstName) like lower(concat('%', :query, '%'))
               or lower(p.lastName) like lower(concat('%', :query, '%'))
               or lower(p.objective) like lower(concat('%', :query, '%'))
               or lower(p.summary) like lower(concat('%', :query, '%'))
            )
            """)
    Page<Profile> search(@Param("query") String query, Pageable pageable);
}
