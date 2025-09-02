package click.dailyfeed.timeline.domain.post.repository.mongo;

import click.dailyfeed.timeline.domain.post.document.PostActivity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostActivityMongoRepository extends MongoRepository<PostActivity, ObjectId> {
}
