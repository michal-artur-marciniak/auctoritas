package dev.auctoritas.auth.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@MappedSuperclass
public abstract class BaseAuditEntity extends BaseEntity {

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
