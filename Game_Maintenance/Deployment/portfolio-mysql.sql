-- Run once BEFORE deploying if using MySQL, after selecting ticketsmantenimiento.
-- Inspect first: SHOW COLUMNS FROM Resumen LIKE 'resenaPublica';
-- Only run the following statement if the column is absent.
ALTER TABLE Resumen ADD COLUMN resenaPublica VARCHAR(600) NULL;
