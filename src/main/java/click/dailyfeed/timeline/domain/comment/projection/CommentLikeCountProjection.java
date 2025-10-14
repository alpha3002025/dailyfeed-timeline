package click.dailyfeed.timeline.domain.comment.projection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글별 좋아요 개수를 담는 Projection
 * MongoDB Aggregation 결과를 매핑하기 위한 클래스
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentLikeCountProjection {
    private Long commentPk;
    private Long likeCount;
}