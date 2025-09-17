package click.dailyfeed.timeline.domain.post.repository.mongo;

import click.dailyfeed.timeline.domain.post.document.PostLikeActivity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostLikeActivityMongoRepository extends MongoRepository<PostLikeActivity, ObjectId> {
}
