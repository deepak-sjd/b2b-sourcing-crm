package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.doctutorials.crm.lead.LeadStage.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeadStageRulesTest {

    private void move(LeadStage from, LeadStage to, boolean contact, boolean req, boolean quotationFlow) {
        LeadStageRules.validateMove(from, to, contact, req, quotationFlow);
    }

    @Test
    void forwardJumpsAreAllowedWhenDataIsPresent() {
        assertThatCode(() -> move(NEW, OPPORTUNITY, true, true, false)).doesNotThrowAnyException();
        assertThatCode(() -> move(NEW, CONTACTED, false, false, false)).doesNotThrowAnyException();
    }

    @Test
    void backwardMovesAreLimitedToOneStage() {
        assertThatCode(() -> move(OPPORTUNITY, MEETING, true, true, false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> move(OPPORTUNITY, QUALIFIED, true, true, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void quotationStageIsOnlyEnteredByTheQuotationFlowAndOnlyLeftByClosing() {
        assertThatThrownBy(() -> move(OPPORTUNITY, QUOTATION, true, true, false))
                .isInstanceOf(BadRequestException.class);
        assertThatCode(() -> move(NEW, QUOTATION, false, false, true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> move(QUOTATION, OPPORTUNITY, true, true, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void closedLeadsCannotMoveAndClosingGoesThroughItsOwnAction() {
        assertThatThrownBy(() -> move(WON, NEW, true, true, false)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> move(LOST, NEW, true, true, false)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> move(NEW, WON, true, true, false)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> move(NEW, LOST, true, true, false)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void qualifiedNeedsContactDetailsAndOpportunityNeedsARequirement() {
        assertThatThrownBy(() -> move(NEW, QUALIFIED, false, false, false)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> move(CONTACTED, OPPORTUNITY, true, false, false)).isInstanceOf(BadRequestException.class);
        assertThatCode(() -> move(CONTACTED, QUALIFIED, true, false, false)).doesNotThrowAnyException();
    }

    @Test
    void wonRequiresAPositiveFinalValue() {
        assertThatThrownBy(() -> LeadStageRules.requireFinalValue(null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> LeadStageRules.requireFinalValue(BigDecimal.ZERO)).isInstanceOf(BadRequestException.class);
        assertThatCode(() -> LeadStageRules.requireFinalValue(new BigDecimal("1000"))).doesNotThrowAnyException();
    }
}