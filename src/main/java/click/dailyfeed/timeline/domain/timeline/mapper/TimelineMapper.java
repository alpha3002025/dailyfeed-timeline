package click.dailyfeed.timeline.domain.timeline.mapper;

import click.dailyfeed.code.global.web.response.DailyfeedScrollPage;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimelineMapper {
    default <T> DailyfeedScrollPage<T> fromTimelineList(List<T> list, Pageable pageable) {
        return DailyfeedScrollPage.<T>builder()
                .content(list)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    default <T> DailyfeedScrollPage<T> emptySlice(){
        return DailyfeedScrollPage.<T>builder()
                .content(List.of())
                .page(0)
                .size(0)
                .build();
    }
}
