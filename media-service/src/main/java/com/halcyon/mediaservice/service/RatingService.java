package com.halcyon.mediaservice.service;

import com.halcyon.clients.user.PrivateUserResponse;
import com.halcyon.clients.user.UserClient;
import com.halcyon.jwtlibrary.AuthProvider;
import com.halcyon.mediaservice.dto.CreateRatingDto;
import com.halcyon.mediaservice.dto.PostRatingsResponse;
import com.halcyon.mediaservice.dto.UpdateRatingDto;
import com.halcyon.mediaservice.exception.RatingAlreadyExistsException;
import com.halcyon.mediaservice.exception.RatingForbiddenException;
import com.halcyon.mediaservice.exception.RatingNotFoundException;
import com.halcyon.mediaservice.model.Post;
import com.halcyon.mediaservice.model.Rating;
import com.halcyon.mediaservice.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static com.halcyon.clients.util.UserUtil.isUserBanned;
import static com.halcyon.clients.util.UserUtil.isUserVerified;

@Service
@RequiredArgsConstructor
public class RatingService {
    @Value("${private.secret}")
    private String privateSecret;

    private final RatingRepository ratingRepository;
    private final AuthProvider authProvider;
    private final UserClient userClient;
    private final PostService postService;

    private static final String BANNED_USER_MESSAGE = "You are banned.";

    public Rating create(CreateRatingDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);
        isUserVerified(user, "You are not verified. Please confirm your email.");

        Post post = postService.findById(dto.getPostId());

        if (ratingRepository.existsByOwnerIdAndPost(user.getId(), post)) {
            throw new RatingAlreadyExistsException();
        }

        Rating rating = new Rating(dto.getIsLike(), user.getId(), post);
        rating = ratingRepository.save(rating);

        if (rating.isLike()) {
            post.setLikesCount(post.getLikesCount() + 1);
        } else {
            post.setDislikesCount(post.getDislikesCount() + 1);
        }
        postService.save(post);

        return rating;
    }

    public Rating getById(long ratingId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        return findById(ratingId);
    }

    public Rating findById(long ratingId) {
        return ratingRepository.findById(ratingId)
                .orElseThrow(RatingNotFoundException::new);
    }

    public String delete(long ratingId) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        Rating rating = findById(ratingId);

        if (rating.getOwnerId() != user.getId()) {
            throw new RatingForbiddenException();
        }

        ratingRepository.delete(rating);

        Post post = rating.getPost();
        if (rating.isLike()) {
            post.setLikesCount(post.getLikesCount() - 1);
        } else {
            post.setDislikesCount(post.getDislikesCount() - 1);
        }
        postService.save(post);

        return "The rating was successfully deleted.";
    }

    public Rating changeType(UpdateRatingDto dto) {
        PrivateUserResponse user = userClient.getByEmail(authProvider.getSubject(), privateSecret);
        isUserBanned(user, BANNED_USER_MESSAGE);

        Rating rating = findById(dto.getRatingId());
        boolean lastType = rating.isLike();

        if (rating.getOwnerId() != user.getId()) {
            throw new RatingForbiddenException();
        }

        rating.setLike(dto.getIsLike());
        rating = ratingRepository.save(rating);

        setCountOfRatingsInPost(rating, lastType);

        return rating;
    }

    private void setCountOfRatingsInPost(Rating rating, boolean lastType) {
        Post post = rating.getPost();
        if (lastType && !rating.isLike()) {
            post.setLikesCount(post.getLikesCount() - 1);
            post.setDislikesCount(post.getDislikesCount() + 1);
            postService.save(post);
        } else if (!lastType && rating.isLike()) {
            post.setDislikesCount(post.getDislikesCount() - 1);
            post.setLikesCount(post.getLikesCount() + 1);
            postService.save(post);
        }
    }

    public PostRatingsResponse getRatingsCountInPost(long postId) {
        Post post = postService.getById(postId);
        return new PostRatingsResponse(postId, post.getLikesCount(), post.getDislikesCount());
    }

    public Page<Rating> findRatingsInPost(long postId, boolean isLike, int offset, int limit) {
        Post post = postService.getById(postId);
        return ratingRepository.findAllByPostAndIsLike(post, isLike,
                PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
