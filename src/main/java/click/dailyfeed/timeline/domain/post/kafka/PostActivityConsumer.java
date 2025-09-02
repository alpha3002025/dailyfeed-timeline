package click.dailyfeed.timeline.domain.post.kafka;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostActivityConsumer {

    @KafkaListener(
            topics = "post-activity-*",
            groupId = "post-activity-consumer-group-1",
            containerFactory = "postActivityConsumerFactory"
    )
    public void consumeAllPostActivityEvents(
            @Payload PostDto.PostActivityEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset ) {
        // 토픽명에서 날짜 추출
        String dateStr = extractDateFromTopicName(topic);

        if (dateStr != null) {
            // 날짜 형식 검증 (yyyyMMdd 형식인지 확인)
            if (dateStr.matches("\\d{8}")) { // 날짜 타입 처리
                processEventByDate(event, dateStr);
            }
            else{
                // 날짜 타입이 아닌 다른 타입의 토픽 분류
            }
        }
    }

    /**
     * 토픽명에서 날짜 추출
     */
    private String extractDateFromTopicName(String topicName) {
        String prefix = "post-created-activity-";
        if (topicName.startsWith(prefix)) {
            return topicName.substring(prefix.length());
        }
        return null;
    }

    /**
     * 날짜별 이벤트 처리
     */
    private void processEventByDate(PostDto.PostActivityEvent event, String dateStr) {
        LocalDate eventDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate today = LocalDate.now();

        if (eventDate.equals(today)) {
//            processCurrentDateEvent(event);
            processEvent(event);
        } else if (eventDate.isBefore(today)) {
            if(eventDate.isAfter(today.minusDays(2))) {
                // 이틀전 이벤트 까지만 처리 (
                processEvent(event);
            }
            else{
//                processHistoricalEvent(event);
            }
        } else {
            log.info("미래에서 오셨군요");
        }
    }

    private void processEvent(PostDto.PostActivityEvent event) {
        // 레디스 luascript 기반 put
    }


    /**
     * 현재 날짜 이벤트 처리
     */
    private void processCurrentDateEvent(PostDto.PostActivityEvent event) {
        log.info("Processing current date event: postId={}, activityType={}",
                event.getPostId(), event.getPostActivityType());
        // 실시간 처리 로직 구현
//        switch (event.getPostActivityType()) {
//            case PostActivityType.CREATE:
//                handlePostCreated(event);
//                break;
//            case PostActivityType.UPDATE:
//                handlePostUpdated(event);
//                break;
//            case PostActivityType.DELETE:
//                handlePostDeleted(event);
//                break;
//            default:
//                log.warn("Unknown activity type: {}", event.getPostActivityType());
//        }
    }

    /**
     * 과거 날짜 이벤트 처리 (배치 처리) // 배치 모듈에서... 구독
     */
    private void processHistoricalEvent(PostDto.PostActivityEvent event) {
        log.info("Processing historical event: postId={}, eventDate={}",
                event.getPostId(), event.getCreatedAt());
    }

}
