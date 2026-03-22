package net.devstudy.resume.messaging.internal.repository.storage;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.messaging.api.model.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedDesc(Long conversationId);

    Page<Message> findByConversationIdOrderByCreatedDesc(Long conversationId, Pageable pageable);

    Page<Message> findByConversationIdAndIdLessThanOrderByCreatedDesc(Long conversationId, Long beforeId,
            Pageable pageable);

    long countByConversationIdAndSenderIdNot(Long conversationId, Long senderId);

    long countByConversationIdAndIdGreaterThanAndSenderIdNot(Long conversationId, Long lastReadMessageId,
            Long senderId);
}
