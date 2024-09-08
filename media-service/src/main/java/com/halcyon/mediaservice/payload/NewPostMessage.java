package com.halcyon.mediaservice.payload;

import com.halcyon.clients.subscription.SubscriptionResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NewPostMessage {
    private Long postId;
    private List<SubscriptionResponse> subscribers;
}
