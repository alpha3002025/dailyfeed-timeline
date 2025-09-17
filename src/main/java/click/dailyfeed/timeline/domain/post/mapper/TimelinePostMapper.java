package click.dailyfeed.timeline.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import click.dailyfeed.timeline.domain.post.document.PostLikeActivity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimelinePostMapper {

//    @Mapping(source = "postActivityEvent.memberId", target = "memberId")
//    @Mapping(source = "postActivityEvent.followingId", target = "followingId")
//    @Mapping(source = "postActivityEvent.postId", target = "postId")
//    @Mapping(source = "postActivityEvent.postActivityType", target = "postActivityType")
//    @Mapping(source = "postActivityEvent.createdAt", target = "createdAt")
//    @Mapping(source = "postActivityEvent.updatedAt", target = "updatedAt")
//    PostActivity fromPostActivityEvent(PostDto.PostActivityEvent postActivityEvent);

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
}
