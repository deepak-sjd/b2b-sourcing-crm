package com.doctutorials.crm.note;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notes", indexes = {
        @Index(name = "idx_notes_lead_id", columnList = "lead_id")
})
@Getter
@Setter
public class Note extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @NotBlank
    @Column(length = 4000)
    private String content;
}
