-- Ejecutar una vez en la base de datos de Game Maintenance antes de desplegar
-- esta versión. Compatible con versiones de MySQL que no soportan
-- "ADD COLUMN IF NOT EXISTS".
ALTER TABLE Trabajo ADD COLUMN nombrePieza VARCHAR(160) NULL;
ALTER TABLE Trabajo ADD COLUMN unidades INT NULL;
ALTER TABLE Trabajo ADD COLUMN precioUnitario DOUBLE NULL;
