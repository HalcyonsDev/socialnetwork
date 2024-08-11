package com.halcyon.authservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic createUserTopic() {
        return TopicBuilder.name("createUser").partitions(3).build();
    }

    @Bean
    public NewTopic resetPasswordTopic() {
        return TopicBuilder.name("resetPassword").partitions(3).build();
    }

    @Bean
    public NewTopic verifyTopic() {
        return TopicBuilder.name("verify").build();
    }

    @Bean
    public NewTopic saveSecretTopic() {
        return TopicBuilder.name("saveSecret").partitions(3).build();
    }

    @Bean
    public NewTopic use2FATopic() {
        return TopicBuilder.name("use2FA").partitions(3).build();
    }

    @Bean
    public NewTopic sendVerificationMessageTopic() {
        return TopicBuilder.name("sendVerificationMessage").partitions(3).build();
    }

    @Bean
    public NewTopic sendForgotPasswordMessageTopic() {
        return TopicBuilder.name("sendForgotPasswordMessage").partitions(3).build();
    }

    @Bean
    public NewTopic sendNewEmailVerificationMessageTopic() {
        return TopicBuilder.name("sendNewEmailVerificationMessage").partitions(3).build();
    }

    @Bean
    public NewTopic sendChangeEmailTopic() {
        return TopicBuilder.name("changeEmail").partitions(3).build();
    }
}
