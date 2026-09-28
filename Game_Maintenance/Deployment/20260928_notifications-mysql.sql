-- Migración aditiva para el centro de notificaciones (MySQL 8+).
CREATE TABLE IF NOT EXISTS Notificacion (
    idNotificacion BIGINT NOT NULL AUTO_INCREMENT,
    idDestinatario BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fechaCreacion DATETIME NOT NULL,
    idResumen BIGINT NULL,
    idImagen BIGINT NULL,
    idClienteRelacionado BIGINT NULL,
    PRIMARY KEY (idNotificacion),
    CONSTRAINT fk_notificacion_destinatario FOREIGN KEY (idDestinatario) REFERENCES Cliente(idCliente),
    CONSTRAINT fk_notificacion_resumen FOREIGN KEY (idResumen) REFERENCES Resumen(idResumen),
    CONSTRAINT fk_notificacion_imagen FOREIGN KEY (idImagen) REFERENCES Imagen(idImagen),
    INDEX idx_notificacion_destinatario_fecha (idDestinatario, fechaCreacion),
    INDEX idx_notificacion_destinatario_leida (idDestinatario, leida)
);
