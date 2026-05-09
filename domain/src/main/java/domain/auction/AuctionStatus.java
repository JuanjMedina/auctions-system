package domain.auction;

public enum AuctionStatus {
  DRAFT, // creada, no visible al publico
  SCHEDULED, // publicada, starts_at en el futuro
  ACTIVE, // en curso, acepta pujas
  EXTENDED, // extendida por puja tardia (anti-sniping)
  CLOSED, // tiempo expirado, pendiente de adjudicacion
  AWARDED, // ganador confirmado, pago pendiente
  PAID, // pago completado
  CANCELLED, // cancelada
  FAILED // cerrada sin pujas o sin alcanzar reserve_price
}
