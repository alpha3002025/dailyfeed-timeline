package click.dailyfeed.timeline.domain.statistics.service;

import click.dailyfeed.code.domain.timeline.statistics.TimelineStatisticsDto;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentMongoAggregation;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostLikeMongoAggregation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class TimelineStatisticsService {
    private final PostLikeMongoAggregation postLikeMongoAggregation;
    private final CommentMongoAggregation commentMongoAggregation;

    public TimelineStatisticsDto.PostItemCounts getPostDetailCounts(Long postId) {
        Long commentCount = commentMongoAggregation.countCommentsByPostPk(postId);
        Long postLikeCount = postLikeMongoAggregation.countLikesByPostPk(postId);

        return TimelineStatisticsDto.PostItemCounts.builder()
                .commentCount(commentCount)
                .likeCount(postLikeCount)
                .build();
    }
}
