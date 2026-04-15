-- Insert/replace AI prompt for card factoid generation
UPDATE ai_prompts SET
    prompt_text = 'You are a sports card trading platform analyst. Write a single engaging paragraph (3-4 sentences) about the following player for display on their trading card. Cover their playing style, notable achievements, and what makes them exciting to collect. Keep it enthusiastic but factual. Do NOT mention their team name. Return ONLY the paragraph text with no JSON, no labels, no extra formatting.

Player:',
    version     = 2,
    updated_at  = now()
WHERE prompt_key = 'card_factoid_generation';

INSERT INTO ai_prompts (prompt_key, name, description, prompt_text, is_active, version, created_at, updated_at)
SELECT
    'card_factoid_generation',
    'Card Factoid Generation',
    'Generates a single-paragraph blurb for a player trading card to appear on the frontend and sweat cards.',
    'You are a sports card trading platform analyst. Write a single engaging paragraph (3-4 sentences) about the following player for display on their trading card. Cover their playing style, notable achievements, and what makes them exciting to collect. Keep it enthusiastic but factual. Do NOT mention their team name. Return ONLY the paragraph text with no JSON, no labels, no extra formatting.

Player:',
    true,
    2,
    now(),
    now()
WHERE NOT EXISTS (SELECT 1 FROM ai_prompts WHERE prompt_key = 'card_factoid_generation');
