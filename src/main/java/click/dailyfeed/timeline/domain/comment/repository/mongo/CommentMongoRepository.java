package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.document.CommentDocument;
import click.dailyfeed.timeline.domain.comment.projection.PostCommentCountProjection;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;

public interface CommentMongoRepository extends MongoRepository<CommentDocument, ObjectId> {
    Optional<CommentDocument> findByCommentPkAndIsDeleted(Long commentPk, Boolean isDeleted);

    // 여러 포스트에 대한 댓글 수를 한 번에 조회
    @Aggregation(pipeline = {
            "{ '$match': { 'post_pk': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$post_pk', 'count': { '$sum': 1 } } }",
            "{ '$project': { 'postPk': '$_id', 'commentCount': '$count', '_id': 0 } }"
    })
    List<PostCommentCountProjection> countCommentsByPostPks(Set<Long> postPks);

    // 댓글이 가장 많은 글 순으로 정렬해서 조회
    @Aggregation(pipeline = {
            "{ '$match': { 'is_deleted': false } }",
            "{ '$group': { '_id': '$post_pk', 'count': { '$sum': 1 } } }",
            "{ '$sort': { 'count': -1 } }",
            "{ '$skip': ?#{#pageable.offset} }",
            "{ '$limit': ?#{#pageable.pageSize} }",
            "{ '$project': { 'postPk': '$_id', 'commentCount': '$count', '_id': 0 } }"
    })
    List<PostCommentCountProjection> findTopPostsByCommentCount(Pageable pageable);

}
