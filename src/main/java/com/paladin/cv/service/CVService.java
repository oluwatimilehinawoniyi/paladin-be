package com.paladin.cv.service;

import com.paladin.cv.CV;
import com.paladin.common.dto.CVDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CVService {
    public CVDTO uploadCV(MultipartFile file, UUID profileId, UUID userID);

    public CVDTO getCVById(UUID cvId, UUID userId);

    public CVDTO getCVbyProfileId(UUID profileId, UUID userId);

    public CV getCVByIdAsEntity(UUID cvId);

    public CVDTO updateCV(UUID cvId, MultipartFile file, UUID userId);

    public byte[] downloadCV(UUID cvId, UUID userId);

    public void deleteCV(UUID cvId, UUID userId);
}
