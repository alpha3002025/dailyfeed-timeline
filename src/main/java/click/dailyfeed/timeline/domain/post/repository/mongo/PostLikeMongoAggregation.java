package click.dailyfeed.timeline.domain.post.repository.mongo;

import click.dailyfeed.timeline.domain.post.projection.PostLikeCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class PostLikeMongoAggregation {
    private final MongoTemplate mongoTemplate;

    public List<PostLikeCountProjection> countLikesByPostPks(Set<Long> postPks) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("post_pk").in(postPks)),
                Aggregation.group("post_pk").count().as("likeCount"),
                Aggregation.project()
                        .andExpression("_id").as("postPk")
                        .andInclude("likeCount")
                        .andExclude("_id")
        );

        AggregationResults<PostLikeCountProjection> results =
                mongoTemplate.aggregate(aggregation, "post_likes", PostLikeCountProjection.class);

        return results.getMappedResults();
    }

    public Long countLikesByPostPk(Long postPk) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("post_pk").is(postPk)),
                Aggregation.count().as("likeCount")
        );

        AggregationResults<PostLikeCountProjection> results =
                mongoTemplate.aggregate(aggregation, "post_likes", PostLikeCountProjection.class);

        return results.getMappedResults().isEmpty() ? 0L : results.getMappedResults().get(0).getLikeCount().longValue();
    }
}
