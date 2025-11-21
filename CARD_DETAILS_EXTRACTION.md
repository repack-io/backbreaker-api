# Card Details Extraction Feature

This feature uses AWS Bedrock (Claude 3.5 Sonnet) to extract detailed information from trading card images using AI vision analysis.

## Overview

The card details extraction system analyzes both the front and back images of trading cards to extract:
- Player name (first and last)
- Team name
- Parallel type (Base, Refractor, etc.)
- Serial number (if numbered)
- Card metadata (year, set, number)
- Special features (rookie card, autograph, memorabilia)

The extracted information is stored in a structured format with proper deduplication of players and teams.

## Database Schema

### Tables Created

**`players`** - Sports players extracted from cards
- `id` - Primary key
- `first_name` - Player's first name
- `last_name` - Player's last name
- `card_category_type_id` - Card category (1=baseball, 2=football, 3=basketball, 4=hockey)
- `created_at` - Timestamp

**`teams`** - Sports teams extracted from cards
- `id` - Primary key
- `name` - Team name
- `card_category_type_id` - Card category
- `created_at` - Timestamp

**`card_details`** - Detailed card information
- `id` - Primary key
- `series_card_id` - Foreign key to series_cards (unique)
- `player_id` - Foreign key to players
- `team_id` - Foreign key to teams (nullable)
- `parallel_type` - Type of parallel (Refractor, Chrome, etc.)
- `serial_number` - Serial number if card is numbered
- `card_category_type_id` - Card category
- `card_status_id` - Status (1=available, etc.)
- `product_tier_id` - Foreign key to product_tiers
- `hit_date` - Date card was pulled
- `usd_value` - Estimated value in USD
- `created_at`, `updated_at` - Timestamps

## API Endpoint

### POST `/api/cards/extract-details`

Extract card details from a series card's front and back images.

**Request:**
```json
{
  "series_card_id": 123,
  "card_category_type": "baseball"
}
```

**Response (201 Created):**
```json
{
  "card_detail_id": 1,
  "series_card_id": 123,
  "player_id": 5,
  "player_name": "Mike Trout",
  "team_id": 3,
  "team_name": "Los Angeles Angels",
  "parallel_type": "Refractor",
  "serial_number": "45/99",
  "card_status_id": 1,
  "hit_date": null,
  "usd_value": null,
  "created_at": "2025-11-19T14:30:00",
  "extracted_data": {
    "player_first_name": "Mike",
    "player_last_name": "Trout",
    "team_name": "Los Angeles Angels",
    "parallel_type": "Refractor",
    "serial_number": "45/99",
    "card_year": "2023",
    "card_set": "Topps Chrome",
    "card_number": "27",
    "rookie_card": false,
    "autograph": false,
    "memorabilia": false,
    "confidence": "high"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request (missing series_card_id, invalid category)
- `404 Not Found` - Card not found
- `409 Conflict` - Card details already exist for this card
- `500 Internal Server Error` - Image download or AI analysis failed

## Valid Card Categories

- `baseball` (ID: 1)
- `football` (ID: 2)
- `basketball` (ID: 3)
- `hockey` (ID: 4)

## How It Works

1. **Validates Request** - Checks that the series card exists and category is valid
2. **Checks for Duplicates** - Returns error if card details already extracted
3. **Downloads Images** - Fetches front and back card images from S3 or HTTP
4. **AI Analysis** - Sends both images to Bedrock Claude 3.5 Sonnet with specialized prompt
5. **Extracts Data** - Parses JSON response from AI
6. **Deduplicates** - Gets or creates player and team records
7. **Saves Details** - Creates card_details record with all extracted information
8. **Returns Response** - Sends back complete card details with extracted metadata

## Configuration

The service uses the Bedrock model configuration from `application.properties`:

```properties
# Card details extraction use case
bedrock.models.card-details-extraction.model-id=claude-sonnet
bedrock.models.card-details-extraction.max-tokens=1024
bedrock.models.card-details-extraction.temperature=0.1
```

## AI Prompt

The system sends both card images to Claude with a detailed prompt that requests:
- Player identification (first and last name)
- Team identification (full name)
- Parallel type detection
- Serial number extraction (if numbered)
- Card metadata (year, set, number)
- Special feature detection (RC, auto, relic)
- Confidence level (high/medium/low)

The AI returns structured JSON that matches the `ExtractedCardData` DTO.

## Implementation Classes

**Models:**
- `Player` - JPA entity for players
- `Team` - JPA entity for teams
- `CardDetail` - JPA entity for card details

**Repositories:**
- `PlayerRepository` - Player data access with deduplication
- `TeamRepository` - Team data access with deduplication
- `CardDetailRepository` - Card details data access

**Services:**
- `CardDetailsExtractionService` - Core extraction logic

**Controllers:**
- `CardDetailsController` - REST API endpoint

**DTOs:**
- `CardDetailsExtractionRequest` - API request
- `CardDetailsExtractionResponse` - API response
- `ExtractedCardData` - Raw AI extraction results

## Usage Example

```bash
curl -X POST http://localhost:8080/api/cards/extract-details \
  -H "Content-Type: application/json" \
  -d '{
    "series_card_id": 123,
    "card_category_type": "baseball"
  }'
```

## Notes

- Card details can only be extracted once per series_card_id
- Players and teams are automatically deduplicated by name and category
- The system handles both S3 URLs and HTTP URLs for card images
- Extraction uses multi-image Bedrock support for improved accuracy
- Default card status is set to 1 (available)
- The `hit_date` and `usd_value` fields default to null and can be updated later

## Migration from Repackio

This feature was originally implemented in the `repackio` Python Lambda project but has been properly moved to the `backbreaker` Java Spring Boot API where it belongs. The Python implementation has been removed.
