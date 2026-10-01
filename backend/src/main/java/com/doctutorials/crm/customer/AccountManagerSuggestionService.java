package com.doctutorials.crm.customer;

import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI automation #3 — "suggest, don't decide". Recommends whichever eligible
 * user currently manages the fewest active customers, so new customers
 * don't all pile onto whoever happens to be first in a dropdown. Returned to
 * the frontend as a dismissible chip next to the Account Manager field; the
 * caller still has to accept it (or pick someone else) — this never assigns
 * anything on its own.
 */
@Service
@RequiredArgsConstructor
public class AccountManagerSuggestionService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public record Suggestion(UUID userId, String fullName, long currentLoad) {}

    public Optional<Suggestion> suggest() {
        List<User> eligible = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() == Role.COUNSELOR || u.getRole() == Role.MANAGER)
                .toList();

        return eligible.stream()
                .map(u -> new Suggestion(
                        u.getId(),
                        u.getFullName(),
                        customerRepository.countByAccountManagerIdAndActiveTrue(u.getId())))
                .min(Comparator.comparingLong(Suggestion::currentLoad));
    }
}
