package tefbanesco

// Prefijo común para las acciones de Intent
const val ACTION_PREFIX = "icg.actions.electronicpayment.tefbanesco."

// Acciones explícitas
const val ACTION_INITIALIZE    = ACTION_PREFIX + "INITIALIZE"
const val ACTION_GET_BEHAVIOR  = ACTION_PREFIX + "GET_BEHAVIOR"
const val ACTION_TRANSACTION   = ACTION_PREFIX + "TRANSACTION"

/** Claves (extras) que maneja el módulo según API v3.7 **/
object ExtraKeys {
    // INITIALIZE
    const val PARAMETERS              = "Parameters"
    const val TOKEN                   = "Token"
    const val ERROR_MESSAGE           = "ErrorMessage"

    // GET_BEHAVIOR
    const val SUPPORTS_TRANSACTION_VOID        = "SupportsTransactionVoid"
    const val SUPPORTS_TRANSACTION_QUERY       = "SupportsTransactionQuery"
    const val SUPPORTS_NEGATIVE_SALES          = "SupportsNegativeSales"
    const val SUPPORTS_PARTIAL_REFUND          = "SupportsPartialRefund"
    const val SUPPORTS_BATCH_CLOSE             = "SupportsBatchClose"
    const val SUPPORTS_TIP_ADJUSTMENT          = "SupportsTipAdjustment"
    const val ONLY_CREDIT_FOR_TIP_ADJUSTMENT   = "OnlyCreditForTipAdjustment"
    const val SUPPORTS_CREDIT                  = "SupportsCredit"
    const val SUPPORTS_DEBIT                   = "SupportsDebit"
    const val SUPPORTS_EBT_FOODSTAMP           = "SupportsEBTFoodstamp"
    const val HAS_CUSTOM_PARAMS                = "HasCustomParams"
    const val CAN_CHARGE_CARD                  = "CanChargeCard"
    const val CAN_AUDIT                        = "canAudit" // Nótese la minúscula inicial, según documentación
    const val EXECUTE_VOID_WHEN_AVAILABLE      = "ExecuteVoidWhenAvailable"
    const val SAVE_LOYALTY_CARD_NUM            = "SaveLoyaltyCardNum"
    const val CAN_PRINT                        = "CanPrint"
    const val READ_CARD_FROM_API               = "ReadCardFromApi"
    const val ONLY_USE_DOCUMENT_PATH           = "OnlyUseDocumentPath" // Nuevo en API v3.7

    // TRANSACTION entrada
    const val TRANSACTION_TYPE                 = "TransactionType"
    const val AMOUNT                           = "Amount"
    const val CURRENCY_ISO                     = "CurrencyISO"
    const val LANGUAGE_ISO                     = "LanguageISO"
    const val TRANSACTION_ID_HIO               = "TransactionId"
    const val SHOP_DATA                        = "ShopData"
    const val SELLER_DATA                      = "SellerData"
    const val TAX_DETAIL                       = "TaxDetail"
    const val RECEIPT_PRINTER_COLUMNS          = "ReceiptPrinterColumns"
    const val DOCUMENT_DATA                    = "DocumentData"
    const val DOCUMENT_PATH                    = "DocumentPath"
    const val OVER_PAYMENT_TYPE                = "OverPaymentType"

    // TRANSACTION salida
    const val TRANSACTION_RESULT               = "TransactionResult"
    const val TRANSACTION_DATA_MODULE          = "TransactionData"
    const val AUTHORIZATION_ID                 = "AuthorizationId"
    const val CARD_HOLDER                      = "CardHolder"
    const val CARD_TYPE                        = "CardType"
    const val CARD_NUM                         = "CardNum"
    const val ERROR_MESSAGE_TITLE              = "ErrorMessageTitle"
    const val DOCUMENT_ID                      = "DocumentId"
}