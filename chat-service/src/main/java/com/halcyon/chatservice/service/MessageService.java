package com.halcyon.chatservice.service;

import com.halcyon.chatservice.dto.CreateMessageDto;
import com.halcyon.chatservice.dto.UpdateMessageDto;
import com.halcyon.chatservice.exception.MessageForbiddenException;
import com.halcyon.chatservice.exception.MessageNotFoundException;
import com.halcyon.chatservice.model.Message;
import com.halcyon.chatservice.repository.MessageRepository;
import com.halcyon.chatservice.support.MessageStatus;
import com.halcyon.chatservice.support.Notification;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.clients.user.UserResponse;
import com.halcyon.jwtlibrary.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class MessageService {
    @Value("${private.secret}")
    private String privateSecret;

    private final MessageRepository messageRepository;
    private final AuthProvider authProvider;
    private final UserClient userClient;
    private final SimpMessagingTemplate messagingTemplate;

    public Message create(CreateMessageDto dto) {
        PrivateUserResponse sender = userClient.getByEmail(authProvider.getSubject(), privateSecret);

        if (sender.getId() == dto.getRecipientId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't send a message to yourself.");
        }

        UserResponse recipient = userClient.getById(dto.getRecipientId());

        areCorrectUsers(sender, recipient);

        Message message = save(Message.builder()
                .content(dto.getContent())
                .senderId(sender.getId())
                .recipientId(recipient.getId())
                .status(MessageStatus.RECEIVED)
                .build());

        Notification notification = new Notification(message.getId(), sender.getId(), sender.getUsername());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipient.getId()), "/queue/messages",
                notification
        );

        return message;
    }

    public Message updateContent(UpdateMessageDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        Message message = findById(dto.getMessageId());
        if (message.getSenderId() == user.getId()) {
            throw new MessageForbiddenException("You don't have the rights to update message with this id.");
        }

        message.setContent(dto.getContent());
        message.setChanged(true);
        return save(message);
    }

    public Message findById(long messageId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, "You are banned.");

        Message message = messageRepository.findById(messageId)
                .orElseThrow(MessageNotFoundException::new);

        if (message.getSenderId() != user.getId() && message.getRecipientId() == user.getId()) {
            throw new MessageForbiddenException("You don't have the rights to get message with this id.");
        }

        return message;
    }

    public long countNewMessages(long recipientId) {
        PrivateUserResponse sender = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(sender, "You are banned.");

        return messageRepository.countBySenderIdAndRecipientIdAndStatus(recipientId, sender.getId(), MessageStatus.RECEIVED);
    }

    public Page<Message> findSentMeMessages(long recipientId, int offset, int limit) {
        PrivateUserResponse sender = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(sender, "You are banned.");

        Page<Message> messages = messageRepository.findAllBySenderIdAndRecipientId(recipientId, sender.getId(),
                PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt")));

        if (!messages.isEmpty()) {
            messages.stream()
                    .peek(message -> message.setStatus(MessageStatus.DELIVERED))
                    .forEach(this::save);
        }

        return messages;
    }

    public Page<Message> findMyMessages(long recipientId, int offset, int limit) {
        PrivateUserResponse sender = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(sender, "You are banned.");

        return messageRepository.findAllBySenderIdAndRecipientId(sender.getId(), recipientId,
                PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private Message save(Message message) {
        return messageRepository.save(message);
    }

    private void areCorrectUsers(PrivateUserResponse owner, UserResponse recipient) {
        isUserBanned(owner, "You are banned.");
        isUserVerified(owner, "You are not verified. Please confirm your email.");

        isUserBanned(recipient, "Recipient is banned.");
        isUserVerified(recipient, "Recipient is not verified.");
    }
}
