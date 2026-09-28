package io.bookworm.api.payment.dto;

import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Member wallet response DTO.
 * <p>
 * Why: Transports current points balance, total rupee equivalent, and paginated transaction history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private BigDecimal pointsBalance;
    private MoneyDTO rupeeValue;
    private List<WalletTransactionDTO> transactions;
    private PaginationDTO pagination;
}
