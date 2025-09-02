package click.dailyfeed.timeline.domain.following.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController("/api/timeline/followings")
public class FollowingController {
    @GetMapping("/latest/posts")
    public DailyfeedPageResponse<FollowDto.FollowActivityDto> getLatestPosts(
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return null;
    }
}
