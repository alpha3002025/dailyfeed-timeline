package click.dailyfeed.timeline.domain.timeline.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.timeline.domain.comment.projection.PostCommentCountProjection;
import click.dailyfeed.timeline.domain.post.entity.Post;
import click.dailyfeed.timeline.domain.post.projection.PostLikeCountProjection;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimelineMapper {
    default <T> DailyfeedScrollPage<T> fromTimelineList(List<T> list, Pageable pageable) {
        return DailyfeedScrollPage.<T>builder()
                .content(list)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    default PostDto.PostLikeCountStatistics toPostLikeStatistics(PostLikeCountProjection postLikeCountProjection) {
        return PostDto.PostLikeCountStatistics.builder()
                .postPk(postLikeCountProjection.getPostPk())
                .likeCount(postLikeCountProjection.getLikeCount())
                .build();
    }

    default PostDto.PostCommentCountStatistics toPostCommentCountStatistics(PostCommentCountProjection postCommentCountProjection) {
        return PostDto.PostCommentCountStatistics.builder()
                .postPk(postCommentCountProjection.getPostPk())
                .commentCount(postCommentCountProjection.getCommentCount())
                .build();
    }

    default PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, PostDto.PostLikeCountStatistics postLikeStatistics, PostDto.PostCommentCountStatistics commentCountStatistics) {
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getDisplayName() : null)
                .authorHandle(author != null ? author.getMemberHandle() : null)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .viewCount(post.getViewCount())
                .likeCount(postLikeStatistics != null ? postLikeStatistics.getLikeCount() : 0L)
                .commentCount(commentCountStatistics != null ? commentCountStatistics.getCommentCount() : 0L)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
