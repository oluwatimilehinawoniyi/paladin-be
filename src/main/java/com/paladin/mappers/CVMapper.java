package com.paladin.mappers;

import com.paladin.dto.CVDTO;
import com.paladin.cv.CV;
import com.paladin.dto.CVSummaryDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CVMapper {
    CVDTO toDTO(CV cv);

    CV toEntity(CVDTO dto);

    CVSummaryDTO toSummaryDTO(CV cv);
}

