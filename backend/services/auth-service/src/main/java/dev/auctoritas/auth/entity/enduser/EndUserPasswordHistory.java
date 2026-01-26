package dev.auctoritas.auth.entity.enduser;

import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_history")
@Getter
@Setter
@NoArgsConstructor
public class EndUserPasswordHistory extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;
}
