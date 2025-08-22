-- Replace [profileTitle] with [position] in all existing templates
UPDATE cover_letter_templates
SET template_text = REPLACE(template_text, '[profileTitle]', '[position]')
WHERE style_category IN ('Professional/Formal', 'Enthusiastic/Energetic', 'Results-focused/Quantitative', 'Conversational/Personal');