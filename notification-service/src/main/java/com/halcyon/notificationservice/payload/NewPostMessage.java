package com.halcyon.notificationservice.payload;

import com.halcyon.clients.subscription.SubscriptionResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewPostMessage {
    private Long postId;
    private List<SubscriptionResponse> subscribers;
}
