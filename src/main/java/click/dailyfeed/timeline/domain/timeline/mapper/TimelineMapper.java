package click.dailyfeed.timeline.domain.timeline.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.timeline.timeline.dto.TimelineDto;
import click.dailyfeed.code.global.menu.MessageProperties;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.timeline.domain.comment.entity.Comment;
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

    default TimelineDto.TimelinePostActivity toTimelinePostActivity(PostDto.Post p, Boolean liked, MemberProfileDto.Summary author) {
        return TimelineDto.TimelinePostActivity
                .builder()
                .likeCount(p.getLikeCount())
                .commentCount(p.getCommentCount())
                .viewCount(p.getViewCount())
                .liked(liked)
                .id(p.getId())
                .authorId(p.getAuthorId())
                .authorName(author != null ? author.getDisplayName() : "Unknown")
                .authorHandle(author != null ? author.getMemberHandle() : "unknown")
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .createdAt(p.getCreatedAt())
                .title(p.getTitle())
                .content(p.getContent())
                .build();
    }

    default PostDto.Post toPostDtoWithCountProjection(PostDto.Post post, PostCommentCountProjection projection) {
        return PostDto.Post.builder()
                .viewCount(post != null ? post.getViewCount() : 0L)
                .likeCount(post.getLikeCount())
                .commentCount(projection.getCommentCount())
                .liked(post.getLiked())
                .id(post != null ? post.getId() : null)
                .authorId(post != null ? post.getAuthorId() : null)
                .authorName(post != null ? post.getAuthorName() : "Unknown")
                .authorHandle(post != null ? post.getAuthorHandle() : "unknown")
                .authorAvatarUrl(post != null ? post.getAuthorAvatarUrl() : null)
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
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

    default PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, Boolean liked, PostDto.PostLikeCountStatistics postLikeStatistics, PostDto.PostCommentCountStatistics commentCountStatistics) {
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
                .liked(liked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    default CommentDto.Comment toReplyCommentAtTopLevel(Comment comment, Long replyCount, Long commentLikeCount, MemberProfileDto.Summary author) {
        Post post = null;
        if (comment != null){
            post = comment.getPost();
        }

        return CommentDto.Comment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getMemberName() : MessageProperties.KO.DELETED_USER)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : MessageProperties.KO.NO_AVATAR_URL)
                .postId(post != null ? post.getId() : null)
                .parentId(null)
                .depth(comment.getDepth())
                .likeCount(commentLikeCount)
                .replyCount(replyCount)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    default CommentDto.Comment toReplyComment(Long parentCommentId, Comment comment, Long replyCount, Long commentLikeCount, MemberProfileDto.Summary author) {
        return CommentDto.Comment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getMemberName() : null)
                .authorAvatarUrl(author != null ? author.getAvatarUrl() : null)
                .postId(parentCommentId != null ? parentCommentId : null)
                .parentId(null)
                .depth(comment.getDepth())
                .likeCount(commentLikeCount)
                .replyCount(replyCount)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
