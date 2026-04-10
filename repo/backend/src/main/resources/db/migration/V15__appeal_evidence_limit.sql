-- Enforce maximum 5 evidence files per appeal at the database level
-- Using a trigger since CHECK constraints can't reference other rows
CREATE OR REPLACE FUNCTION check_appeal_evidence_limit() RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM appeal_evidence WHERE appeal_id = NEW.appeal_id) >= 5 THEN
        RAISE EXCEPTION 'Maximum 5 evidence files per appeal (appeal_id=%)', NEW.appeal_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_appeal_evidence_limit ON appeal_evidence;
CREATE TRIGGER trg_appeal_evidence_limit
    BEFORE INSERT ON appeal_evidence
    FOR EACH ROW EXECUTE FUNCTION check_appeal_evidence_limit();
