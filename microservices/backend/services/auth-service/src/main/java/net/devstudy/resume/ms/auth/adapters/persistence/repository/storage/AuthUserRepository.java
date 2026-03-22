package net.devstudy.resume.ms.auth.adapters.persistence.repository.storage;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import net.devstudy.resume.ms.auth.domain.entity.AuthUser;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByUid(String uid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select au from AuthUser au where au.uid = :uid")
    Optional<AuthUser> findByUidForUpdate(@Param("uid") String uid);

    boolean existsByUid(String uid);
}
