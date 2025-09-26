package click.dailyfeed.timeline.domain.comment.kafka;

import click.dailyfeed.code.domain.content.comment.dto.CommentDto;
import click.dailyfeed.code.global.kafka.type.DateBasedTopicType;
import click.dailyfeed.timeline.domain.comment.document.CommentLikeActivity;
import click.dailyfeed.timeline.domain.comment.mapper.TimelineCommentMapper;
import click.dailyfeed.timeline.domain.comment.redis.CommentLikeActivityEventRedisService;
import click.dailyfeed.timeline.domain.comment.repository.mongo.CommentLikeActivityMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentLikeActivityConsumer {
    private final CommentLikeActivityMongoRepository commentLikeActivityMongoRepository;
    private final CommentLikeActivityEventRedisService commentLikeActivityEventRedisService;
    private final TimelineCommentMapper timelineCommentMapper;

    @KafkaListener(
            topicPattern = DateBasedTopicType.COMMENT_LIKE_ACTIVITY_PATTERN,
            groupId = "comment-like-activity-consumer-group-timeline-svc",
            containerFactory = "commentLikeActivityKafkaListenerContainerFactory"
    )
    public void consumeAllPostActivityEvents(
            @Payload CommentDto.LikeActivityEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        // 오프셋 및 이벤트 정보 로깅
        log.debug("📨 Consuming message - Topic: {}, Partition: {}, Offset: {}, PostId: {}, EventType: {}",
                topic, partition, offset, event.getCommentId(), event.getCommentLikeType());

        try{
            // 토픽명에서 날짜 추출
            String dateStr = DateBasedTopicType.COMMENT_LIKE_ACTIVITY.extractDateFromTopicName(topic);

            if (dateStr != null) {
                // 날짜 형식 검증 (yyyyMMdd 형식인지 확인)
                if (dateStr.matches("\\d{8}")) { // 날짜 타입 처리
                    processEventByDate(event, dateStr);
                }
                else{
                    // 날짜 타입이 아닌 다른 타입의 토픽 분류
                }
            }

            // 메시지 처리 성공 후 오프셋 커밋
            acknowledgment.acknowledge();
            log.debug("✅ Offset committed - Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);

        } catch (Exception e) {
            log.error("❌ Failed to process message - Topic: {}, Partition: {}, Offset: {}, Error: {}",
                    topic, partition, offset, e.getMessage(), e);
            // 실패 시 오프셋 커밋하지 않음 - 재시작 시 이 메시지부터 다시 처리
            // DLQ로 보내거나 재시도 로직 구현 가능
        }
    }

    /**
     * 날짜별 이벤트 처리
     */
    private void processEventByDate(CommentDto.LikeActivityEvent event, String dateStr) {
        LocalDate eventDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate today = LocalDate.now();

        if (eventDate.equals(today)) {
            cachingPostActivityEvent(event);
        } else if (eventDate.isBefore(today)) {
            if(eventDate.isAfter(today.minusDays(2))) {
                cachingPostActivityEvent(event);
            }
            else{
                // 접미사가 yyyyMMdd 형식이 아닌 다른 형식의 토픽일 경우 이곳에서 처리 (운영을 위한 특정 용도)
            }
        } else {
            log.info("미래에서 오셨군요, 10년 뒤에 삼성전자 얼마에요?");
        }
    }

    private void cachingPostActivityEvent(CommentDto.LikeActivityEvent event) {
        // 1) Message read
        if (event == null) {
            return;
        }

        // 2) cache put
        commentLikeActivityEventRedisService.rPushEvent(event);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Scheduled(fixedRate = 1000)
    public void insertMany(){
        // 1초에 한번씩 동작 (애플리케이션 시작 5초 후부터)
        int processedBatches = 0;

        // 1초에 한번씩 동작
        while(true){
            List<CommentDto.LikeActivityEvent> eventList = commentLikeActivityEventRedisService.lPopList();
            if(eventList == null || eventList.isEmpty()){
                break;
            }
            try{
                log.info("📦 Processing batch #{} - size: {}", ++processedBatches, eventList.size());
                List<CommentLikeActivity> insertList = eventList
                        .stream()
                        .map(ev -> timelineCommentMapper.fromCommentLikeActivityEvent(ev))
                        .toList();

                commentLikeActivityMongoRepository.saveAll(insertList);
                log.debug("✅ Successfully saved {} events to MongoDB", eventList.size());
            }
            catch (Exception e){
                log.error("❌ Failed to save batch to MongoDB", e);

                // kafka DLQ publish
                // (TODO 구현 예정)

                // redis DLQ caching
                commentLikeActivityEventRedisService.rPushDeadLetterEvent(eventList);
                log.info("📮 Moved {} events to dead letter queue", eventList.size());
            }
        }
    }
}
