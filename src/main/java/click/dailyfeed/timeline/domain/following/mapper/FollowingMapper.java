package click.dailyfeed.timeline.domain.following.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface FollowingMapper {
    default PostDto.PostActivityEvent toEvent(PostActivity postActivity) {
        return PostDto.PostActivityEvent.builder()
                .followingId(postActivity.getFollowingId())
                .postId(postActivity.getPostId())
                .postActivityType(postActivity.getPostActivityType())
                .createdAt(postActivity.getCreatedAt())
                .updatedAt(postActivity.getUpdatedAt())
                .build();
    }

    default <T> DailyfeedPage<T> fromMongoPage(Page<PostActivity> page, List<T> content) {
        return DailyfeedPage.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    default <T> DailyfeedPage<T> emptyPage() {
        return DailyfeedPage.<T>builder()
                .content(List.of())
                .page(0)
                .size(0)
                .totalElements(0)
                .totalPages(0)
                .isFirst(true)
                .isLast(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
