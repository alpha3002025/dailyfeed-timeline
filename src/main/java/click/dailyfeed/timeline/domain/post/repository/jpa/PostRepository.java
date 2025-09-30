package click.dailyfeed.timeline.domain.post.repository.jpa;

import click.dailyfeed.timeline.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 인기 게시글 (좋아요 수 기준)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false ORDER BY (p.viewCount + p.likeCount * 2) DESC, p.createdAt DESC")
    Page<Post> findPopularPostsNotDeleted(Pageable pageable);

    // 특정 기간 내 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByCreatedDateBetweenAndNotDeleted(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    // 댓글이 많은 게시글 조회 (댓글 수로 정렬)
    @Query("SELECT p " +
            "FROM Post p LEFT JOIN p.comments c " +
            "WHERE p.isDeleted = false AND (c.isDeleted = false OR c.id IS NULL) " +
            "GROUP BY p " +
            "ORDER BY COUNT(c) DESC, p.createdAt DESC")
    Page<Post> findMostCommentedPosts(Pageable pageable);

    // 최근 활동이 있는 게시글 (최근 댓글 기준)
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN p.comments c " +
            "WHERE p.isDeleted = false AND (c.isDeleted = false OR c.id IS NULL) " +
            "ORDER BY COALESCE(MAX(c.createdAt), p.createdAt) DESC")
    Page<Post> findPostsByRecentActivity(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false and p.id in :ids ORDER BY p.createdAt DESC")
    List<Post> findPostsByIdsInNotDeletedOrderByCreatedDateDesc(Set<Long> ids);
}
