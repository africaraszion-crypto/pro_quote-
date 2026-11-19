package com.example.ui.util

import android.content.Context
import android.content.res.Resources
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Html
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.R
import com.example.data.model.PublicQuoteBundle
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.localizedResourcesFor
import com.example.ui.i18n.quoteStatusLabel

object QuotePdfPrinter {

    fun printQuote(
        context: Context,
        bundle: PublicQuoteBundle,
        language: AppLanguage = AppLanguage.English
    ) {
        val resources = localizedResourcesFor(context, language)
        val webView = WebView(context)
        val html = buildPrintableHtml(bundle, resources)
        val jobName = "ProQuote_${bundle.quote.quoteNumber}"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun escape(text: String?): String {
        return Html.escapeHtml(text.orEmpty())
    }

    private fun buildPrintableHtml(bundle: PublicQuoteBundle, res: Resources): String {
        val quote = bundle.quote
        val ws = bundle.workspace
        val client = bundle.client
        val currency = ws?.currency ?: "TZS"
        val brandColor = ws?.brandColor?.ifBlank { "#0f172a" } ?: "#0f172a"
        val companyName = escape(ws?.companyName ?: res.getString(R.string.app_name))
        val localizedStatus = quoteStatusLabel(res, quote.quoteStatus)

        val itemRows = bundle.items.joinToString("\n") { item ->
            val descHtml = if (!item.description.isNullOrBlank()) {
                """<div class="item-desc">${escape(item.description)}</div>"""
            } else ""
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            """
            <tr>
              <td>
                <div class="item-name">${escape(item.itemName)}</div>
                $descHtml
              </td>
              <td class="num">$qtyStr</td>
              <td class="num">${escape(Formatters.formatCurrency(item.unitPrice, currency))}</td>
              <td class="num bold">${escape(Formatters.formatCurrency(item.totalPrice, currency))}</td>
            </tr>
            """.trimIndent()
        }

        val logoHtml = if (!ws?.logoUrl.isNullOrBlank()) {
            """<img src="${escape(ws?.logoUrl)}" class="logo" alt="$companyName Logo" />"""
        } else ""

        val signatureSection = if (quote.status.equals("Accepted", ignoreCase = true) && !quote.signatureDataUrl.isNullOrBlank()) {
            """
            <div class="sig-box">
              <div class="sig-badge">${escape(res.getString(R.string.pdf_sig_badge))}</div>
              <img src="${quote.signatureDataUrl}" class="sig-img" alt="Signature" />
              <div class="sig-meta">
                ${
                    res.getString(
                        R.string.pdf_signed_by_html,
                        escape(quote.signedByName),
                        escape(Formatters.formatDisplayDateTime(quote.signedAt))
                    )
                }
              </div>
            </div>
            """.trimIndent()
        } else ""

        val discountRow = if (quote.discountAmount > 0) {
            """<tr><td>${escape(res.getString(R.string.builder_discount))}</td><td class="num">-${escape(Formatters.formatCurrency(quote.discountAmount, currency))}</td></tr>"""
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            @page { size: A4; margin: 16mm; }
            @media print {
              body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
            }
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              color: #0f172a;
              margin: 0;
              padding: 24px;
              line-height: 1.5;
              font-size: 13px;
            }
            .accent-bar { height: 6px; background: $brandColor; border-radius: 3px; margin-bottom: 24px; }
            .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; }
            .company { font-size: 22px; font-weight: 700; color: $brandColor; }
            .logo { max-height: 48px; max-width: 160px; margin-bottom: 8px; display: block; }
            .quote-label { text-align: right; }
            .quote-label h1 { margin: 0; font-size: 24px; letter-spacing: -0.5px; color: #0f172a; }
            .quote-num { font-family: monospace; font-size: 14px; color: #475569; margin-top: 4px; }
            .meta-grid { display: flex; justify-content: space-between; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 24px; }
            .meta-col h4 { margin: 0 0 6px 0; font-size: 11px; text-transform: uppercase; letter-spacing: 0.6px; color: #64748b; }
            table.items { width: 100%; border-collapse: collapse; margin-bottom: 24px; }
            table.items th { text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #64748b; border-bottom: 2px solid #e2e8f0; padding: 10px 8px; }
            table.items td { border-bottom: 1px solid #e2e8f0; padding: 12px 8px; vertical-align: top; }
            .num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
            .bold { font-weight: 600; }
            .item-name { font-weight: 600; color: #0f172a; }
            .item-desc { font-size: 12px; color: #64748b; margin-top: 2px; }
            .totals { width: 280px; margin-left: auto; border-collapse: collapse; margin-bottom: 28px; }
            .totals td { padding: 6px 0; }
            .totals tr.grand td { border-top: 2px solid $brandColor; padding-top: 10px; font-size: 16px; font-weight: 700; color: $brandColor; }
            .section { margin-bottom: 18px; padding: 14px; border: 1px solid #e2e8f0; border-radius: 8px; }
            .section h4 { margin: 0 0 6px 0; font-size: 11px; text-transform: uppercase; letter-spacing: 0.6px; color: #64748b; }
            .sig-box { margin-top: 24px; padding: 16px; border: 1px solid #10b981; background: #ecfdf5; border-radius: 8px; }
            .sig-badge { font-size: 11px; font-weight: 700; color: #059669; letter-spacing: 0.6px; margin-bottom: 8px; }
            .sig-img { max-height: 90px; background: #ffffff; border: 1px solid #d1fae5; border-radius: 4px; padding: 4px; }
            .sig-meta { margin-top: 8px; font-size: 12px; color: #065f46; }
          </style>
        </head>
        <body>
          <div class="accent-bar"></div>
          <div class="header">
            <div>
              $logoHtml
              <div class="company">$companyName</div>
            </div>
            <div class="quote-label">
              <h1>${escape(res.getString(R.string.public_doc_heading))}</h1>
              <div class="quote-num">${escape(quote.quoteNumber)}</div>
              <div class="quote-num">${escape(res.getString(R.string.pdf_status_line, localizedStatus))}</div>
            </div>
          </div>
          <div class="meta-grid">
            <div class="meta-col">
              <h4>${escape(res.getString(R.string.public_prepared_for))}</h4>
              <div><strong>${escape(client?.clientName ?: res.getString(R.string.label_client_fallback))}</strong></div>
              ${if (!client?.email.isNullOrBlank()) "<div>${escape(client?.email)}</div>" else ""}
              ${if (!client?.phone.isNullOrBlank()) "<div>${escape(client?.phone)}</div>" else ""}
              ${if (!client?.address.isNullOrBlank()) "<div>${escape(client?.address)}</div>" else ""}
            </div>
            <div class="meta-col" style="text-align:right;">
              <h4>${escape(res.getString(R.string.public_quote_dates))}</h4>
              <div>${escape(res.getString(R.string.public_issued_on, Formatters.formatDisplayDate(quote.createdAt)))}</div>
              <div>${escape(res.getString(R.string.public_valid_until_on, Formatters.formatDisplayDate(quote.validUntil)))}</div>
            </div>
          </div>
          <table class="items">
            <thead>
              <tr>
                <th>${escape(res.getString(R.string.pdf_col_item_desc))}</th>
                <th class="num">${escape(res.getString(R.string.pdf_col_qty))}</th>
                <th class="num">${escape(res.getString(R.string.pdf_col_unit_price))}</th>
                <th class="num">${escape(res.getString(R.string.pdf_col_total))}</th>
              </tr>
            </thead>
            <tbody>
              $itemRows
            </tbody>
          </table>
          <table class="totals">
            <tr><td>${escape(res.getString(R.string.builder_subtotal))}</td><td class="num">${escape(Formatters.formatCurrency(quote.subtotal, currency))}</td></tr>
            $discountRow
            <tr><td>${escape(res.getString(R.string.builder_tax_switch_label))}</td><td class="num">${escape(Formatters.formatCurrency(quote.taxAmount, currency))}</td></tr>
            <tr class="grand"><td>${escape(res.getString(R.string.pdf_col_total))}</td><td class="num">${escape(Formatters.formatCurrency(quote.totalAmount, currency))}</td></tr>
          </table>
          ${if (!quote.paymentInstructions.isNullOrBlank()) """<div class="section"><h4>${escape(res.getString(R.string.public_payment_instructions))}</h4><div>${escape(quote.paymentInstructions).replace("\n", "<br/>")}</div></div>""" else ""}
          ${if (!quote.notes.isNullOrBlank()) """<div class="section"><h4>${escape(res.getString(R.string.public_terms_conditions))}</h4><div>${escape(quote.notes).replace("\n", "<br/>")}</div></div>""" else ""}
          $signatureSection
        </body>
        </html>
        """.trimIndent()
    }
}
