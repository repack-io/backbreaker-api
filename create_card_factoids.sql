-- Create card_factoids table
-- One-to-one with card_details, stores AI-generated prospect blurb data
CREATE TABLE IF NOT EXISTS card_factoids (
    id             BIGSERIAL PRIMARY KEY,
    card_detail_id BIGINT NOT NULL UNIQUE REFERENCES card_details(id),
    factoid        TEXT
);
