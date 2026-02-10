package net.devstudy.resume.staticdata.api.model;

import java.io.Serial;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "skill_category")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class SkillCategory extends AbstractEntity<Long> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SKILL_CATEGORY_ID_GENERATOR", sequenceName = "skill_category_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SKILL_CATEGORY_ID_GENERATOR")
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String category;

    public SkillCategory() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
