package click.dailyfeed.timeline.domain.post.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostActivityDynamicTopicService {
    private final KafkaAdmin kafkaAdmin;

    @Value("${infrastructure.kafka.topic.post-activity.prefix}")
    private String topicPrefix;

    @Value("${infrastructure.kafka.topic.post-activity.prefix-date-format}")
    private String dateFormat;

    /**
     * 특정 날짜의 토픽명을 생성
     */
    public String generateTopicName(LocalDate date) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
        try{
            return topicPrefix + date.format(dateTimeFormatter);
        }
        catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("date format error");
        }
    }

    /**
     * 현재 날짜의 토픽명 반환
     */
    public String getCurrentDateTopic() {
        return generateTopicName(LocalDate.now());
    }

    /**
     * 날짜 범위에 해당하는 모든 토픽명 생성
     */
    public List<String> generateTopicNamesForDateRange(LocalDate startDate, LocalDate endDate) {
        List<String> topicNames = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            topicNames.add(generateTopicName(current));
            current = current.plusDays(1);
        }

        return topicNames;
    }

    /**
     * 토픽 존재 여부 확인
     */
    public boolean topicExists(String topicName) {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            Set<String> existingTopics = listTopicsResult.names().get();
            return existingTopics.contains(topicName);
        } catch (Exception e) {
            log.error("Failed to check topic existence: {}", topicName, e);
            return false;
        }
    }

    /**
     * 동적으로 토픽 생성
     */
    public CompletableFuture<Void> createTopicIfNotExists(String topicName) {
        return CompletableFuture.runAsync(() -> {
            if (topicExists(topicName)) {
                log.info("Topic already exists: {}", topicName);
                return;
            }

            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                NewTopic newTopic = new NewTopic(topicName, 3, (short) 1);
                CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
                result.all().get();
                log.info("Successfully created topic: {}", topicName);
            } catch (Exception e) {
                log.error("Failed to create topic: {}", topicName, e);
                throw new RuntimeException("Failed to create topic: " + topicName, e);
            }
        });
    }

    /**
     * 여러 토픽을 한번에 생성
     */
    public CompletableFuture<Void> createTopicsIfNotExists(List<String> topicNames) {
        return CompletableFuture.runAsync(() -> {
            List<String> topicsToCreate = topicNames.stream()
                    .filter(topicName -> !topicExists(topicName))
                    .collect(Collectors.toList());

            if (topicsToCreate.isEmpty()) {
                log.info("All topics already exist");
                return;
            }

            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                List<NewTopic> newTopics = topicsToCreate.stream()
                        .map(topicName -> new NewTopic(topicName, 3, (short) 1))
                        .collect(Collectors.toList());

                CreateTopicsResult result = adminClient.createTopics(newTopics);
                result.all().get();
                log.info("Successfully created topics: {}", topicsToCreate);
            } catch (Exception e) {
                log.error("Failed to create topics: {}", topicsToCreate, e);
                throw new RuntimeException("Failed to create topics", e);
            }
        });
    }

    /**
     * 지정된 날짜 범위의 토픽들을 미리 생성
     */
    public void prepareTopicsForDateRange(LocalDate startDate, LocalDate endDate) {
        List<String> topicNames = generateTopicNamesForDateRange(startDate, endDate);
        createTopicsIfNotExists(topicNames).join();
    }

    /**
     * 현재 존재하는 post-created-activity 토픽들 조회
     */
    public List<String> getExistingPostActivityTopics() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult listTopicsResult = adminClient.listTopics();
            Set<String> existingTopics = listTopicsResult.names().get();

            return existingTopics.stream()
                    .filter(topic -> topic.startsWith(topicPrefix))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get existing post activity topics", e);
            return Collections.emptyList();
        }
    }
}
