package com.paladin.coverLetter.service.impl;

import com.paladin.coverLetter.repository.CoverletterTemplateRepository;
import com.paladin.coverLetter.service.CoverletterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoverLetterServiceImpl implements CoverletterService {

    private final CoverletterTemplateRepository coverletterTemplates;

    private static final Map<String, String> COVER_LETTER_TONES = Map.of(
            "professional", "Professional/Formal",
            "enthusiastic", "Enthusiastic/Energetic",
            "results", "Results-focused/Quantitative",
            "conversational", "Conversational/Personal"
    );

    public String generate(String category, String candidateName,  String companyName,  String position) {
        String mappedCategory = COVER_LETTER_TONES.get(category);

        if (!COVER_LETTER_TONES.containsKey(category)) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
        String template = coverletterTemplates.getRandomTemplate(mappedCategory);

        if (candidateName != null) {
            template = template.replace("[candidateName]", candidateName);
        }
        if (companyName != null) {
            template = template.replace("[company]", companyName);
        }
        if (position != null) {
            template = template.replace("[position]", position);
        }

        return template;
    }
}
