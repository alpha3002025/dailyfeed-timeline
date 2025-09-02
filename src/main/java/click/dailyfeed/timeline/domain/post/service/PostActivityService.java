package click.dailyfeed.timeline.domain.post.service;

import click.dailyfeed.timeline.domain.post.repository.mongo.PostActivityMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class PostActivityService {
    private final PostActivityMongoRepository postActivityMongoRepository;

    // 카프카 리슨
    public void listenPostActivityMessage(){
        // 1) Message read
        // 2) cache put
    }

    // 스케쥴링
    public void batchInsertMany(){
        // cache 에 쌓인 데이터를 주기적으로 MongoDB 에 insert Many 후 완료되면 캐시를 비운다.
        // insert 실패시 :
    }
}
