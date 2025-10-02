package com.paladin.common.mappers;

import com.paladin.common.dto.CVDTO;
import com.paladin.cv.CV;
import com.paladin.common.dto.CVSummaryDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CVMapper {
    CVDTO toDTO(CV cv);

    CV toEntity(CVDTO dto);

    CVSummaryDTO toSummaryDTO(CV cv);
}

