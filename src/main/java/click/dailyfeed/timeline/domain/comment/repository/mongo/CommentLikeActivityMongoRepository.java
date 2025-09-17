package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.document.CommentLikeActivity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentLikeActivityMongoRepository extends MongoRepository<CommentLikeActivity, ObjectId> {
}
