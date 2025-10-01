package click.dailyfeed.timeline.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import click.dailyfeed.timeline.domain.post.document.PostLikeActivity;
import click.dailyfeed.timeline.domain.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimelinePostMapper {
    default PostActivity fromPostActivityEvent(PostDto.PostActivityEvent postActivityEvent) {
        return PostActivity.newDocumentBuilder()
                .memberId(postActivityEvent.getMemberId())
                .postId(postActivityEvent.getPostId())
                .postActivityType(postActivityEvent.getPostActivityType())
                .build();
    }

    default PostLikeActivity fromPostLikeActivityEvent(PostDto.LikeActivityEvent likeActivityEvent) {
        return PostLikeActivity.newDocumentBuilder()
                .memberId(likeActivityEvent.getMemberId())
                .postId(likeActivityEvent.getPostId())
                .postLikeType(likeActivityEvent.getPostLikeType())
                .build();
    }

    default PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, Long commentCount) {
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author == null ? null : author.getId())
                .authorName(author == null ? "탈퇴한 사용자" : author.getDisplayName())
                .authorHandle(author == null ? "탈퇴한 사용지" : author.getMemberHandle())
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    default PostDto.Post toPostDto(Post post, MemberProfileDto.Summary author, Long commentCount, Long likeCount) {
        return PostDto.Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(author == null ? null : author.getId())
                .authorName(author == null ? "탈퇴한 사용자" : author.getDisplayName())
                .authorHandle(author == null ? "탈퇴한 사용지" : author.getMemberHandle())
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .viewCount(post.getViewCount())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
