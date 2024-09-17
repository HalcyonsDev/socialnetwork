package com.halcyon.mediaservice.service;

import com.halcyon.clients.exception.BannedUserException;
import com.halcyon.clients.exception.UnverifiedUserException;
import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreateChildCommentDto;
import com.halcyon.mediaservice.dto.CreateCommentDto;
import com.halcyon.mediaservice.exception.CommentForbiddenException;
import com.halcyon.mediaservice.model.Comment;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.repository.CommentRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTests {
    @Mock
    private AuthProvider authProvider;

    @Mock
    private UserClient userClient;

    @Mock
    private PostService postService;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private static final String BANNED_USER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_USER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String SUCCESSFULLY_DELETED_MESSAGE = "The comment was successfully deleted";
    private static final String FORBIDDEN_COMMENT_MESSAGE = "You don't have the rights to change this comment.";

    private static PrivateUserResponse user;
    private static Post post;

    @BeforeAll
    static void beforeAll() {
        user = PrivateUserResponse.builder()
                .id(1)
                .username("test_username")
                .email("test_user@gmail.com")
                .isVerified(true)
                .build();

        post = new Post("TestTitle", "TestContent", user.getId());
        post.setId(1L);
    }

    @Test
    void createParentComment() {
        CreateCommentDto createCommentDto = getCreateCommentDto();
        mockCreatingParentComment(createCommentDto.getContent());

        Comment comment = commentService.create(createCommentDto);

        assertThat(comment).isNotNull();
        assertThat(comment.getContent()).isEqualTo(createCommentDto.getContent());
        assertThat(comment.getAuthorId()).isEqualTo(user.getId());
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getParent()).isNull();
    }

    private CreateCommentDto getCreateCommentDto() {
        return new CreateCommentDto(post.getId(), "TestContent");
    }

    private void mockCreatingParentComment(String content) {
        mockGettingUser();
        when(postService.findById(post.getId())).thenReturn(post);

        Comment comment = getParentComment(content);
        when(commentRepository.save(comment)).thenReturn(comment);
    }

    private void mockGettingUser() {
        when(authProvider.getSubject()).thenReturn(user.getEmail());
        when(userClient.getByEmail(user.getEmail(), null)).thenReturn(user);
    }

    private Comment getParentComment(String content) {
        return Comment.builder()
                .content(content)
                .authorId(user.getId())
                .post(post)
                .build();
    }

    @Test
    void createParentComment_bannedUser() {
        mockGettingBannedUser();

        CreateCommentDto createCommentDto = getCreateCommentDto();
        isValidBannedUserException(() -> commentService.create(createCommentDto));

        user.setBanned(false);
    }

    private void mockGettingBannedUser() {
        user.setBanned(true);
        mockGettingUser();
    }

    private void isValidBannedUserException(Executable executable) {
        BannedUserException bannedUserException = assertThrows(BannedUserException.class, executable);
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_USER_MESSAGE);
    }

    @Test
    void createParentComment_unverifiedUser() {
        mockGettingUnverifiedUser();

        CreateCommentDto createCommentDto = getCreateCommentDto();
        isValidUnverifiedUserException(() -> commentService.create(createCommentDto));

        user.setVerified(true);
    }

    private void mockGettingUnverifiedUser() {
        user.setVerified(false);
        mockGettingUser();
    }

    private void isValidUnverifiedUserException(Executable executable) {
        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class, executable);
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_USER_MESSAGE);
    }

    @Test
    void createChildComment() {
        CreateChildCommentDto createChildCommentDto = getCreateChildCommentDto();
        Comment parent = getParentComment("ParentCommentContent");
        parent.setId(1L);

        mockCreatingChildComment(parent, createChildCommentDto.getContent());

        Comment child = commentService.create(createChildCommentDto);

        assertThat(child).isNotNull();
        assertThat(child.getContent()).isEqualTo(createChildCommentDto.getContent());
        assertThat(child.getAuthorId()).isEqualTo(user.getId());
        assertThat(child.getPost()).isEqualTo(post);
        assertThat(child.getParent()).isEqualTo(parent);
    }

    public CreateChildCommentDto getCreateChildCommentDto() {
        return new CreateChildCommentDto(1L, "ChildCommentContent");
    }

    private void mockCreatingChildComment(Comment parent, String content) {
        mockGettingUser();
        mockFindingComment(parent);

        Comment child = getChildComment(content, parent);
        when(commentRepository.save(child)).thenReturn(child);
    }

    private void mockFindingComment(Comment comment) {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
    }

    public Comment getChildComment(String content, Comment parent) {
        return Comment.builder()
                .content(content)
                .authorId(user.getId())
                .post(post)
                .parent(parent)
                .build();
    }

    @Test
    void createChildComment_bannedUser() {
        mockGettingBannedUser();

        CreateChildCommentDto createChildCommentDto = getCreateChildCommentDto();
        isValidBannedUserException(() -> commentService.create(createChildCommentDto));

        user.setBanned(false);
    }

    @Test
    void createChildComment_unverifiedUser() {
        mockGettingUnverifiedUser();

        CreateChildCommentDto createChildCommentDto = getCreateChildCommentDto();
        isValidUnverifiedUserException(() -> commentService.create(createChildCommentDto));

        user.setVerified(true);
    }

    @Test
    void delete() {
        Comment comment = getParentComment("TestContent");
        mockDeleting(comment);

        String response = commentService.delete(1L);
        verify(commentRepository).delete(comment);

        assertThat(response).isEqualTo(SUCCESSFULLY_DELETED_MESSAGE);
    }

    private void mockDeleting(Comment comment) {
        mockGettingUser();
        mockFindingComment(comment);
    }

    @Test
    void delete_forbidden() {
        Comment comment = getParentComment("TestContent");
        comment.setAuthorId(2L);
        mockDeleting(comment);

        CommentForbiddenException commentForbiddenException = assertThrows(CommentForbiddenException.class,
                () -> commentService.delete(1L));
        assertThat(commentForbiddenException.getMessage()).isEqualTo(FORBIDDEN_COMMENT_MESSAGE);
    }

    @Test
    void delete_bannedUser() {
        mockGettingBannedUser();
        isValidBannedUserException(() -> commentService.delete(1L));
        user.setBanned(false);
    }

    @Test
    void getById() {
        Comment comment = getParentComment("TestContent");
        mockGettingUser();
        mockFindingComment(comment);

        Comment returnedComment = commentService.getById(1L);

        assertThat(returnedComment).isNotNull();
        assertThat(returnedComment.getAuthorId()).isEqualTo(user.getId());
        assertThat(returnedComment.getPost()).isEqualTo(post);
    }

    @Test
    void getById_bannedUser() {
        mockGettingBannedUser();
        isValidBannedUserException(() -> commentService.getById(1L));
        user.setBanned(false);
    }

    @Test
    void findAllByPost() {
        when(postService.getById(post.getId())).thenReturn(post);

        commentService.findAllByPost(post.getId());
        verify(commentRepository).findAllByPost(post);
    }
}
