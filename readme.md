# Project Documentation

## About project

The application is an implementation of a social network with the following functionality:

- **Authentication.** Users have the opportunity to register, login, logout, use third-party services sucha as Google, GitHub, etc. to create an account, enable two-factor authentication.
- **User Account.** Users have the opportunity to create and change their data, including password and email, which can be changed via email message, upload avatars. They must confirm email when changing it or creating an account.
- **Subscriptions, strikes.** Users can subscribe and send strikes at each other with an explanation of the reason. Users with a large number of strikes are banned.
- **Media.** Users can create posts, rate, comment on and read them. There is also a feed logic with subscribers posts.
- **Notifications.** Users receive emails to confirm their email, when changing their password, when creating a post by subscribers, etc.
- **Chats.** Users can communicate with each other through tet-a-tet chats.

## Microservices

1. **Eureka Server.** It is used as a service registry. It allows microservices to register themselves and discover other services within the system dynamically.
2. **API Gateway.** It acts as a single entry point, handling request routing, authentication, and response aggregation across multiple services.
3. **Auth Service.** It's responsible for authentication, authorization, OAuth2, 2FA, etc.
4. **User Service.** It contains the logic of creating and managing users, subscriptions, strikes, etc.
5. **Media Service.** It's responsible for the logic of working with posts, likes, dislikes, comments, etc.
6. **Chat Service.** It contains the logic of tet-a-tet chats and messages.
7. **Notification Service.** It contains the logic of sending messages of various subjects to the mail.

## Technologies

### Java 17

Java is a high-level, object-oriented programming language designed to be platform-independent, running on any system with a Java Virtual Machine (JVM). It is widely used for building enterprise-scale applications, mobile apps, web applications, and server-side software.

### Maven
Maven is a build automation and project management tool primarily used for Java projects. It simplifies the process of managing project dependencies, building, and deploying software by using a standardized project object model (POM) file.

### Spring Framework

Spring Framework is a comprehensive, modular framework for building enterprise-level Java applications. It provides a wide range of functionalities, including dependency injection, aspect-oriented programming, transaction management, and support for building web applications with Spring MVC.

### Spring Boot

Spring Boot is an extension of the Spring Framework that simplifies the process of building standalone, production-ready Spring applications. It provides pre-configured templates and embedded servers, eliminating much of the boilerplate configuration typically required in Spring applications.

### Spring Data

Spring Data is a part of the Spring Framework that simplifies data access and manipulation across various databases and data storage technologies. It provides a consistent and easy-to-use abstraction layer for database interactions, supporting both relational and non-relational databases.

### Spring Cloud

Spring Cloud provides tools for building cloud-native applications, offering features like service discovery, configuration management, and circuit breakers. It integrates with Spring Boot to simplify developing scalable microservices.

### Spring Security

Spring Security provides tools for securing applications, offering features like authentication, authorization, and protection against common vulnerabilities. It integrates seamlessly with Spring Boot to simplify implementing robust security for any application.

### Apache Kafka

Apache Kafka is a distributed streaming platform used for building real-time data pipelines and streaming applications. It provides high-throughput, low-latency messaging, and is designed to handle large volumes of data efficiently.

### Redis
Redis is an open-source, in-memory data store used as a database, cache, and message broker. It offers fast read/write operations and supports various data structures. Common use cases include caching, session storage, message queuing, and real-time analytics, making it ideal for high-performance applications.

### Websocket

WebSocket is a protocol for real-time, full-duplex communication between a client and server over a single TCP connection. It enables low-latency data exchange, ideal for applications like chat, gaming, and live updates.

### PostgreSQL

PostgreSQL is an advanced, open-source relational database management system (RDBMS). It is known for its robustness, extensibility, and support for SQL standards.

### Liquibase

Liquibase is an open-source database schema change management tool. It allows developers to track, version, and deploy database changes seamlessly across different environments.

### JWT (JSON Web Token)

JWT (JSON Web Token) is a compact and secure way to transmit information between two parties as a JSON object. In the context of authentication, JWT tokens are used to verify the identity of a user without requiring the server to store session data.

### Docker

Docker is a platform that allows you to package, deploy, and run applications using containers. Containers are lightweight and portable environments that ensure consistency across different systems.

### JIB

Jib is a tool for building Docker and OCI container images for Java applications. It simplifies the process by directly building images from your Java code without needing a Dockerfile.

### Testcontainers
Testcontainers is a Java library for integration testing that uses Docker containers. It allows developers to run tests against real service instances (like databases and message brokers) in isolated environments, ensuring clean and reliable test setups.