package click.dailyfeed.timeline.domain.comment.repository.jpa;

import click.dailyfeed.timeline.domain.comment.entity.Comment;
import click.dailyfeed.timeline.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글의 최상위 댓글들을 Slice 조회 (Scroll 용도)
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parent IS NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    Slice<Comment> findTopLevelCommentsByPostWithPaging(@Param("post") Post post, Pageable pageable);

    // ID로 댓글 조회 (삭제되지 않은)
    @Query("SELECT c FROM Comment c INNER JOIN FETCH c.post WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findByIdAndNotDeleted(@Param("id") Long id);

    // 특정 댓글의 대댓글들을 Slice 조회 (Scroll 용도)
    @Query("SELECT c FROM Comment c WHERE c.parent = :parent AND c.isDeleted = false ORDER BY c.createdAt ASC")
    Slice<Comment> findChildrenByParentSlice(@Param("parent") Comment parent, Pageable pageable);

    // 특정 사용자의 댓글들 (Scroll 용도)
    @Query("SELECT c FROM Comment c WHERE c.authorId = :authorId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Slice<Comment> findByAuthorIdAndNotDeleted(@Param("authorId") Long authorId, Pageable pageable);

    // 특정 게시글의 최상위 댓글들을 대댓글 개수와 함께 조회 (Scroll 용도)
    // 대댓글 개수를 포함한 Projection 을 반환
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    Slice<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

    // 특정 댓글들의 대댓글 개수를 조회
    @Query("SELECT c.parent.id as parentId, COUNT(c) as replyCount " +
           "FROM Comment c " +
           "WHERE c.parent.id IN :parentIds AND c.isDeleted = false " +
           "GROUP BY c.parent.id")
    List<ReplyCountProjection> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    // 대댓글 개수 조회를 위한 Projection 인터페이스
    interface ReplyCountProjection {
        Long getParentId();
        Long getReplyCount();
    }
}
