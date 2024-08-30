package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.CreateStrikeDto;
import com.halcyon.userservice.exception.BannedUserException;
import com.halcyon.userservice.exception.StrikeAlreadyExistsException;
import com.halcyon.userservice.exception.UnverifiedUserException;
import com.halcyon.userservice.model.Strike;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.StrikeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrikeServiceTests {
    @Mock
    private AuthProvider authProvider;

    @Mock
    private UserService userService;

    @Mock
    private StrikeRepository strikeRepository;

    @InjectMocks
    private StrikeService strikeService;

    private static final String STRIKE_ALREADY_EXISTS_MESSAGE = "You have already struck to this user.";
    private static final String BANNED_OWNER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_OWNER_MESSAGE = "You are not verified. Please confirm your email.";
    private static final String BANNED_TARGET_MESSAGE = "This user is already banned.";

    private static User owner;
    private static User target;

    @BeforeAll
    static void beforeAll() {
        owner = User.builder()
                .username("owner_username")
                .email("owner_user@gmail.com")
                .avatarPath("owner_avatar_path")
                .isVerified(true)
                .authProvider("google")
                .strikes(new ArrayList<>())
                .build();

        target = User.builder()
                .username("target_username")
                .email("target_user@gmail.com")
                .avatarPath("target_avatar_path")
                .isVerified(true)
                .authProvider("google")
                .strikes(new ArrayList<>())
                .build();
    }

    @Test
    void create() {
        mockCreating();

        Strike returnedStrike = strikeService.create(getCreateStrikeDto());
        Strike strike = getStrike();

        assertThat(returnedStrike.getOwner()).isEqualTo(strike.getOwner());
        assertThat(returnedStrike.getTarget()).isEqualTo(strike.getTarget());
        assertThat(returnedStrike.getCause()).isEqualTo(strike.getCause());
    }

    private void mockCreating() {
        mockGettingUsers();
        when(strikeRepository.existsByOwnerAndTarget(owner, target)).thenReturn(false);

        Strike strike = getStrike();
        when(strikeRepository.save(strike)).thenReturn(strike);
    }

    private void mockGettingUsers() {
        mockGettingOwner();
        when(userService.findByEmail(target.getEmail())).thenReturn(target);
    }

    private void mockGettingOwner() {
        when(authProvider.getSubject()).thenReturn(owner.getEmail());
        when(userService.findByEmail(owner.getEmail())).thenReturn(owner);
    }

    private Strike getStrike() {
        return new Strike("spam", owner, target);
    }

    private CreateStrikeDto getCreateStrikeDto() {
        return new CreateStrikeDto(target.getEmail(), "spam");
    }

    @Test
    void create_banUser() {
        setStrikesToTarget();
        mockCreating();

        strikeService.create(getCreateStrikeDto());
        verify(userService).ban(target);

        target.setStrikes(new ArrayList<>());
    }

    private void setStrikesToTarget() {
        List<Strike> strikes = new ArrayList<>();

        for (int i = 0; i < 19; i++) {
            strikes.add(getStrike());
        }

        target.setStrikes(strikes);
    }

    @Test
    void create_alreadyExists() {
        mockGettingUsers();
        when(strikeRepository.existsByOwnerAndTarget(owner, target)).thenReturn(true);

        CreateStrikeDto createStrikeDto = getCreateStrikeDto();
        StrikeAlreadyExistsException strikeAlreadyExistsException = assertThrows(StrikeAlreadyExistsException.class,
                () -> strikeService.create(createStrikeDto));
        assertThat(strikeAlreadyExistsException.getMessage()).isEqualTo(STRIKE_ALREADY_EXISTS_MESSAGE);
    }

    @Test
    void create_bannedOwner() {
        owner.setBanned(true);
        mockGettingOwner();

        CreateStrikeDto createStrikeDto = getCreateStrikeDto();
        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> strikeService.create(createStrikeDto));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_OWNER_MESSAGE);

        owner.setBanned(false);
    }

    @Test
    void create_unverifiedOwner() {
        owner.setVerified(false);
        mockGettingOwner();

        CreateStrikeDto createStrikeDto = getCreateStrikeDto();
        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> strikeService.create(createStrikeDto));
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_OWNER_MESSAGE);

        owner.setVerified(true);
    }

    @Test
    void create_bannedTarget() {
        target.setBanned(true);
        mockGettingUsers();

        CreateStrikeDto createStrikeDto = getCreateStrikeDto();
        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> strikeService.create(createStrikeDto));
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_TARGET_MESSAGE);

        target.setBanned(false);
    }

    @Test
    void getSentStrikes() {
        mockGettingOwner();
        strikeService.getSentStrikes();
        verify(strikeRepository).findAllByOwner(owner);
    }

    @Test
    void getSentStrikes_bannedOwner() {
        owner.setBanned(true);
        mockGettingOwner();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> strikeService.getSentStrikes());
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_OWNER_MESSAGE);

        owner.setBanned(false);
    }

    @Test
    void getSentStrikes_unverifiedOwner() {
        owner.setVerified(false);
        mockGettingOwner();

        UnverifiedUserException unverifiedUserException = assertThrows(UnverifiedUserException.class,
                () -> strikeService.getSentStrikes());
        assertThat(unverifiedUserException.getMessage()).isEqualTo(UNVERIFIED_OWNER_MESSAGE);

        owner.setVerified(true);
    }

    @Test
    void getSentMeStrikes() {
        mockGettingOwner();
        strikeService.getSentMeStrikes();
        verify(strikeRepository).findAllByTarget(owner);
    }

    @Test
    void getSentMeStrikes_bannedOwner() {
        owner.setBanned(true);
        mockGettingOwner();

        BannedUserException bannedUserException = assertThrows(BannedUserException.class,
                () -> strikeService.getSentMeStrikes());
        assertThat(bannedUserException.getMessage()).isEqualTo(BANNED_OWNER_MESSAGE);

        owner.setBanned(false);
    }
}
