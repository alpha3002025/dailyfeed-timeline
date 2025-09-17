package click.dailyfeed.timeline.domain.post.kafka;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.code.global.kafka.type.DateBasedTopicType;
import click.dailyfeed.timeline.domain.post.document.PostLikeActivity;
import click.dailyfeed.timeline.domain.post.mapper.TimelinePostMapper;
import click.dailyfeed.timeline.domain.post.redis.PostLikeActivityEventRedisService;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostLikeActivityMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
public class PostLikeActivityConsumer {
    private final PostLikeActivityMongoRepository postLikeActivityMongoRepository;
    private final PostLikeActivityEventRedisService postLikeActivityEventRedisService;
    private final TimelinePostMapper timelinePostMapper;

    @KafkaListener(
            topicPattern = DateBasedTopicType.POST_LIKE_ACTIVITY_PATTERN,
            groupId = "post-like-activity-consumer-group-1",
            containerFactory = "postLikeActivityKafkaListenerContainerFactory"
    )
    public void consumeAllPostActivityEvents(
            @Payload PostDto.LikeActivityEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset ) {
        // 토픽명에서 날짜 추출
        String dateStr = DateBasedTopicType.POST_LIKE_ACTIVITY.extractDateFromTopicName(topic);
//        log.info("😀😀😀😀😀 topicName = {}, postId = {}, memberId = {}, followingId = {}, type = {}, createdAt = {}, updatedAt = {}", topic, event.getPostId(), event.getMemberId(), event.getFollowingId(), event.getPostActivityType(), event.getCreatedAt(), event.getUpdatedAt());

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
     * 날짜별 이벤트 처리
     */
    private void processEventByDate(PostDto.LikeActivityEvent event, String dateStr) {
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

    private void cachingPostActivityEvent(PostDto.LikeActivityEvent event) {
        // 1) Message read
        if (event == null) {
            return;
        }

        // 2) cache put
        postLikeActivityEventRedisService.rPushEvent(event);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Scheduled(fixedRate = 1000)
    public void insertMany(){
        // 1초에 한번씩 동작
        while(true){
            List<PostDto.LikeActivityEvent> eventList = postLikeActivityEventRedisService.lPopList();
            log.info("🔨🔨🔨🔨🔨🔨🔨eventList.size() = {}", eventList.size());
            if(eventList == null || eventList.isEmpty()){
                break;
            }
            try{
                List<PostLikeActivity> insertList = eventList
                        .stream()
                        .map(ev -> timelinePostMapper.fromPostLikeActivityEvent(ev))
                        .toList();

                postLikeActivityMongoRepository.saveAll(insertList);
            }
            catch (Exception e){
                // kafka DLQ publish
                // (TODO 구현 예정)

                // redis DLQ caching
                postLikeActivityEventRedisService.rPushDeadLetterEvent(eventList);
            }
        }

        // insert 실패시
        // 다른 Redis List 에 put  (실패한 데이터이더라도, 실패 후 다시 성공적으로 insert 하면,
        // created_at 기준으로 읽어들이기 때문에, (도큐먼트의 순서가 아닌, created_at 으로 읽어들이므로)
        // 후처리가 데이터의 모호함을 만들지 않는다.

    }
}
