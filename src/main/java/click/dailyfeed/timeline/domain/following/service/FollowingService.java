package click.dailyfeed.timeline.domain.following.service;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.feign.domain.member.MemberFeignHelper;
import click.dailyfeed.timeline.domain.following.mapper.FollowingMapper;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostActivityMongoRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FollowingService {
    private final FollowingMapper followingMapper;
    private final MemberFeignHelper memberFeignHelper;
    private final PostActivityMongoRepository postActivityMongoRepository;

    public DailyfeedPageResponse<PostDto.PostActivityEvent> findLatestPostsActivity(
            String token, HttpServletResponse response, Pageable pageable
    ){
        MemberDto.Member member = memberFeignHelper.getMember(token, response);
        Page<PostActivity> pageResult = postActivityMongoRepository.findByFollowingIdAndActivityTypeNotEquals(
                member.getId(), PostActivityType.DELETE, pageable
        );

        List<PostDto.PostActivityEvent> pageEvent = pageResult.getContent().stream().map(followingMapper::toEvent).toList();

        DailyfeedPage<PostDto.PostActivityEvent> page = followingMapper.fromMongoPage(pageResult, pageEvent);


        return DailyfeedPageResponse.<PostDto.PostActivityEvent>builder()
                .content(page).reason("SUCCESS").ok("Y").statusCode("200")
                .build();
    }

    // todo read post created/updated
    public DailyfeedPageResponse<FollowDto.FollowActivity> findLatestPostsActivity(
            Long userId, Pageable pageable
    ){
        return null;
    }
}
