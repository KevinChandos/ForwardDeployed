package io.bookworm.api.payment.mapper;

import io.bookworm.api.catalogue.mapper.CatalogueMapper;
import io.bookworm.api.common.dto.MoneyDTO;
import io.bookworm.api.common.dto.PaginationDTO;
import io.bookworm.api.payment.domain.PaymentTransaction;
import io.bookworm.api.payment.domain.WalletAccount;
import io.bookworm.api.payment.domain.WalletTransaction;
import io.bookworm.api.payment.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for Payment & Wallet bounded context.
 * <p>
 * Why: Converts PaymentTransaction, WalletAccount, and WalletTransaction entities into REST DTOs with null safety.
 */
@Mapper(
    componentModel = "spring",
    uses = {CatalogueMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "transaction.order.orderId")
    @Mapping(target = "amount", source = "transaction.amount", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "paymentMethod", source = "transaction.paymentMethod")
    @Mapping(target = "status", source = "transaction.status")
    @Mapping(target = "gatewayTransactionId", source = "transaction.gatewayPaymentId")
    @Mapping(target = "failureReason", source = "transaction.failureReason")
    PaymentDetailResponse toPaymentDetailResponse(PaymentTransaction transaction);

    @Mapping(target = "transactionId", source = "transaction.transactionId")
    @Mapping(target = "gatewayOrderId", source = "transaction.gatewayOrderId")
    @Mapping(target = "paymentUrl", source = "paymentUrl")
    @Mapping(target = "amount", source = "transaction.amount", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "status", source = "transaction.status")
    PaymentInitiatedResponse toPaymentInitiatedResponse(PaymentTransaction transaction, String paymentUrl);

    @Mapping(target = "transactionId", source = "transaction.transactionId")
    @Mapping(target = "status", source = "transaction.status")
    @Mapping(target = "orderId", source = "transaction.order.orderId")
    @Mapping(target = "orderNumber", source = "transaction.order.orderNumber")
    @Mapping(target = "failureReason", source = "transaction.failureReason")
    PaymentResultResponse toPaymentResultResponse(PaymentTransaction transaction);

    @Mapping(target = "transactionId", source = "walletTransaction.transactionId")
    @Mapping(target = "type", source = "walletTransaction.type")
    @Mapping(target = "points", source = "walletTransaction.points")
    @Mapping(target = "rupeeValue", source = "walletTransaction.rupeeValue", qualifiedByName = "amountToMoneyDTO")
    @Mapping(target = "orderId", source = "walletTransaction.order.orderId")
    @Mapping(target = "description", source = "walletTransaction.description")
    @Mapping(target = "createdAt", source = "walletTransaction.createdAt")
    WalletTransactionDTO toWalletTransactionDTO(WalletTransaction walletTransaction);

    List<WalletTransactionDTO> toWalletTransactionDTOList(List<WalletTransaction> walletTransactions);

    @Mapping(target = "pointsBalance", source = "wallet.balancePoints")
    @Mapping(target = "rupeeValue", source = "rupeeValue")
    @Mapping(target = "transactions", source = "transactions")
    @Mapping(target = "pagination", source = "pagination")
    WalletResponse toWalletResponse(WalletAccount wallet, MoneyDTO rupeeValue, List<WalletTransactionDTO> transactions, PaginationDTO pagination);

    @Named("amountToMoneyDTO")
    default MoneyDTO amountToMoneyDTO(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return MoneyDTO.builder()
                .amount(amount.toPlainString())
                .currency("INR")
                .build();
    }
}
