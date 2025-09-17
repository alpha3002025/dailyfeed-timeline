package click.dailyfeed.timeline.domain.comment.mapper;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.timeline.domain.comment.document.CommentActivity;
import click.dailyfeed.timeline.domain.comment.document.CommentLikeActivity;
import org.springframework.stereotype.Component;

@Component
public class TimelineCommentMapper {
    public CommentActivity fromCommentActivityEvent(CommentDto.CommentActivityEvent commentActivityEvent) {
        return CommentActivity.newDocumentBuilder()
                .authorId(commentActivityEvent.getMemberId())
                .commentId(commentActivityEvent.getCommentId())
                .commentActivityType(commentActivityEvent.getCommentActivityType())
                .build();
    }

    public CommentLikeActivity fromCommentLikeActivityEvent(CommentDto.LikeActivityEvent likeActivityEvent) {
        return CommentLikeActivity.newDocumentBuilder()
                .memberId(likeActivityEvent.getMemberId())
                .commentId(likeActivityEvent.getCommentId())
                .commentLikeType(likeActivityEvent.getCommentLikeType())
                .build();
    }
}
