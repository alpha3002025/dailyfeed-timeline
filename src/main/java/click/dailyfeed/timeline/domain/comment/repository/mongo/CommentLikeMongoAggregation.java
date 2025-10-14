package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.projection.CommentLikeCountProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentLikeMongoAggregation {
    private final MongoTemplate mongoTemplate;

    /**
     * 여러 댓글들의 좋아요 개수를 집계하여 반환
     * @param commentPks 좋아요 개수를 조회할 댓글 PK 목록
     * @return 각 댓글의 좋아요 개수 목록
     */
    public List<CommentLikeCountProjection> countLikesByCommentPks(Set<Long> commentPks) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("comment_pk").in(commentPks)),
                Aggregation.group("comment_pk").count().as("likeCount"),
                Aggregation.project()
                        .andExpression("_id").as("commentPk")   // 위에서 groupBy 의 key(_id) 는 comment_pk 였기에 projection 의 _id 에 대한 alias 를 commentPk 로 지정
                                                                                    // MongoDB Aggregation 에서 $group 의 결과는 항상 _id 필드에 저장
                        .andInclude("likeCount")
                        .andExclude("_id")
        );

        log.info("countLikesByCommentPks aggregation query: {}", aggregation.toString());

        AggregationResults<CommentLikeCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comment_likes", CommentLikeCountProjection.class);

        List<CommentLikeCountProjection> mappedResults = results.getMappedResults();
        log.info("countLikesByCommentPks results count: {}", mappedResults.size());
        mappedResults.forEach(result ->
                log.info("commentPk: {}, likeCount: {}", result.getCommentPk(), result.getLikeCount())
        );

        return mappedResults;
    }

    /**
     * 특정 댓글의 좋아요 개수를 집계하여 반환
     * @param commentPk 좋아요 개수를 조회할 댓글 PK
     * @return 댓글의 좋아요 개수 (없으면 0)
     */
    public Long countLikesByCommentPk(Long commentPk) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("comment_pk").is(commentPk)),
                Aggregation.count().as("likeCount")
        );

        AggregationResults<CommentLikeCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comment_likes", CommentLikeCountProjection.class);

        return results.getMappedResults().isEmpty() ? 0L : results.getMappedResults().get(0).getLikeCount();
    }
}
