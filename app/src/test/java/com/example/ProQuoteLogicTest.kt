package com.example

import com.example.data.model.EditableLineItem
import com.example.data.model.Quote
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.ProQuoteUiState
import com.example.ui.viewmodel.QuoteBuilderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProQuoteLogicTest {

    @Test
    fun nextQuoteNumber_incrementsFromExistingQuotes() {
        assertEquals("QT-1001", Formatters.nextQuoteNumber(emptyList()))
        val existing = listOf(
            Quote(id = "1", workspaceId = "w1", quoteNumber = "QT-1001"),
            Quote(id = "2", workspaceId = "w1", quoteNumber = "QT-1004")
        )
        assertEquals("QT-1005", Formatters.nextQuoteNumber(existing))
    }

    @Test
    fun quoteBuilder_calculatesSubtotalDiscountTaxAndTotal() {
        val builder = QuoteBuilderState(
            lineItems = listOf(
                EditableLineItem("1", "Design Sprint", "", "2", "500"),
                EditableLineItem("2", "API Integration", "", "1", "1000")
            ),
            discountText = "200",
            taxEnabled = true,
            taxRateText = "18.0"
        )
        assertEquals(2000.0, builder.subtotal, 0.001)
        assertEquals(200.0, builder.discountAmount, 0.001)
        assertEquals(1800.0, builder.taxableBase, 0.001)
        assertEquals(324.0, builder.taxAmount, 0.001)
        assertEquals(2124.0, builder.grandTotal, 0.001)
    }

    @Test
    fun dashboardMetrics_computesConversionRevenueAndPipeline() {
        val quotes = listOf(
            Quote(id = "1", workspaceId = "w1", quoteNumber = "QT-1001", status = "Draft", totalAmount = 100.0),
            Quote(id = "2", workspaceId = "w1", quoteNumber = "QT-1002", status = "Sent", totalAmount = 400.0),
            Quote(id = "3", workspaceId = "w1", quoteNumber = "QT-1003", status = "Accepted", totalAmount = 600.0),
            Quote(id = "4", workspaceId = "w1", quoteNumber = "QT-1004", status = "Declined", totalAmount = 200.0)
        )
        val metrics = ProQuoteUiState(quotes = quotes).metrics
        assertEquals(3, metrics.totalQuotesSent)
        assertEquals(1, metrics.acceptedCount)
        assertEquals(33.333, metrics.conversionRatePercent, 0.01)
        assertEquals(600.0, metrics.acceptedRevenue, 0.001)
        assertEquals(400.0, metrics.pendingPipelineValue, 0.001)
    }

    @Test
    fun extractQuoteIdFromInput_handlesWebAndDeepLinkPaths() {
        assertEquals("abc-123", Formatters.extractQuoteIdFromInput("https://proquote.app/q/abc-123"))
        assertEquals("abc-123", Formatters.extractQuoteIdFromInput("/q/abc-123"))
        assertEquals("abc-123", Formatters.extractQuoteIdFromInput("proquote://q/abc-123"))
        assertEquals("abc-123", Formatters.extractQuoteIdFromInput("abc-123"))
    }

    @Test
    fun formatCurrency_formatsMultiCurrencyCorrectly() {
        val usd = Formatters.formatCurrency(1250.5, "USD")
        val tzs = Formatters.formatCurrency(1500000.0, "TZS")
        assertTrue(usd.contains("1,250.50"))
        assertTrue(tzs.contains("1,500,000"))
    }
}
