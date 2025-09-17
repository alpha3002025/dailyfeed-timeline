package click.dailyfeed.timeline.domain.comment.document;

import click.dailyfeed.code.domain.content.comment.type.CommentActivityType;
import lombok.Builder;
import lombok.Getter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Builder
@Document("comment_activities")
public class CommentActivity {
    @Id
    private ObjectId id;

    @Field("author_id")
    @Indexed
    private Long authorId;

    @Field("comment_id")
    @Indexed
    private Long commentId;

    @Field("comment_activity_type")
    private CommentActivityType commentActivityType;

    @CreatedDate
    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @PersistenceCreator
    public CommentActivity(ObjectId id, Long authorId, Long commentId, CommentActivityType commentActivityType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.commentId = commentId;
        this.commentActivityType = commentActivityType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Builder(builderMethodName = "newDocumentBuilder")
    public CommentActivity(Long authorId, Long commentId, CommentActivityType commentActivityType){
        this.authorId = authorId;
        this.commentId = commentId;
        this.commentActivityType = commentActivityType;
    }
}
