package com.halcyon.chatservice.repository;

import com.halcyon.chatservice.model.Message;
import com.halcyon.chatservice.support.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    long countBySenderIdAndRecipientIdAndStatus(Long senderId, Long recipientId, MessageStatus status);

    Page<Message> findAllBySenderIdAndRecipientId(long senderId, long recipientId, Pageable pageable);
}
