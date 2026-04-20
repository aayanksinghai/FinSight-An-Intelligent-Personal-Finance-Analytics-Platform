ALTER TABLE transactions ADD COLUMN content_hash CHAR(64);

-- Backfill existing data with placeholder hashes (using their UUIDs to maintain uniqueness temporarily, or random)
UPDATE transactions 
SET content_hash = md5(id::text) || md5((id::text || 'part2'))
WHERE content_hash IS NULL;

-- Now add the unique constraint (partial index handles nulls, but we will make it NOT NULL later if needed, here just UNIQUE)
CREATE UNIQUE INDEX uq_transactions_content_hash ON transactions(content_hash) WHERE content_hash IS NOT NULL;
