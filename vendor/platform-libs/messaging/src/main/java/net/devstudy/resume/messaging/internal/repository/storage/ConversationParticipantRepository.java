package net.devstudy.resume.messaging.internal.repository.storage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.messaging.api.model.ConversationParticipant;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByProfileId(Long profileId);

    List<ConversationParticipant> findByConversationId(Long conversationId);

    List<ConversationParticipant> findByConversationIdIn(Iterable<Long> conversationIds);

    Optional<ConversationParticipant> findByConversationIdAndProfileId(Long conversationId, Long profileId);
}
