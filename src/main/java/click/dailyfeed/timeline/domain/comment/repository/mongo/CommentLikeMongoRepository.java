package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.document.CommentLikeDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Set;

public interface CommentLikeMongoRepository extends MongoRepository<CommentLikeDocument, ObjectId> {
    CommentLikeDocument findByCommentPkAndMemberId(Long commentPk, Long memberId);

    @Query("{ 'comment_pk': { $in: ?0 } }")
    List<CommentLikeDocument> findByCommentPkIn(Set<Long> commentPks);
}
