-- Run BEFORE deploying the new backend. Additive migration: existing records are preserved.
BEGIN;
ALTER TABLE Resumen ADD COLUMN IF NOT EXISTS resenaPublica VARCHAR(600) NULL;
COMMIT;
