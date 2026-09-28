-- Migración aditiva para el centro de notificaciones (PostgreSQL).
BEGIN;
CREATE TABLE IF NOT EXISTS Notificacion (
    idNotificacion BIGSERIAL PRIMARY KEY,
    idDestinatario BIGINT NOT NULL REFERENCES Cliente(idCliente),
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fechaCreacion TIMESTAMP NOT NULL,
    idResumen BIGINT NULL REFERENCES Resumen(idResumen),
    idImagen BIGINT NULL REFERENCES Imagen(idImagen),
    idClienteRelacionado BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_notificacion_destinatario_fecha
    ON Notificacion (idDestinatario, fechaCreacion DESC);
CREATE INDEX IF NOT EXISTS idx_notificacion_destinatario_leida
    ON Notificacion (idDestinatario, leida);
COMMIT;
