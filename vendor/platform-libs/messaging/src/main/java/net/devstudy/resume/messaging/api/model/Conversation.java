package net.devstudy.resume.messaging.api.model;

import java.io.Serial;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "conversation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conversation_pair", columnNames = "pair_key")
        })
public class Conversation extends AbstractEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    @Convert(converter = ConversationType.PersistJPAConverter.class)
    private ConversationType type = ConversationType.DIRECT;

    @Column(name = "pair_key", length = 64, unique = true)
    private String pairKey;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created", nullable = false)
    private Instant created;

    public Conversation() {
    }

    public Conversation(ConversationType type, String pairKey, Instant created) {
        this.type = type;
        this.pairKey = pairKey;
        this.created = created;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public String getPairKey() {
        return pairKey;
    }

    public void setPairKey(String pairKey) {
        this.pairKey = pairKey;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }
}
