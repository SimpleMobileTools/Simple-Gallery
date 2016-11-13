package com.simplemobiletools.gallery.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.activity_about.*
import java.util.*

class AboutActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setupEmail()
        setupCopyright()
        setupRateUs()
        setupInvite()
        setupLicense()
        setupFacebook()
        setupGPlus()
    }

    private fun setupEmail() {
        val email = getString(R.string.email)
        val appName = getString(R.string.app_name)
        val href = "<a href=\"mailto:$email?subject=$appName\">$email</a>"
        about_email.text = Html.fromHtml(href)
        about_email.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupCopyright() {
        val versionName = BuildConfig.VERSION_NAME
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val copyrightText = String.format(getString(R.string.copyright), versionName, year)
        about_copyright.text = copyrightText
    }

    private fun setupRateUs() {
        if (mConfig.isFirstRun) {
            about_rate_us.visibility = View.GONE
        } else {
            about_rate_us.setOnClickListener {
                val uri = Uri.parse("market://details?id=$packageName")
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (ignored: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getStoreUrl())))
                }
            }
        }
    }

    fun setupInvite() {
        about_invite.setOnClickListener {
            val text = String.format(getString(R.string.share_text), getString(R.string.app_name), getStoreUrl())
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                startActivity(Intent.createChooser(this, getString(R.string.invite_via)))
            }
        }
    }

    fun setupLicense() {
        about_license.setOnClickListener {
            val intent = Intent(applicationContext, LicenseActivity::class.java)
            startActivity(intent)
        }
    }

    fun setupFacebook() {
        about_facebook.setOnClickListener {
            var link = "https://www.facebook.com/simplemobiletools"
            try {
                packageManager.getPackageInfo("com.facebook.katana", 0)
                link = "fb://page/150270895341774"
            } catch (ignored: Exception) {
            }

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
    }

    fun setupGPlus() {
        about_gplus.setOnClickListener {
            val link = "https://plus.google.com/communities/104880861558693868382"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
    }

    private fun getStoreUrl() = "https://play.google.com/store/apps/details?id=$packageName"
}
