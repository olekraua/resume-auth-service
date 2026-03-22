package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import net.devstudy.resume.shared.model.AbstractEntity;

@MappedSuperclass
public abstract class AbstractFinishDateEntity<T> extends AbstractEntity<T> {
    @Serial
    private static final long serialVersionUID = -3388293457711051284L;

    @Column(name = "finish_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate finishDate;

    @Transient
    private Integer finishDateMonth;

    @Transient
    private Integer finishDateYear;

    public LocalDate getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(LocalDate finishDate) {
        this.finishDate = finishDate;
    }

    @Transient
    public boolean isFinish() {
        return finishDate != null;
    }

    @Transient
    public Integer getFinishDateMonth() {
        return (finishDate != null) ? finishDate.getMonthValue() : null;
    }

    @Transient
    public Integer getFinishDateYear() {
        return (finishDate != null) ? finishDate.getYear() : null;
    }

    public void setFinishDateMonth(Integer finishDateMonth) {
        this.finishDateMonth = finishDateMonth;
        setupFinishDate();
    }

    public void setFinishDateYear(Integer finishDateYear) {
        this.finishDateYear = finishDateYear;
        setupFinishDate();
    }

    private void setupFinishDate() {
        if (finishDateYear != null && finishDateMonth != null) {
            setFinishDate(LocalDate.of(finishDateYear, finishDateMonth, 1));
        } else {
            setFinishDate(null);
        }
    }
}


