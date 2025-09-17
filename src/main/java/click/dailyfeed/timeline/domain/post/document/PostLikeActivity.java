package click.dailyfeed.timeline.domain.post.document;

import click.dailyfeed.code.domain.content.post.type.PostLikeType;
import lombok.Builder;
import lombok.Getter;
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
@Document("post_like_activities")
@CompoundIndexes({
        @CompoundIndex(name = "member_created_idx", def = "{'member_id': 1, 'created_at': -1}"),
})
public class PostLikeActivity {
    @Id
    private ObjectId id;

    @Field("member_id")
    @Indexed
    private Long memberId;

    @Field("post_id")
    private Long postId;

    @Field("post_like_type")
    private PostLikeType postLikeType;

    @CreatedDate
    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @PersistenceCreator
    public PostLikeActivity(
            ObjectId id, Long memberId, Long postId, PostLikeType postLikeType, LocalDateTime createdAt, LocalDateTime updatedAt
    ){
        this.id = id;
        this.memberId = memberId;
        this.postId = postId;
        this.postLikeType = postLikeType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Builder(builderMethodName = "newDocumentBuilder")
    public PostLikeActivity(Long memberId, Long postId, PostLikeType postLikeType) {
        this.memberId = memberId;
        this.postId = postId;
        this.postLikeType = postLikeType;
    }

}
