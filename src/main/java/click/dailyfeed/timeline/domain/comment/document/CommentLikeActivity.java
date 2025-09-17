package click.dailyfeed.timeline.domain.comment.document;

import click.dailyfeed.code.domain.content.comment.type.CommentLikeType;
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
@Document("comment_like_activities")
@CompoundIndexes({
        @CompoundIndex(name = "member_created_idx", def = "{'member_id': 1, 'created_at': -1}"),
})
public class CommentLikeActivity {
    @Id
    private ObjectId id;

    @Field("member_id")
    @Indexed
    private Long memberId;

    @Field("comment_id")
    private Long commentId;

    @Field("comment_like_type")
    private CommentLikeType commentLikeType;

    @CreatedDate
    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @PersistenceCreator
    public CommentLikeActivity(
            ObjectId id, Long memberId, Long commentId, CommentLikeType commentLikeType, LocalDateTime createdAt, LocalDateTime updatedAt
    ){
        this.id = id;
        this.memberId = memberId;
        this.commentId = commentId;
        this.commentLikeType = commentLikeType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Builder(builderMethodName = "newDocumentBuilder")
    public CommentLikeActivity(Long memberId, Long commentId, CommentLikeType commentLikeType) {
        this.memberId = memberId;
        this.commentId = commentId;
        this.commentLikeType = commentLikeType;
    }
}
