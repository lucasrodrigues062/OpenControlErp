-- V1_0_4__create_spring_modulith_event_publication.sql
-- Table required by Spring Modulith JPA Event Publication Registry

CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_event_publication_event_type ON event_publication(event_type);
CREATE INDEX IF NOT EXISTS idx_event_publication_by_completion_date ON event_publication(completion_date);
