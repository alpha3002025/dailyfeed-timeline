package click.dailyfeed.timeline.domain.comment.repository.jpa;

import click.dailyfeed.timeline.domain.comment.entity.Comment;
import click.dailyfeed.timeline.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 댓글과 모든 자식 댓글들을 소프트 삭제
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true, c.createdAt = CURRENT_TIMESTAMP WHERE c.id = :commentId OR c.parent.id = :commentId")
    void softDeleteCommentAndChildren(@Param("commentId") Long commentId);

    interface PostCommentCountProjection {
        Long getPostId();
        Long getCommentCount();
    }

    // 글 하나에 대한 댓글 수 조회
    @Query("SELECT p.id as postId, COUNT(c.id) as commentCount " +
            "FROM Post p LEFT JOIN Comment c ON p.id = c.post.id AND c.isDeleted = false " +
            "WHERE p IN :posts AND p.isDeleted = false " +
            "GROUP BY p.id")
    List<PostCommentCountProjection> findCommentCountsByPosts(@Param("posts") List<Post> posts);
}
