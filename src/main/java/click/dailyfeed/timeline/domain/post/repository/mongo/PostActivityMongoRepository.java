package click.dailyfeed.timeline.domain.post.repository.mongo;

import click.dailyfeed.timeline.domain.post.document.PostActivity;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PostActivityMongoRepository extends MongoRepository<PostActivity, ObjectId> {

    @Query("{ 'member_id': { $in: ?0 }, 'post_activity_type': { $in: ['CREATE', 'UPDATE'] }, 'created_at': { $gte: ?1 } }")
    Page<PostActivity> findFollowingActivitiesWhereFollowingIdsIn(List<Long> followingIds, LocalDateTime since, Pageable pageable);
}
