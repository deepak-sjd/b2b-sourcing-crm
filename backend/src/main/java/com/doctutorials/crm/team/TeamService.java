package com.doctutorials.crm.team;

import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.security.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public Team create(@NotBlank String name, String description) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        // A team belongs to whichever organization its creator belongs to - there's no
        // cross-organization team creation today (that's a future multi-tenant-admin
        // feature, not something this endpoint needs to expose yet).
        team.setOrganization(currentUserService.get().getOrganization());
        return teamRepository.save(team);
    }

    public Team getById(UUID id) {
        return teamRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Team", id));
    }

    public List<Team> listAll() {
        return teamRepository.findAll();
    }
}
