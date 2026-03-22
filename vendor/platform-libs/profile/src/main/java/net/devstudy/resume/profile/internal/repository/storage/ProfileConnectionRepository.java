package net.devstudy.resume.profile.internal.repository.storage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import net.devstudy.resume.profile.api.model.ProfileConnection;
import net.devstudy.resume.profile.api.model.ProfileConnectionStatus;

public interface ProfileConnectionRepository extends JpaRepository<ProfileConnection, Long> {

    Optional<ProfileConnection> findByPairKey(String pairKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ProfileConnection c where c.pairKey = :pairKey")
    Optional<ProfileConnection> findByPairKeyForUpdate(@Param("pairKey") String pairKey);

    @Query("""
            select c from ProfileConnection c
            where c.addressee.id = :profileId and c.status = :status
            order by c.created desc
            """)
    List<ProfileConnection> findIncomingByStatus(@Param("profileId") Long profileId,
            @Param("status") ProfileConnectionStatus status);

    @Query("""
            select c from ProfileConnection c
            where c.requester.id = :profileId and c.status = :status
            order by c.created desc
            """)
    List<ProfileConnection> findOutgoingByStatus(@Param("profileId") Long profileId,
            @Param("status") ProfileConnectionStatus status);

    @Query("""
            select c from ProfileConnection c
            where (c.requester.id = :profileId or c.addressee.id = :profileId)
              and c.status = :status
            order by c.responded desc
            """)
    List<ProfileConnection> findAcceptedByProfile(@Param("profileId") Long profileId,
            @Param("status") ProfileConnectionStatus status);

    long deleteByRequesterIdOrAddresseeId(Long requesterId, Long addresseeId);
}
