package net.devstudy.resume.auth.internal.repository.storage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.auth.internal.entity.AuthUser;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByUid(String uid);

    boolean existsByUid(String uid);
}
