package click.dailyfeed.timeline.domain.post.mapper;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
                .followingId(postActivityEvent.getFollowingId())
                .postId(postActivityEvent.getPostId())
                .postActivityType(postActivityEvent.getPostActivityType())
                .build();
    }
}
