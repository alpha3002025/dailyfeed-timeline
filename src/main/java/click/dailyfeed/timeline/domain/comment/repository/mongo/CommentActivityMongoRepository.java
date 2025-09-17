package click.dailyfeed.timeline.domain.comment.repository.mongo;

import click.dailyfeed.timeline.domain.comment.document.CommentActivity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentActivityMongoRepository extends MongoRepository<CommentActivity, ObjectId> {
}
