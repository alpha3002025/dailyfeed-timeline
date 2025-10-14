package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.projection.PostCommentCountProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class CommentMongoAggregation {
    private final MongoTemplate mongoTemplate;

    public List<PostCommentCountProjection> countCommentsByPostPks(Set<Long> postPks) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("post_pk").in(postPks).and("is_deleted").is(false)),
                Aggregation.group("post_pk").count().as("commentCount"),
                Aggregation.project()
                        .andExpression("_id").as("postPk")  // 위에서 groupBy 의 key(_id) 는 post_pk 였기에 projection 의 _id 에 대한 alias 를 postPk 로 지정
                                                                                // MongoDB Aggregation 에서 $group 의 결과는 항상 _id 필드에 저장
                        .andInclude("commentCount")
                        .andExclude("_id")
        );

        log.info("countCommentsByPostPks aggregation query: {}", aggregation.toString());

        AggregationResults<PostCommentCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comments", PostCommentCountProjection.class);

        List<PostCommentCountProjection> mappedResults = results.getMappedResults();
        log.info("countCommentsByPostPks results count: {}", mappedResults.size());
        mappedResults.forEach(result ->
            log.info("postPk: {}, commentCount: {}", result.getPostPk(), result.getCommentCount())
        );

        return mappedResults;
    }

    public Long countCommentsByPostPk(Long postPk) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("post_pk").is(postPk).and("is_deleted").is(false)),
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
                        .andExpression("_id").as("postPk")   // 위에서 groupBy 의 key(_id) 는 post_pk 였기에 projection 의 _id 에 대한 alias 를 postPk 로 지정
                                                                                // MongoDB Aggregation 에서 $group 의 결과는 항상 _id 필드에 저장
                        .andInclude("commentCount")
                        .andExclude("_id")
        );

        AggregationResults<PostCommentCountProjection> results =
                mongoTemplate.aggregate(aggregation, "comments", PostCommentCountProjection.class);

        return results.getMappedResults();
    }
}
