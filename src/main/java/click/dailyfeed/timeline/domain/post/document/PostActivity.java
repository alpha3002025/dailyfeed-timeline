package click.dailyfeed.timeline.domain.post.document;

import click.dailyfeed.code.domain.content.post.type.PostActivityType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Builder
@Document("post_activities")
@CompoundIndexes({
    // 특정 멤버의 팔로잉 활동 조회 (최신순) - is_deleted는 애플리케이션에서 필터링
    @CompoundIndex(name = "member_created_idx", def = "{'member_id': 1, 'created_at': -1}"),

    // 특정 사용자를 팔로우하는 활동 조회 (최신순) - is_deleted는 애플리케이션에서 필터링
    @CompoundIndex(name = "following_created_idx", def = "{'following_id': 1, 'created_at': -1}"),

    // 특정 포스트 관련 팔로잉 활동 조회
    @CompoundIndex(name = "post_created_idx", def = "{'post_id': 1, 'created_at': -1}"),

    // 팔로잉 관계 조회용 (현재 상태 확인)
    @CompoundIndex(name = "member_following_idx", def = "{'member_id': 1, 'following_id': 1}")
})
public class PostActivity {
    @Id
    private ObjectId id;

    @Field("member_id")
    @Indexed
    private Long memberId;

    @Field("post_id")
    private Long postId;

    @Field("post_activity_type")
    private PostActivityType postActivityType;

    @CreatedDate
    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @PersistenceCreator
    public PostActivity(
            ObjectId id, Long memberId, Long postId, PostActivityType postActivityType, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this.id = id;
        this.memberId = memberId;
        this.postId = postId;
        this.postActivityType = postActivityType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Builder(builderMethodName = "newDocumentBuilder")
    public PostActivity(Long memberId, Long postId, PostActivityType postActivityType) {
        this.memberId = memberId;
        this.postId = postId;
        this.postActivityType = postActivityType;
    }
}
