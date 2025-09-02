package click.dailyfeed.timeline.domain.following.service;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FollowingService {
    private final MemberFeignHelper memberFeignHelper;


    // todo read post created/updated
    public DailyfeedPageResponse<FollowDto.FollowActivityDto> findLatestPostsActivity(
            String token, HttpServletResponse response, Pageable pageable
    ){
        MemberDto.Member member = memberFeignHelper.getMember(token, response);
        return null;
    }

    // todo read post created/updated
    public DailyfeedPageResponse<FollowDto.FollowActivityDto> findLatestPostsActivity(
            Long userId, Pageable pageable
    ){
        return null;
    }
}
