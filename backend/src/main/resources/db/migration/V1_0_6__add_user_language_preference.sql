-- V1_0_6__add_user_language_preference.sql
-- Add preferred language support to iam_user table for cross-device localization

ALTER TABLE iam_user
    ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'en-US';

COMMENT ON COLUMN iam_user.preferred_language IS 'ISO language tag for user preferred UI language (e.g. en-US, pt-BR)';
