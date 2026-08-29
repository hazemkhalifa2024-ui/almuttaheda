package com.example.data.printer

import android.content.Context
import com.example.data.model.Device
import com.example.data.model.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

object NetworkPrinterService {

    private class ArabicChar(
        val unicode: Char,
        val isolated: Char,
        val initial: Char,
        val medial: Char,
        val final: Char,
        val connectsLeft: Boolean,
        val connectsRight: Boolean
    )

    private val ARABIC_CHARS_LIST = listOf(
        // unicode, isolated, initial, medial, final, connectsLeft, connectsRight
        ArabicChar('ء', 0xFE80.toChar(), 0xFE80.toChar(), 0xFE80.toChar(), 0xFE80.toChar(), false, false),
        ArabicChar('آ', 0xFE81.toChar(), 0xFE81.toChar(), 0xFE82.toChar(), 0xFE82.toChar(), false, true),
        ArabicChar('أ', 0xFE83.toChar(), 0xFE83.toChar(), 0xFE84.toChar(), 0xFE84.toChar(), false, true),
        ArabicChar('ؤ', 0xFE85.toChar(), 0xFE85.toChar(), 0xFE86.toChar(), 0xFE86.toChar(), false, true),
        ArabicChar('إ', 0xFE87.toChar(), 0xFE87.toChar(), 0xFE88.toChar(), 0xFE88.toChar(), false, true),
        ArabicChar('ئ', 0xFE89.toChar(), 0xFE8B.toChar(), 0xFE8C.toChar(), 0xFE8A.toChar(), true, true),
        ArabicChar('ا', 0xFE8D.toChar(), 0xFE8D.toChar(), 0xFE8E.toChar(), 0xFE8E.toChar(), false, true),
        ArabicChar('ب', 0xFE8F.toChar(), 0xFE91.toChar(), 0xFE92.toChar(), 0xFE90.toChar(), true, true),
        ArabicChar('ة', 0xFE93.toChar(), 0xFE93.toChar(), 0xFE94.toChar(), 0xFE94.toChar(), false, true),
        ArabicChar('ت', 0xFE95.toChar(), 0xFE97.toChar(), 0xFE98.toChar(), 0xFE96.toChar(), true, true),
        ArabicChar('ث', 0xFE99.toChar(), 0xFE9B.toChar(), 0xFE9C.toChar(), 0xFE9A.toChar(), true, true),
        ArabicChar('ج', 0xFE9D.toChar(), 0xFE9F.toChar(), 0xFEA0.toChar(), 0xFE9E.toChar(), true, true),
        ArabicChar('ح', 0xFEA1.toChar(), 0xFEA3.toChar(), 0xFEA4.toChar(), 0xFEA2.toChar(), true, true),
        ArabicChar('خ', 0xFEA5.toChar(), 0xFEA7.toChar(), 0xFEA8.toChar(), 0xFEA6.toChar(), true, true),
        ArabicChar('د', 0xFEA9.toChar(), 0xFEA9.toChar(), 0xFEAA.toChar(), 0xFEAA.toChar(), false, true),
        ArabicChar('ذ', 0xFEAB.toChar(), 0xFEAB.toChar(), 0xFEAC.toChar(), 0xFEAC.toChar(), false, true),
        ArabicChar('ر', 0xFEAD.toChar(), 0xFEAD.toChar(), 0xFEAE.toChar(), 0xFEAE.toChar(), false, true),
        ArabicChar('ز', 0xFEAF.toChar(), 0xFEAF.toChar(), 0xFEB0.toChar(), 0xFEB0.toChar(), false, true),
        ArabicChar('س', 0xFEB1.toChar(), 0xFEB3.toChar(), 0xFEB4.toChar(), 0xFEB2.toChar(), true, true),
        ArabicChar('ش', 0xFEB5.toChar(), 0xFEB7.toChar(), 0xFEB8.toChar(), 0xFEB6.toChar(), true, true),
        ArabicChar('ص', 0xFEB9.toChar(), 0xFEBB.toChar(), 0xFEBC.toChar(), 0xFEBA.toChar(), true, true),
        ArabicChar('ض', 0xFEBD.toChar(), 0xFEBF.toChar(), 0xFEC0.toChar(), 0xFEBE.toChar(), true, true),
        ArabicChar('ط', 0xFEC1.toChar(), 0xFEC3.toChar(), 0xFEC4.toChar(), 0xFEC2.toChar(), true, true),
        ArabicChar('ظ', 0xFEC5.toChar(), 0xFEC7.toChar(), 0xFEC8.toChar(), 0xFEC6.toChar(), true, true),
        ArabicChar('ع', 0xFEC9.toChar(), 0xFECB.toChar(), 0xFECC.toChar(), 0xFECA.toChar(), true, true),
        ArabicChar('غ', 0xFECD.toChar(), 0xFECF.toChar(), 0xFED0.toChar(), 0xFECE.toChar(), true, true),
        ArabicChar('ف', 0xFED1.toChar(), 0xFED3.toChar(), 0xFED4.toChar(), 0xFED2.toChar(), true, true),
        ArabicChar('ق', 0xFED5.toChar(), 0xFED7.toChar(), 0xFED8.toChar(), 0xFED6.toChar(), true, true),
        ArabicChar('ك', 0xFED9.toChar(), 0xFEDB.toChar(), 0xFEDC.toChar(), 0xFEDA.toChar(), true, true),
        ArabicChar('ل', 0xFEDD.toChar(), 0xFEDF.toChar(), 0xFEE0.toChar(), 0xFEDE.toChar(), true, true),
        ArabicChar('م', 0xFEE1.toChar(), 0xFEE3.toChar(), 0xFEE4.toChar(), 0xFEE2.toChar(), true, true),
        ArabicChar('ن', 0xFEE5.toChar(), 0xFEE7.toChar(), 0xFEE8.toChar(), 0xFEE6.toChar(), true, true),
        ArabicChar('ه', 0xFEE9.toChar(), 0xFEEB.toChar(), 0xFEEC.toChar(), 0xFEEA.toChar(), true, true),
        ArabicChar('و', 0xFEED.toChar(), 0xFEED.toChar(), 0xFEEE.toChar(), 0xFEEE.toChar(), false, true),
        ArabicChar('ى', 0xFEEF.toChar(), 0xFEEF.toChar(), 0xFEF0.toChar(), 0xFEF0.toChar(), false, true),
        ArabicChar('ي', 0xFEF1.toChar(), 0xFEF3.toChar(), 0xFEF4.toChar(), 0xFEF2.toChar(), true, true),
        
        // Ligatures
        ArabicChar('\uFEFB', '\uFEFB', '\uFEFB', '\uFEFC', '\uFEFC', false, true), // Lam-Alef
        ArabicChar('\uFEF7', '\uFEF7', '\uFEF7', '\uFEF8', '\uFEF8', false, true), // Lam-Alef Hamza Above
        ArabicChar('\uFEF9', '\uFEF9', '\uFEF9', '\uFEFA', '\uFEFA', false, true), // Lam-Alef Hamza Below
        ArabicChar('\uFEF5', '\uFEF5', '\uFEF5', '\uFEF6', '\uFEF6', false, true)  // Lam-Alef Madda Above
    )

    private val ARABIC_CHARS_MAP = ARABIC_CHARS_LIST.associateBy { it.unicode }

    private fun preprocessLamAlef(text: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == 'ل' && i + 1 < text.length) {
                val next = text[i + 1]
                when (next) {
                    'ا' -> { result.append('\uFEFB'); i += 2; continue }
                    'أ' -> { result.append('\uFEF7'); i += 2; continue }
                    'إ' -> { result.append('\uFEF9'); i += 2; continue }
                    'آ' -> { result.append('\uFEF5'); i += 2; continue }
                }
            }
            result.append(c)
            i++
        }
        return result.toString()
    }

    private fun shapeArabicText(text: String): String {
        val preprocessed = preprocessLamAlef(text)
        val result = StringBuilder()
        
        for (i in preprocessed.indices) {
            val c = preprocessed[i]
            val currentInfo = ARABIC_CHARS_MAP[c]
            
            if (currentInfo == null) {
                result.append(c)
                continue
            }
            
            val prevChar = if (i > 0) preprocessed[i - 1] else ' '
            val prevInfo = ARABIC_CHARS_MAP[prevChar]
            val connectsRight = currentInfo.connectsRight && prevInfo != null && prevInfo.connectsLeft
            
            val nextChar = if (i + 1 < preprocessed.length) preprocessed[i + 1] else ' '
            val nextInfo = ARABIC_CHARS_MAP[nextChar]
            val connectsLeft = currentInfo.connectsLeft && nextInfo != null && nextInfo.connectsRight
            
            val shapedChar = when {
                connectsRight && connectsLeft -> currentInfo.medial
                connectsRight -> currentInfo.final
                connectsLeft -> currentInfo.initial
                else -> currentInfo.isolated
            }
            result.append(shapedChar)
        }
        
        return result.toString()
    }

    /**
     * Shapes and formats Arabic text to render properly on thermal printers (connected and RTL).
     */
    fun formatArabicForPrinter(text: String): String {
        if (text.isEmpty()) return text
        
        // 1. Contextual Arabic shaping using our pure-Kotlin engine
        val shaped = try {
            shapeArabicText(text)
        } catch (e: Exception) {
            text // Fallback
        }

        // 2. Reverse line characters for RTL printing, respecting English and numeric segments
        return shaped.split("\n").joinToString("\n") { line ->
            reverseLine(line)
        }
    }

    private fun reverseLine(line: String): String {
        if (line.isBlank()) return line
        
        val segments = mutableListOf<Pair<Boolean, String>>() // Pair(isArabicRtl, text)
        var currentSegment = StringBuilder()
        var currentIsArabic = isArabicChar(line.firstOrNull() ?: ' ')
        
        for (char in line) {
            val isArabic = isArabicChar(char)
            val isNeutral = char == ' ' || char == ':' || char == '-' || char == '/' || char == '=' || 
                            char == '(' || char == ')' || char == '[' || char == ']' || char == '{' || 
                            char == '}' || char == '.' || char == ',' || char == '!' || char == '?' || 
                            char == '%' || char == '•' || char == '*' || char == '+'
            
            if (isNeutral) {
                currentSegment.append(char)
            } else if (isArabic == currentIsArabic) {
                currentSegment.append(char)
            } else {
                segments.add(Pair(currentIsArabic, currentSegment.toString()))
                currentSegment = StringBuilder().append(char)
                currentIsArabic = isArabic
            }
        }
        if (currentSegment.isNotEmpty()) {
            segments.add(Pair(currentIsArabic, currentSegment.toString()))
        }
        
        val result = StringBuilder()
        for (i in segments.indices.reversed()) {
            val (isRtl, segmentText) = segments[i]
            if (isRtl) {
                result.append(reverseArabicSegment(segmentText))
            } else {
                result.append(segmentText)
            }
        }
        return result.toString()
    }

    private fun reverseArabicSegment(segment: String): String {
        val chars = segment.toCharArray()
        val reversed = chars.reversedArray()
        for (i in reversed.indices) {
            when (reversed[i]) {
                '(' -> reversed[i] = ')'
                ')' -> reversed[i] = '('
                '[' -> reversed[i] = ']'
                ']' -> reversed[i] = '['
                '{' -> reversed[i] = '}'
                '}' -> reversed[i] = '{'
                '<' -> reversed[i] = '>'
                '>' -> reversed[i] = '<'
            }
        }
        return String(reversed)
    }

    private fun isArabicChar(c: Char): Boolean {
        val code = c.code
        return (code in 0x0600..0x06FF) || 
               (code in 0x0750..0x077F) || 
               (code in 0x08A0..0x08FF) || 
               (code in 0xFB50..0xFDFF) || 
               (code in 0xFE70..0xFEFF)
    }

    /**
     * Sends ESC/POS command bytes directly to the thermal receipt printer over raw TCP/IP (port 9100).
     */
    suspend fun printReceipt(
        ipAddress: String,
        device: Device,
        config: PrinterConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (ipAddress.isBlank()) {
                return@withContext Result.failure(Exception("عنوان IP الخاص بالطابعة فارغ"))
            }

            val socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, 9100), 4000) // 4 seconds timeout
            val outputStream: OutputStream = socket.getOutputStream()

            // ESC/POS Initialization Commands for Arabic Xprinter support
            val init = byteArrayOf(0x1B, 0x40) // Initialize printer (ESC @)
            val disableChinese = byteArrayOf(0x1C, 0x2E) // FS . to cancel Chinese/double-byte mode
            val selectArabicCodepage = byteArrayOf(0x1B, 0x74, 22) // ESC t 22 to select CP1256 (Arabic) page code on Xprinter

            val alignCenter = byteArrayOf(0x1B, 0x61, 0x01) // Center align
            val alignRight = byteArrayOf(0x1B, 0x61, 0x02) // Right align
            val boldOn = byteArrayOf(0x1B, 0x45, 0x01) // Bold on
            val boldOff = byteArrayOf(0x1B, 0x45, 0x00) // Bold off
            val feedAndCut = byteArrayOf(0x1D, 0x56, 0x41, 0x03) // Feed paper and cut (GS V A 3)

            // Start communication
            outputStream.write(init)
            outputStream.write(disableChinese)
            outputStream.write(selectArabicCodepage)

            // Header (Centered, Bold)
            outputStream.write(alignCenter)
            outputStream.write(boldOn)
            val formattedShopName = formatArabicForPrinter(config.shopName) + "\n"
            outputStream.write(formattedShopName.toByteArray(charset("CP1256")))
            outputStream.write(boldOff)

            if (config.shopPhone.isNotBlank()) {
                val formattedPhone = formatArabicForPrinter("هاتف: ${config.shopPhone}") + "\n"
                outputStream.write(formattedPhone.toByteArray(charset("CP1256")))
            }
            outputStream.write("===============================\n".toByteArray())
            val formattedTicket = formatArabicForPrinter("إيصال صيانة رقم: ${device.ticketNumber}") + "\n"
            outputStream.write(formattedTicket.toByteArray(charset("CP1256")))
            outputStream.write("===============================\n".toByteArray())

            // Body (Right Aligned)
            outputStream.write(alignRight)
            val formattedCustName = formatArabicForPrinter("اسم العميل: ${device.customer_name}") + "\n"
            outputStream.write(formattedCustName.toByteArray(charset("CP1256")))
            
            if (!device.customer_phone.isNullOrBlank()) {
                val formattedCustPhone = formatArabicForPrinter("رقم الهاتف: ${device.customer_phone}") + "\n"
                outputStream.write(formattedCustPhone.toByteArray(charset("CP1256")))
            }
            val formattedDevName = formatArabicForPrinter("اسم الجهاز: ${device.device_name}") + "\n"
            outputStream.write(formattedDevName.toByteArray(charset("CP1256")))
            
            if (!device.issue_description.isNullOrBlank()) {
                val formattedIssue = formatArabicForPrinter("العطل: ${device.issue_description}") + "\n"
                outputStream.write(formattedIssue.toByteArray(charset("CP1256")))
            }
            val techName = when (device.technician?.lowercase()) {
                "tech1" -> "أحمد (هاردوير)"
                "tech2" -> "محمود (شاشات)"
                "hazem" -> "حازم"
                "admin" -> "المدير"
                else -> device.technician ?: "غير محدد"
            }
            val formattedTech = formatArabicForPrinter("الفني المسؤول: $techName") + "\n"
            outputStream.write(formattedTech.toByteArray(charset("CP1256")))
            
            if (!device.due_date.isNullOrBlank()) {
                val formattedDueDate = formatArabicForPrinter("ميعاد التسليم المتوقع: ${device.due_date}") + "\n"
                outputStream.write(formattedDueDate.toByteArray(charset("CP1256")))
            }
            outputStream.write("-------------------------------\n".toByteArray())
            val formattedCost = formatArabicForPrinter("التكلفة التقديرية: ${device.estimated_cost ?: "0"} ج.م") + "\n"
            outputStream.write(formattedCost.toByteArray(charset("CP1256")))
            if (device.down_payment > 0) {
                val formattedDownPayment = formatArabicForPrinter("العربون المدفوع: ${device.down_payment} ج.م") + "\n"
                outputStream.write(formattedDownPayment.toByteArray(charset("CP1256")))
                val formattedRemaining = formatArabicForPrinter("المبلغ المتبقي: ${device.remaining_balance} ج.م") + "\n"
                outputStream.write(formattedRemaining.toByteArray(charset("CP1256")))
            }
            outputStream.write("-------------------------------\n".toByteArray())

            // Footer (Centered)
            outputStream.write(alignCenter)
            if (config.footerNote.isNotBlank()) {
                val formattedFooter = formatArabicForPrinter(config.footerNote) + "\n"
                outputStream.write(formattedFooter.toByteArray(charset("CP1256")))
            }
            val formattedThanks = formatArabicForPrinter("شكراً لثقتكم بنا") + "\n"
            outputStream.write(formattedThanks.toByteArray(charset("CP1256")))
            
            outputStream.write("\n\n\n\n".toByteArray()) // Feed lines properly before cutting

            // Cut paper
            outputStream.write(feedAndCut)

            outputStream.flush()
            socket.close()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends TSPL command bytes directly to the sticker label printer over raw TCP/IP (port 9100).
     */
    suspend fun printSticker(
        ipAddress: String,
        device: Device,
        config: PrinterConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (ipAddress.isBlank()) {
                return@withContext Result.failure(Exception("عنوان IP الخاص بالطابعة فارغ"))
            }

            val socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, 9100), 4000) // 4 seconds timeout
            val outputStream: OutputStream = socket.getOutputStream()

            val techName = when (device.technician?.lowercase()) {
                "tech1" -> "Ahmad"
                "tech2" -> "Mahmoud"
                "hazem" -> "Hazem"
                "admin" -> "Admin"
                else -> device.technician ?: "N/A"
            }
            val dueDateStr = device.due_date ?: "N/A"

            // Format Arabic text fields for TSPL compatibility (reverse & shape)
            val shopNameShaped = formatArabicForPrinter(config.shopName)
            val customerNameShaped = formatArabicForPrinter(device.customer_name)
            val deviceNameShaped = formatArabicForPrinter(device.device_name)

            // Build TSPL command sequence
            // Standard size 50mm x 30mm with a 3mm gap
            val tspl = buildString {
                append("SIZE 50 mm, 30 mm\r\n")
                append("GAP 3 mm, 0 mm\r\n")
                append("DIRECTION 1\r\n")
                append("CODEPAGE 1256\r\n") // Select Arabic code page in TSPL
                append("CLS\r\n")
                // Draw Shop name & Ticket number at the top
                append("TEXT 10,10,\"3\",0,1,1,\"$shopNameShaped\"\r\n")
                append("TEXT 280,10,\"3\",0,1,1,\"${device.ticketNumber}\"\r\n")
                append("BAR 10,35,380,3\r\n")
                
                // Draw Customer & Device Details
                append("TEXT 10,50,\"2\",0,1,1,\"NAME: $customerNameShaped\"\r\n")
                append("TEXT 10,75,\"2\",0,1,1,\"DEV: $deviceNameShaped\"\r\n")
                append("TEXT 10,100,\"2\",0,1,1,\"TECH: $techName\"\r\n")
                append("TEXT 10,125,\"2\",0,1,1,\"DUE: $dueDateStr\"\r\n")
                // Draw Barcode 128
                append("BARCODE 10,150,\"128\",40,1,0,2,2,\"${device.ticketNumber}\"\r\n")
                // Print command (1 copy)
                append("PRINT 1,1\r\n")
            }

            outputStream.write(tspl.toByteArray(charset("CP1256")))
            outputStream.flush()
            socket.close()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


