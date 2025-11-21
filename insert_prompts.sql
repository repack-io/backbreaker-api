-- Insert script for migrating prompts from text files to ai_prompts table
-- Run this script after the ai_prompts table has been created

-- Prompt 1: Card Crop
INSERT INTO ai_prompts (prompt_key, name, description, prompt_text, version, is_active)
VALUES (
    'card_crop',
    'Card Crop Detection',
    'Detects crop boundaries for sports trading card images, identifying container types (rigid case, sleeve, or bare card) and returning tight bounding box coordinates.',
    'You are detecting crop boundaries for a sports trading card image.

TASK: Identify the edges to crop to. Return coordinates as relative values (0.0 to 1.0).

CONTAINER IDENTIFICATION:
- RIGID CASE (PSA/BGS/SGC): Thick plastic case with certification label → crop to outer case edges
- SLEEVE/TOPLOADER: Thin/semi-rigid plastic → crop to card edges only
- BARE CARD: No protection → crop to card edges

CROPPING REQUIREMENTS:
Your bounding box must be TIGHT on ALL FOUR SIDES (top, bottom, left, right).

For rigid cases:
- Minimize background on ALL sides
- Case should occupy 85-95% of the cropped area
- Left and right margins should be just as small as top and bottom margins

For sleeves/bare cards:
- Crop tightly to the printed card edges on ALL four sides
- Ignore any plastic sleeve borders
- Card should occupy 70-85% of the cropped area

COMMON MISTAKE TO AVOID:
❌ DON''T crop tight on top/bottom but leave wide margins on left/right
✅ DO crop equally tight on all four sides

COORDINATE SYSTEM:
Relative to original image dimensions (0.0 to 1.0):
- left: distance from left edge / image_width
- top: distance from top edge / image_height
- width: cropped_width / image_width
- height: cropped_height / image_height

VALIDATION:
Before finalizing, verify:
- Is the left margin approximately equal to the right margin?
- Is the top margin approximately equal to the bottom margin?
- Are all four edges cropped as tightly as possible?

EXACT JSON OUTPUT (no markdown, no extra text):
{
  "bounding_box": {
    "left": 0.05,
    "top": 0.03,
    "width": 0.90,
    "height": 0.94
  },
  "confidence": 92,
  "container_type": "rigid_case",
  "reasoning": "PSA case detected by label and thick borders. Cropped tightly to case edges on all four sides with minimal background."
}

container_type must be: "rigid_case" OR "flexible_sleeve" OR "bare_card"

EXAMPLE - Rigid PSA case:
If image is 1000x1400 pixels and PSA case spans pixels 50-950 horizontally and 30-1370 vertically:
{
  "bounding_box": {
    "left": 0.05,
    "top": 0.021,
    "width": 0.90,
    "height": 0.957
  },
  "confidence": 95,
  "container_type": "rigid_case",
  "reasoning": "PSA case with equal margins on all sides. Left margin 5%, right margin 5%, top margin 2%, bottom margin 2%."
}',
    1,
    true
);

-- Prompt 2: Card Details Extraction
INSERT INTO ai_prompts (prompt_key, name, description, prompt_text, version, is_active)
VALUES (
    'card_details_extraction',
    'Card Details Extraction',
    'Analyzes trading card images (front and back) to extract player information, team, parallel type, serial number, card year, and estimated value.',
    'Analyze these trading card images (front and back) and extract the following information in JSON format.

Please identify:
1. Name of person or character (first and last name if possible)
2. Card category, for example: "baseball", "football", "basketball", "hockey", "other", "unknown"
3. Team name, if applicable (sports cards, full team name, e.g., "New York Yankees" not "NYY", or "N/A")
4. Parallel type (if any) - examples: "Base", "Refractor", "Chrome", "Gold", "Silver", "Rainbow", "Auto", "Relic", etc.
5. Serial number (if visible)
6. Estimated USD value range, based on available information from ebay, breaker platforms, or elsewhere
7. Card year, if it spans 2 years, example 2022-2023, only use the lowest value year

Return ONLY valid JSON in this exact format:

{
	"player_first_name": "string",
    "player_last_name": "string",
    "card_category": "string"
    "team_name": "string",
    "parallel_type": "string or null",
    "serial_number": "string or null",
    "card_year": "string or null",
    "usd_value_range": "string or null",
    "confidence": "high or medium or low"
}

If you cannot determine a value with confidence, use null for strings or false for booleans.
Set confidence to "high" if you''re very certain, "medium" if somewhat certain, "low" if uncertain.',
    1,
    true
);

-- Verify inserts
SELECT id, prompt_key, name, version, is_active, created_at
FROM ai_prompts
ORDER BY id;
