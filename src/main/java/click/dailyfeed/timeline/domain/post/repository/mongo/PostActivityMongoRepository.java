package click.dailyfeed.timeline.domain.post.repository.mongo;

import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PostActivityMongoRepository extends MongoRepository<PostActivity, ObjectId> {

    Page<PostActivity> findByFollowingIdAndPostActivityTypeNotContainsOrderByUpdatedAtDesc(
            Long followingId,
            PostActivityType activityType,
            Pageable pageable);

    @Query(value = "{ 'followingId': ?0, 'postActivityType': { $ne: ?1 } }",
            sort = "{ 'updatedAt': -1 }")
    Page<PostActivity> findByFollowingIdAndActivityTypeNotEquals(
            Long followingId,
            PostActivityType activityType,
            Pageable pageable);
}
