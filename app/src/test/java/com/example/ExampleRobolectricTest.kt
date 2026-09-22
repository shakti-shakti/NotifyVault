package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NotificationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NotifyVault", appName)
  }

  @Test
  fun `test otp extraction regex`() {
    val otpRegex = Regex("""\b\d{4,8}\b""")
    val text = "Your HDFC Bank OTP is 849201 for transaction of INR 3,250. Valid for 10 mins."
    val match = otpRegex.find(text)
    assertEquals("849201", match?.value)
  }

  @Test
  fun `test currency amount extraction regex`() {
    val amountRegex = Regex("""([$€£₹]\s?[0-9,]+(\.[0-9]{2})?|USD\s?[0-9,]+)""")
    val text = "Payment of $4,250.00 received via Stripe."
    val match = amountRegex.find(text)
    assertEquals("$4,250.00", match?.value)
  }
}
