package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class QuoteStatus(val label: String) {
    Draft("Draft"),
    Sent("Sent"),
    Accepted("Accepted"),
    Declined("Declined");

    companion object {
        fun fromString(value: String?): QuoteStatus {
            return entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: Draft
        }
    }
}

@JsonClass(generateAdapter = true)
data class Workspace(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "company_name") val companyName: String,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "brand_color") val brandColor: String = "#0f172a",
    @Json(name = "currency") val currency: String = "TZS",
    @Json(name = "tax_rate") val taxRate: Double = 18.0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkspaceCreateRequest(
    @Json(name = "user_id") val userId: String,
    @Json(name = "company_name") val companyName: String,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "brand_color") val brandColor: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "tax_rate") val taxRate: Double
)

@JsonClass(generateAdapter = true)
data class WorkspaceUpdateRequest(
    @Json(name = "company_name") val companyName: String,
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "brand_color") val brandColor: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "tax_rate") val taxRate: Double
)

@JsonClass(generateAdapter = true)
data class Client(
    @Json(name = "id") val id: String,
    @Json(name = "workspace_id") val workspaceId: String,
    @Json(name = "client_name") val clientName: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ClientUpsertRequest(
    @Json(name = "workspace_id") val workspaceId: String,
    @Json(name = "client_name") val clientName: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "address") val address: String? = null
)

@JsonClass(generateAdapter = true)
data class LibraryItem(
    @Json(name = "id") val id: String,
    @Json(name = "workspace_id") val workspaceId: String,
    @Json(name = "item_name") val itemName: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "unit_price") val unitPrice: Double = 0.0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LibraryItemUpsertRequest(
    @Json(name = "workspace_id") val workspaceId: String,
    @Json(name = "item_name") val itemName: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "unit_price") val unitPrice: Double
)

@JsonClass(generateAdapter = true)
data class Quote(
    @Json(name = "id") val id: String,
    @Json(name = "workspace_id") val workspaceId: String,
    @Json(name = "client_id") val clientId: String? = null,
    @Json(name = "quote_number") val quoteNumber: String,
    @Json(name = "status") val status: String = "Draft",
    @Json(name = "valid_until") val validUntil: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "payment_instructions") val paymentInstructions: String? = null,
    @Json(name = "subtotal") val subtotal: Double = 0.0,
    @Json(name = "tax_amount") val taxAmount: Double = 0.0,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "total_amount") val totalAmount: Double = 0.0,
    @Json(name = "signature_data_url") val signatureDataUrl: String? = null,
    @Json(name = "signed_by_name") val signedByName: String? = null,
    @Json(name = "signed_at") val signedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
) {
    val quoteStatus: QuoteStatus get() = QuoteStatus.fromString(status)
}

@JsonClass(generateAdapter = true)
data class QuoteUpsertRequest(
    @Json(name = "workspace_id") val workspaceId: String,
    @Json(name = "client_id") val clientId: String?,
    @Json(name = "quote_number") val quoteNumber: String,
    @Json(name = "status") val status: String,
    @Json(name = "valid_until") val validUntil: String?,
    @Json(name = "notes") val notes: String?,
    @Json(name = "payment_instructions") val paymentInstructions: String?,
    @Json(name = "subtotal") val subtotal: Double,
    @Json(name = "tax_amount") val taxAmount: Double,
    @Json(name = "discount_amount") val discountAmount: Double,
    @Json(name = "total_amount") val totalAmount: Double
)

@JsonClass(generateAdapter = true)
data class QuoteAcceptRequest(
    @Json(name = "status") val status: String = "Accepted",
    @Json(name = "signature_data_url") val signatureDataUrl: String,
    @Json(name = "signed_by_name") val signedByName: String,
    @Json(name = "signed_at") val signedAt: String
)

@JsonClass(generateAdapter = true)
data class QuoteDeclineRequest(
    @Json(name = "status") val status: String = "Declined"
)

@JsonClass(generateAdapter = true)
data class QuoteItem(
    @Json(name = "id") val id: String,
    @Json(name = "quote_id") val quoteId: String,
    @Json(name = "item_name") val itemName: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "quantity") val quantity: Double = 1.0,
    @Json(name = "unit_price") val unitPrice: Double = 0.0,
    @Json(name = "total_price") val totalPrice: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class QuoteItemInsertRequest(
    @Json(name = "quote_id") val quoteId: String,
    @Json(name = "item_name") val itemName: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "quantity") val quantity: Double,
    @Json(name = "unit_price") val unitPrice: Double,
    @Json(name = "total_price") val totalPrice: Double
)

data class PublicQuoteBundle(
    val quote: Quote,
    val items: List<QuoteItem>,
    val client: Client?,
    val workspace: Workspace?
)

data class EditableLineItem(
    val localId: String,
    val itemName: String = "",
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0"
) {
    val qtyValue: Double get() = quantity.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val unitPriceValue: Double get() = unitPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val totalPriceValue: Double get() = qtyValue * unitPriceValue
}

// Supabase Auth Models
@JsonClass(generateAdapter = true)
data class AuthCredentialsRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: AuthUser? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseErrorResponse(
    @Json(name = "code") val code: String? = null,
    @Json(name = "details") val details: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "hint") val hint: String? = null
)

data class SupabaseConfig(
    val url: String = "",
    val key: String = ""
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() &&
            (url.startsWith("https://") || url.startsWith("http://")) &&
            key.isNotBlank()
}

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)
