package click.dailyfeed.timeline.domain.post.kafka;

import click.dailyfeed.code.domain.content.post.dto.PostDto;
import click.dailyfeed.timeline.domain.post.document.PostActivity;
import click.dailyfeed.timeline.domain.post.mapper.TimelinePostMapper;
import click.dailyfeed.timeline.domain.post.redis.PostActivityRedisService;
import click.dailyfeed.timeline.domain.post.repository.mongo.PostActivityMongoRepository;
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
public class PostActivityConsumer {
    private final PostActivityMongoRepository postActivityMongoRepository;
    private final PostActivityRedisService postActivityRedisService;
    private final TimelinePostMapper timelinePostMapper;

    @KafkaListener(
            topicPattern = "post-activity-.*",
            groupId = "post-activity-consumer-group-1",
            containerFactory = "postActivityKafkaListenerContainerFactory"
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

    private void cachingPostActivityEvent(PostDto.PostActivityEvent event) {
        // 1) Message read
        if (event == null) {
            return;
        }

        // 2) cache put
        postActivityRedisService.rPushEvent(event);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Scheduled(fixedRate = 1000)
    public void insertMany(){
        // 1초에 한번씩 동작
        while(true){
            List<PostDto.PostActivityEvent> eventList = postActivityRedisService.lPopList();
            if(eventList == null || eventList.isEmpty()){
                break;
            }
            try{
                List<PostActivity> insertList = eventList.stream().map(timelinePostMapper::fromPostActivityEvent).toList();
                postActivityMongoRepository.saveAll(insertList);
            }
            catch (Exception e){
                // kafka DLQ publish
                // (TODO 구현 예정)

                // redis DLQ caching
                postActivityRedisService.rPushDeadLetterEvent(eventList);
            }
        }

        // insert 실패시
        // 다른 Redis List 에 put  (실패한 데이터이더라도, 실패 후 다시 성공적으로 insert 하면,
        // created_at 기준으로 읽어들이기 때문에, (도큐먼트의 순서가 아닌, created_at 으로 읽어들이므로)
        // 후처리가 데이터의 모호함을 만들지 않는다.

    }

}
