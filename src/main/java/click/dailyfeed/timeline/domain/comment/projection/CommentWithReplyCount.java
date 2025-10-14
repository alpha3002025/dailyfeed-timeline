package click.dailyfeed.timeline.domain.comment.projection;

import click.dailyfeed.timeline.domain.comment.entity.Comment;
import click.dailyfeed.timeline.domain.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대댓글 수를 포함한 댓글 정보 Projection
 * 최상위 댓글 조회 시 각 댓글의 대댓글 개수를 함께 반환하기 위한 Projection
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentWithReplyCount {
    private Long id;
    private String content;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long postId;
    private Long parentId;
    private Integer depth;
    private Long likeCount;
    private Boolean isDeleted;

    // 대댓글 개수
    private Long replyCount;

    /**
     * Comment 엔티티로부터 CommentWithReplyCount 생성 (replyCount 포함)
     */
    public static CommentWithReplyCount from(Comment comment, Long replyCount) {
        return CommentWithReplyCount.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .likeCount(comment.getLikeCount())
                .isDeleted(comment.getIsDeleted())
                .replyCount(replyCount)
                .build();
    }
}