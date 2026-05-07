package domain.bid;

public enum BidStatus {
    ACTIVE,     // puja vigente, puede ser superada
    OUTBID,     // fue superada por otra puja
    WINNING,    // puja ganadora al cierre
    CANCELLED   // cancelada (caso excepcional)
}
