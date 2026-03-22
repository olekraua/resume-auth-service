package net.devstudy.resume.messaging.internal.repository.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.messaging.api.model.MessageAttachment;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    List<MessageAttachment> findByMessageIdIn(Collection<Long> messageIds);

    List<MessageAttachment> findByConversationIdAndUploaderIdAndMessageIdIsNull(Long conversationId, Long uploaderId);

    Optional<MessageAttachment> findByIdAndConversationId(Long id, Long conversationId);
}
