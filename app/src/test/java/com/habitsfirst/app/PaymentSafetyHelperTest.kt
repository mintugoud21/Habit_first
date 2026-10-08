package com.habitsfirst.app

import com.habitsfirst.app.util.PaymentSafetyHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentSafetyHelperTest {

    @Test
    fun testPaymentAppsIdentified() {
        // UPI & Indian payment apps
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.google.android.apps.nbu.paisa.user"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.phonepe.app"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("net.one97.paytm"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("in.org.npci.upiapp"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.dreamplug.androidapp"))

        // Global wallets & banking
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.paypal.android.p2pmobile"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.squareup.cash"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.venmo"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.chase.sig.android"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.sbi.lotusintouch"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.csam.icici.bank.imobile"))
        assertTrue(PaymentSafetyHelper.isPaymentOrBankingApp("com.snapwork.hdfc"))
    }

    @Test
    fun testEssentialAndSystemAppsExempt() {
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("android"))
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("com.android.systemui"))
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("com.android.settings"))
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("com.google.android.dialer"))
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("com.google.android.apps.authenticator2"))
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("com.phonepe.app"))
        assertTrue(PaymentSafetyHelper.isExemptFromBlocking("com.google.android.apps.nbu.paisa.user"))
    }

    @Test
    fun testDistractingSocialAppsNotExempt() {
        assertFalse(PaymentSafetyHelper.isExemptFromBlocking("com.instagram.android"))
        assertFalse(PaymentSafetyHelper.isExemptFromBlocking("com.zhiliaoapp.musically"))
        assertFalse(PaymentSafetyHelper.isExemptFromBlocking("com.google.android.youtube"))
        assertFalse(PaymentSafetyHelper.isExemptFromBlocking("com.twitter.android"))
        assertFalse(PaymentSafetyHelper.isExemptFromBlocking("com.reddit.frontpage"))
        assertFalse(PaymentSafetyHelper.isExemptFromBlocking("com.facebook.katana"))
    }
}
