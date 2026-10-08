package com.habitsfirst.app.util

import java.util.Locale

object PaymentSafetyHelper {

    // Known payment, UPI, banking, and wallet packages that must NEVER be monitored or blocked
    private val KNOWN_PAYMENT_PACKAGES = setOf(
        // Google & Apple / Samsung Wallets
        "com.google.android.apps.nbu.paisa.user", // Google Pay (India / Tez)
        "com.google.android.apps.walletnfcrel",   // Google Wallet
        "com.samsung.android.spay",              // Samsung Pay
        "com.samsung.android.raja.p2p",

        // Indian UPI & Wallets
        "com.phonepe.app",                       // PhonePe
        "com.phonepe.app.preprod",
        "net.one97.paytm",                       // Paytm
        "com.paytm.business",
        "in.org.npci.upiapp",                    // BHIM UPI
        "com.infostretch.bhim",
        "com.dreamplug.androidapp",               // CRED
        "com.freecharge.android",                // Freecharge
        "com.mobikwik_new",                      // MobiKwik
        "com.bharatpe.app",                      // BharatPe

        // Global Payments & Transfers
        "com.paypal.android.p2pmobile",          // PayPal
        "com.paypal.android.merchants",
        "com.venmo",                             // Venmo
        "com.squareup.cash",                     // Cash App
        "com.zellepay.zelle",                    // Zelle
        "com.revolut.revolut",                   // Revolut
        "co.uk.monzo",                           // Monzo
        "com.transferwise.android",              // Wise
        "com.starlingbank.android",              // Starling
        "de.number26.android",                   // N26
        "com.myklarnamobile",                    // Klarna
        "com.affirm.android",                    // Affirm

        // Indian Banks
        "com.sbi.lotusintouch",                  // YONO SBI
        "com.sbi.SBIFreedomPlus",                // SBI Anywhere
        "com.sbi.upi",
        "com.snapwork.hdfc",                     // HDFC MobileBanking
        "com.hdfcbank.payzapp",                  // PayZapp
        "com.csam.icici.bank.imobile",           // iMobile Pay (ICICI)
        "com.icicibank.pockets",
        "com.axis.mobile",                       // Axis Mobile
        "com.axis.okc",
        "com.msf.kbank.mobile",                  // Kotak 811
        "com.pnb.mbank",                         // PNB ONE
        "com.bob.bobworld",                      // BoB World
        "com.canarabank.mobility",               // Canara ai1
        "com.canarabank.ai1",
        "com.infrasofttech.uboi",                // Union Bank Vyom
        "com.indusind.indusmobile",              // IndusInd Mobile
        "com.yesbank",
        "com.idfcfirstbank.optimus",             // IDFC FIRST Bank
        "com.fedmobile",                         // FedMobile
        "com.rblbank.mobank",                    // RBL MoBank

        // US & International Banks
        "com.chase.sig.android",                 // Chase Mobile
        "com.infonow.bofa",                      // Bank of America
        "com.wf.wellsfargomobile",               // Wells Fargo
        "com.citi.citimobile",                   // Citi Mobile
        "com.konylabs.capitalone",               // Capital One
        "com.usbank.mobilebanking",              // U.S. Bank
        "com.td",                                // TD Bank
        "com.discoverfinancial.mobile",          // Discover Mobile
        "com.americanexpress.android.acctsvcs.us",// Amex
        "com.barclays.android.barclaysmobilebanking", // Barclays
        "com.grppl.android.shell.CMBlloydsTSB73", // Lloyds
        "com.grppl.android.shell.halifax",       // Halifax
        "com.rbs.mobile.android.natwest",        // NatWest
        "uk.co.hsbc.hsbcukmobilebanking",        // HSBC UK
        "com.htsu.hsbcpersonalbanking",          // HSBC US
        "uk.co.santander.santanderUK",           // Santander UK
        "com.santander.app",

        // Investment & Trading
        "com.zerodha.kite3",                     // Kite by Zerodha
        "com.nextbillion.groww",                 // Groww
        "in.upstox.app",                         // Upstox
        "com.msf.angelmobile",                   // Angel One
        "com.robinhood.android",                 // Robinhood
        "com.coinbase.android",                  // Coinbase
        "com.binance.dev"                        // Binance
    )

    // Essential system services, phone, authentication, and emergency apps
    private val SYSTEM_AND_ESSENTIAL_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.vending",                   // Google Play Store
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.dialer",             // Phone dialer
        "com.android.dialer",
        "com.google.android.apps.messaging",     // SMS
        "com.android.mms",
        "com.google.android.contacts",
        "com.android.contacts",
        "com.google.android.inputmethod.latin",  // Gboard
        "com.touchtype.swiftkey",                // SwiftKey
        "com.google.android.apps.authenticator2",// Google Authenticator
        "com.azure.authenticator",               // Microsoft Authenticator
        "com.duosecurity.duomobile",             // Duo Mobile
        "com.onepassword",                       // 1Password
        "com.x8bit.bitwarden"                    // Bitwarden
    )

    // Keywords that indicate banking or payment software
    private val PAYMENT_KEYWORDS = listOf(
        "bank",
        "banking",
        "wallet",
        "paytm",
        "phonepe",
        "gpay",
        "bhim",
        "upi",
        "paisa",
        "creditcard",
        "fintech",
        "passbook"
    )

    /**
     * Returns true if the package is a payment, banking, or financial app.
     */
    fun isPaymentOrBankingApp(packageName: String): Boolean {
        val lower = packageName.lowercase(Locale.ROOT)
        if (KNOWN_PAYMENT_PACKAGES.contains(lower)) return true

        for (keyword in PAYMENT_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if the package should NEVER be blocked, intercepted, or monitored.
     */
    fun isExemptFromBlocking(packageName: String): Boolean {
        val lower = packageName.lowercase(Locale.ROOT)
        if (SYSTEM_AND_ESSENTIAL_PACKAGES.contains(lower)) return true
        if (isPaymentOrBankingApp(lower)) return true

        // Essential system UI patterns
        if (lower.startsWith("com.android.") &&
            (lower.contains("systemui") || lower.contains("settings") || lower.contains("dialer") || lower.contains("launcher"))) {
            return true
        }

        if (lower.contains("launcher") || lower.contains("keyboard") || lower.contains("ime")) {
            return true
        }

        return false
    }
}
