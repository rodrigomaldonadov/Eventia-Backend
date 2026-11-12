package com.backend.eventia.model;

import com.backend.eventia.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.VALID;

    @Column(name = "holder_name")
    private String nombre;

    @Column(name = "holder_dni", length = 20)
    private String dni;

    @Column(name = "qr_code", unique = true, nullable = false, length = 64)
    private String qrCode;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order_detail", nullable = false)
    private OrderDetail orderDetail;

    @PrePersist
    public void prePersist() {
        if (this.qrCode == null || this.qrCode.isBlank()) {
            this.qrCode = java.util.UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = TicketStatus.VALID;
        }
    }
}
