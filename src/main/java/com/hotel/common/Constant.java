package com.hotel.common;

public class Constant {

    public static final String JWT_SECRET = "hotel-management-secret-key-2024";
    
    public static final long JWT_EXPIRATION = 86400000;
    
    public static final String TOKEN_PREFIX = "Bearer ";
    
    public static final String ADMIN_ROLE = "ADMIN";
    
    public static final String RECEPTIONIST_ROLE = "RECEPTIONIST";
    
    public static final String CLIENT_ROLE = "CLIENT";
    
    public static final Integer STATUS_ENABLE = 1;
    
    public static final Integer STATUS_DISABLE = 0;
    
    public static final String ROOM_STATUS_AVAILABLE = "AVAILABLE";
    
    public static final String ROOM_STATUS_OCCUPIED = "OCCUPIED";
    
    public static final String ROOM_STATUS_MAINTENANCE = "MAINTENANCE";
    
    public static final String ORDER_STATUS_UNPAID = "UNPAID";
    
    public static final String ORDER_STATUS_PAID = "PAID";
    
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
}
