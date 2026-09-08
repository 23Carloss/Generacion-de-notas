-- Read-only: run in the SAME database used by DB_URL in the Render API service.
SELECT current_database() AS base_actual, current_schema() AS esquema_actual;

-- Counts only: distinguish empty tables from a failed API query, without sharing personal data.
SELECT COUNT(*) AS total_clientes FROM Cliente;
SELECT COUNT(*) AS total_tickets FROM Resumen;

SELECT table_schema, table_name, column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE lower(table_name) = 'resumen' AND lower(column_name) = 'resenapublica';

-- This must succeed even when there are no tickets. It returns no customer records.
SELECT resenaPublica FROM Resumen WHERE 1 = 0;
