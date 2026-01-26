package dev.auctoritas.auth.entity.project;

import dev.auctoritas.common.entity.BaseAuditEntity;
import dev.auctoritas.common.enums.ApiKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey extends BaseAuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, length = 10)
  private String prefix;

  @Column(nullable = false, length = 64)
  private String keyHash;

  @Column(nullable = true)
  private LocalDateTime lastUsedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApiKeyStatus status = ApiKeyStatus.ACTIVE;
}
