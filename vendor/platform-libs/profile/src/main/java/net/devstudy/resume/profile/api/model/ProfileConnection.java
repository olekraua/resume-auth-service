package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "profile_connection")
public class ProfileConnection extends AbstractEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @JsonIgnore
    private Profile requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    @JsonIgnore
    private Profile addressee;

    @Column(name = "pair_key", nullable = false, length = 64, unique = true)
    private String pairKey;

    @Column(nullable = false, length = 16)
    @Convert(converter = ProfileConnectionStatus.PersistJPAConverter.class)
    private ProfileConnectionStatus status;

    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "responded")
    private Instant responded;

    public ProfileConnection() {
    }

    public ProfileConnection(Profile requester, Profile addressee, ProfileConnectionStatus status, Instant created) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = status;
        this.created = created;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Profile getRequester() {
        return requester;
    }

    public void setRequester(Profile requester) {
        this.requester = requester;
    }

    public Profile getAddressee() {
        return addressee;
    }

    public void setAddressee(Profile addressee) {
        this.addressee = addressee;
    }

    public String getPairKey() {
        return pairKey;
    }

    public void setPairKey(String pairKey) {
        this.pairKey = pairKey;
    }

    public ProfileConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileConnectionStatus status) {
        this.status = status;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getResponded() {
        return responded;
    }

    public void setResponded(Instant responded) {
        this.responded = responded;
    }

    @PrePersist
    private void ensurePairKey() {
        if (pairKey != null || requester == null || addressee == null) {
            return;
        }
        Long requesterId = requester.getId();
        Long addresseeId = addressee.getId();
        if (requesterId == null || addresseeId == null) {
            return;
        }
        long min = Math.min(requesterId, addresseeId);
        long max = Math.max(requesterId, addresseeId);
        this.pairKey = min + ":" + max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ProfileConnection other)) return false;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return String.format("ProfileConnection[id=%s, requester=%s, addressee=%s, status=%s]",
                id,
                requester == null ? null : requester.getId(),
                addressee == null ? null : addressee.getId(),
                status);
    }
}
