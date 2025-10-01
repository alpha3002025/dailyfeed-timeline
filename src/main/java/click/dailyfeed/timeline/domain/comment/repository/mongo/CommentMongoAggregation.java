package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.projection.PostCommentCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class CommentMongoAggregation {
    private final MongoTemplate mongoTemplate;

    public List<PostCommentCountProjection> countCommentsByPostPks(Set<Long> postPks) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("post_pk").in(postPks)),
                Aggregation.group("post_pk").count().as("commentCount"),
                Aggregation.project()
                        .andExpression("_id").as("postPk")
                        .andInclude("commentCount")
                        .andExclude("_id")
        );

        AggregationResults<PostCommentCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comments", PostCommentCountProjection.class);

        return results.getMappedResults();
    }

    public Long countCommentsByPostPk(Long postPk) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("post_pk").is(postPk)),
                Aggregation.count().as("commentCount")
        );

        AggregationResults<PostCommentCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comments", PostCommentCountProjection.class);

        return results.getMappedResults().isEmpty() ? 0L : results.getMappedResults().get(0).getCommentCount();
    }

    public List<PostCommentCountProjection> findTopPostsByCommentCount(Pageable pageable) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("is_deleted").is(false)),
                Aggregation.group("post_pk").count().as("commentCount"),
                Aggregation.sort(Sort.Direction.DESC, "commentCount"),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize()),
                Aggregation.project()
                        .andExpression("_id").as("postPk")
                        .andInclude("commentCount")
                        .andExclude("_id")
        );

        AggregationResults<PostCommentCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comments", PostCommentCountProjection.class);

        return results.getMappedResults();
    }
}
