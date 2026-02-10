package net.devstudy.resume.staticdata.api.model;

import java.io.Serial;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "hobby")
public class Hobby extends AbstractEntity<Long> implements Comparable<Hobby> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String name;

    @Transient
    private boolean selected;

    public Hobby() {
    }

    public Hobby(String name) {
        this.name = name;
    }

    public Hobby(String name, boolean selected) {
        this.name = name;
        this.selected = selected;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Transient
    public String getCssClassName() {
        return name == null ? "" : name.replace(" ", "-").toLowerCase();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Hobby other)) return false;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public int compareTo(Hobby o) {
        if (o == null || this.name == null) return 1;
        if (o.name == null) return -1;
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return "Hobby[name=%s]".formatted(name);
    }
}
