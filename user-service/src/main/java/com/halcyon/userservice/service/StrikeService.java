package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.CreateStrikeDto;
import com.halcyon.userservice.exception.StrikeAlreadyExistsException;
import com.halcyon.userservice.model.Strike;
import com.halcyon.userservice.model.User;
import com.halcyon.userservice.repository.StrikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.halcyon.userservice.util.UserUtil.isUserBanned;
import static com.halcyon.userservice.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class StrikeService {
    private final StrikeRepository strikeRepository;
    private final UserService userService;
    private final AuthProvider authProvider;

    private static final String BANNED_OWNER_MESSAGE = "You are banned.";
    private static final String UNVERIFIED_OWNER_MESSAGE = "You are not verified. Please confirm your email.";

    public Strike create(CreateStrikeDto dto) {
        User owner = userService.findByEmail(authProvider.getSubject());
        isUserVerified(owner, UNVERIFIED_OWNER_MESSAGE);
        isUserBanned(owner, BANNED_OWNER_MESSAGE);

        User target = userService.findByEmail(dto.getTargetEmail());
        isUserBanned(target, "This user is already banned.");

        if (strikeRepository.existsByOwnerAndTarget(owner, target)) {
            throw new StrikeAlreadyExistsException();
        }

        if (target.getStrikes().size() == 19) {
            target = userService.ban(target);
        }

        Strike strike = new Strike(dto.getCause(), owner, target);
        return strikeRepository.save(strike);
    }

    public List<Strike> getSentStrikes() {
        User owner = userService.findByEmail(authProvider.getSubject());
        isUserBanned(owner, BANNED_OWNER_MESSAGE);
        isUserVerified(owner, UNVERIFIED_OWNER_MESSAGE);

        return strikeRepository.findAllByOwner(owner);
    }

    public List<Strike> getSentMeStrikes() {
        User target = userService.findByEmail(authProvider.getSubject());
        isUserBanned(target, BANNED_OWNER_MESSAGE);

        return strikeRepository.findAllByTarget(target);
    }
}
