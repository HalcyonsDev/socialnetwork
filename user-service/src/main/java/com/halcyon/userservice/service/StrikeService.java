package com.halcyon.userservice.service;

import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.userservice.dto.StrikeRequestDto;
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

    public Strike create(StrikeRequestDto dto) {
        User owner = userService.findByEmail(authProvider.getSubject());
        User target = userService.findByEmail(dto.getTargetEmail());

        isUserBanned(owner, "You are banned.");
        isUserBanned(target, "This user is already banned.");
        isUserVerified(owner, "You are not verified. Please confirm your email.");
        isUserVerified(target, "Target user is not verified.");

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

        isUserBanned(owner, "You are banned.");
        isUserVerified(owner, "You are not verified. Please confirm your email.");

        return strikeRepository.findAllByOwner(owner);
    }

    public List<Strike> getSentMeStrikes() {
        User target = userService.findByEmail(authProvider.getSubject());

        isUserBanned(target, "You are banned.");
        isUserVerified(target, "You are not verified. Please confirm your email.");

        return strikeRepository.findAllByTarget(target);
    }
}
