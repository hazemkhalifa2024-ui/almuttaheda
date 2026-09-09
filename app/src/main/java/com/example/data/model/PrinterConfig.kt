package com.example.data.model

data class PrinterConfig(
    // Global Shop Info
    val shopName: String = "المتحدة للصيانة",
    val shopPhone: String = "01000000000",
    val footerNote: String = "شكراً لزيارتكم الورشة - ضمان شهر ضد عيوب الصيانة",

    // Printer 1: Customer Thermal Receipt (طابعة ريسيت العميل)
    val receiptPrinterName: String = "طابعة ريسيت العميل (80mm)",
    val receiptPrinterIp: String = "192.168.1.100",
    val receiptPaperWidth: String = "80mm", // "80mm", "58mm"
    val autoPrintReceiptOnIntake: Boolean = true,
    val receiptCopies: Int = 1,

    // Printer 2: Device Barcode Sticker (طابعة استيكر الباركود للجهاز)
    val stickerPrinterName: String = "طابعة ملصقات الباركود (50x30mm)",
    val stickerPrinterIp: String = "192.168.1.101",
    val stickerWidth: String = "50mm x 30mm", // "50mm x 30mm", "40mm x 25mm", "40mm x 30mm"
    val autoPrintStickerOnIntake: Boolean = true,
    val stickerCopies: Int = 1,

    // Master Dual Print Engine
    val dualAutoPrintOnIntake: Boolean = true, // عند الحفظ والاستلام: تطبع الطابعتان فوراً تلقائياً!
    val printStickerTwiceOnSave: Boolean = false // طباعة الاستيكر مرتين عند الحفظ
) {
    // Backward compatibility helpers
    val paperWidth: String get() = receiptPaperWidth
}
