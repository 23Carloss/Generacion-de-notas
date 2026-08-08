
export const PLATAFORMAS = {
  XBOX: "Xbox",
  PLAYSTATION: "PlayStation",
  PC: "PC",
  LAPTOP: "Laptop",
};
 
export const TIPOS_TRABAJO = {
  REPARACION: "Reparación",
  DIAGNOSTICO: "Diagnóstico",
  CHIPEO: "Chipeo",
  INSTALACION_JUEGOS: "Instalación de juegos",
  MANTENIMIENTO: "Mantenimiento",
};
 
// Estado de taller: campo de conveniencia manejado en el front (aún no existe
// en el modelo Resumen de la BD). Si lo quieren persistir, agregar una
// columna "estado" a la entidad Resumen y a ResumenDTO.
export const ESTADOS = [
  "Recibido",
  "En diagnóstico",
  "En reparación",
  "Listo para entrega",
  "Entregado",
];
 
export const ESTADO_BADGE_CLASS = {
  Recibido: "badge-recibido",
  "En diagnóstico": "badge-diagnostico",
  "En reparación": "badge-reparacion",
  "Listo para entrega": "badge-listo",
  Entregado: "badge-entregado",
};
 
// Mismo enum que hp.models.Cliente.ROL / DTOs.ClienteDTO.ROL en el backend.
export const ROLES = {
  ADMINISTRADOR: "Administrador",
  USUARIO: "Usuario",
};