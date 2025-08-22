package com.paladin.coverLetter.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CoverletterTemplateRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getTemplatesByCategory(String category) {
        return jdbcTemplate.queryForList(
                "SELECT template_text FROM cover_letter_templates WHERE style_category = ?",
                String.class,
                category
        );
    }

    public String getRandomTemplate(String category) {
        return jdbcTemplate.queryForObject(
                "SELECT template_text FROM cover_letter_templates WHERE style_category = ? ORDER BY RANDOM() LIMIT 1",
                String.class,
                category
        );
    }

}
