package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.document.CommentLikeDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentLikeMongoRepository extends MongoRepository<CommentLikeDocument, ObjectId> {
    CommentLikeDocument findByCommentPkAndMemberId(Long commentPk, Long memberId);
}
