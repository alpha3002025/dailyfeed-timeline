package click.dailyfeed.timeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableScheduling
@EnableJpaAuditing
@EnableMongoAuditing
@EnableJpaRepositories(
        basePackages = "click.dailyfeed.timeline.domain.**.repository.jpa",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
@EnableMongoRepositories(
        basePackages = "click.dailyfeed.timeline.domain.**.repository.mongo",
        mongoTemplateRef = "mongoTemplate"
)
@EnableTransactionManagement
@SpringBootApplication
@ComponentScan(basePackages = {
        "click.dailyfeed.feign",
        "click.dailyfeed.timeline",
        "click.dailyfeed.pagination",
        "click.dailyfeed.redis",
        "click.dailyfeed.kafka",
})
public class TimelineApplication {

	public static void main(String[] args) {
		SpringApplication.run(TimelineApplication.class, args);
	}

}
