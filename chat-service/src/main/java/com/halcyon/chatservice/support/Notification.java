package com.halcyon.chatservice.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long messageId;
    private Long senderId;
    private String senderName;
}
