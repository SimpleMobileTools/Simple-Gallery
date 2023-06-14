package com.simplemobiletools.commons.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.isEmpty
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.RateStarsDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.item_about.view.*

class AboutActivity : BaseSimpleActivity() {
    private var appName = ""
    private var primaryColor = 0
    private var textColor = 0
    private var backgroundColor = 0
    private var inflater: LayoutInflater? = null

    private var firstVersionClickTS = 0L
    private var clicksSinceFirstClick = 0
    private val EASTER_EGG_TIME_LIMIT = 3000L
    private val EASTER_EGG_REQUIRED_CLICKS = 7

    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        primaryColor = getProperPrimaryColor()
        textColor = getProperTextColor()
        backgroundColor = getProperBackgroundColor()
        inflater = LayoutInflater.from(this)

        updateMaterialActivityViews(about_coordinator, about_holder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(about_nested_scrollview, about_toolbar)

        appName = intent.getStringExtra(APP_NAME) ?: ""

    }

    override fun onResume() {
        super.onResume()
        updateTextColors(about_nested_scrollview)
        setupToolbar(about_toolbar, NavigationIcon.Arrow)

        about_support_layout.removeAllViews()
        about_social_layout.removeAllViews()
        about_other_layout.removeAllViews()

        setupEmail()
        setupRateUs()
        setupInvite()
        setupContributors()
        setupDonate()
        setupGitHub()
        setupReddit()
        setupTelegram()
        setupMoreApps()
        setupWebsite()
        setupPrivacyPolicy()
        setupLicense()
        setupVersion()
    }

    private fun setupFAQ() {
        val faqItems = intent.getSerializableExtra(APP_FAQ) as ArrayList<FAQItem>
        if (faqItems.isNotEmpty()) {
            inflater?.inflate(R.layout.item_about, null)?.apply {
                setupAboutItem(this, R.drawable.ic_question_mark_vector, R.string.frequently_asked_questions)
                about_support_layout.addView(this)

                setOnClickListener {
                    launchFAQActivity()
                }
            }
        }
    }

    private fun launchFAQActivity() {
        val faqItems = intent.getSerializableExtra(APP_FAQ) as ArrayList<FAQItem>
        Intent(applicationContext, FAQActivity::class.java).apply {
            putExtra(APP_ICON_IDS, getAppIconIDs())
            putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
            putExtra(APP_FAQ, faqItems)
            startActivity(this)
        }
    }

    private fun setupEmail() {
        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            if (about_support_layout.isEmpty()) {
                about_support.beGone()
                about_support_divider.beGone()
            }

            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_mail_vector, R.string.my_email)
            about_support_layout.addView(this)

            setOnClickListener {
                val msg = "${getString(R.string.before_asking_question_read_faq)}\n\n${getString(R.string.make_sure_latest)}"
                if (intent.getBooleanExtra(SHOW_FAQ_BEFORE_MAIL, false) && !baseConfig.wasBeforeAskingShown) {
                    baseConfig.wasBeforeAskingShown = true
                    ConfirmationAdvancedDialog(this@AboutActivity, msg, 0, R.string.read_faq, R.string.skip) { success ->
                        if (success) {
                            launchFAQActivity()
                        } else {
                            launchEmailIntent()
                        }
                    }
                } else {
                    launchEmailIntent()
                }
            }
        }
    }

    private fun launchEmailIntent() {
        val appVersion = String.format(getString(R.string.app_version, intent.getStringExtra(APP_VERSION_NAME)))
        val deviceOS = String.format(getString(R.string.device_os), Build.VERSION.RELEASE)
        val newline = "\n"
        val separator = "------------------------------"
        val body = "$appVersion$newline$deviceOS$newline$separator$newline$newline"

        val address = if (packageName.startsWith("com.simplemobiletools")) {
            getString(R.string.my_email)
        } else {
            getString(R.string.my_fake_email)
        }

        val selectorIntent = Intent(ACTION_SENDTO)
            .setData("mailto:$address".toUri())
        val emailIntent = Intent(ACTION_SEND).apply {
            putExtra(EXTRA_EMAIL, arrayOf(address))
            putExtra(EXTRA_SUBJECT, appName)
            putExtra(EXTRA_TEXT, body)
            selector = selectorIntent
        }

        try {
            startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            val chooser = createChooser(emailIntent, getString(R.string.send_email))
            try {
                startActivity(chooser)
            } catch (e: Exception) {
                toast(R.string.no_email_client_found)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun setupRateUs() {
        if (resources.getBoolean(R.bool.hide_google_relations) || resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_star_vector, R.string.rate_us)

            setOnClickListener {
                if (baseConfig.wasBeforeRateShown) {
                    launchRateUsPrompt()
                } else {
                    baseConfig.wasBeforeRateShown = true
                    val msg = "${getString(R.string.before_rate_read_faq)}\n\n${getString(R.string.make_sure_latest)}"
                    ConfirmationAdvancedDialog(this@AboutActivity, msg, 0, R.string.read_faq, R.string.skip) { success ->
                        if (success) {
                            launchFAQActivity()
                        } else {
                            launchRateUsPrompt()
                        }
                    }
                }
            }
        }
    }

    private fun launchRateUsPrompt() {
        if (baseConfig.wasAppRated) {
            redirectToRateUs()
        } else {
            RateStarsDialog(this@AboutActivity)
        }
    }

    private fun setupInvite() {
        if (resources.getBoolean(R.bool.hide_google_relations) || resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_add_person_vector, R.string.invite_friends)

            setOnClickListener {
                val text = String.format(getString(R.string.share_text), appName, getStoreUrl())
                Intent().apply {
                    action = ACTION_SEND
                    putExtra(EXTRA_SUBJECT, appName)
                    putExtra(EXTRA_TEXT, text)
                    type = "text/plain"
                    startActivity(createChooser(this, getString(R.string.invite_via)))
                }
            }
        }
    }

    private fun setupContributors() {
        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_face_vector, R.string.contributors)

            setOnClickListener {
                val intent = Intent(applicationContext, ContributorsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setupDonate() {
        if (resources.getBoolean(R.bool.show_donate_in_about) && !resources.getBoolean(R.bool.hide_all_external_links)) {
            inflater?.inflate(R.layout.item_about, null)?.apply {
                setupAboutItem(this, R.drawable.ic_dollar_vector, R.string.donate)

                setOnClickListener {
                    launchViewIntent(getString(R.string.donate_url))
                }
            }
        }
    }

    private fun setupFacebook() {
        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            about_item_icon.setImageResource(R.drawable.ic_facebook_vector)
            about_item_label.setText(R.string.facebook)
            about_item_label.setTextColor(textColor)
            about_social_layout.addView(this)

            setOnClickListener {
                var link = "https://www.facebook.com/simplemobiletools"
                try {
                    packageManager.getPackageInfo("com.facebook.katana", 0)
                    link = "fb://page/150270895341774"
                } catch (ignored: Exception) {
                }

                launchViewIntent(link)
            }
        }
    }

    private fun setupGitHub() {
        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            about_item_icon.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_github_vector, backgroundColor.getContrastColor()))
            about_item_label.setText(R.string.github)
            about_item_label.setTextColor(textColor)
            about_social_layout.addView(this)

            setOnClickListener {
                launchViewIntent("https://github.com/RikardoMexican/GalleryRAW")
            }
        }
    }

    private fun setupReddit() {
        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            about_item_icon.setImageResource(R.drawable.ic_reddit_vector)
            about_item_label.setText(R.string.reddit)
            about_item_label.setTextColor(textColor)
            about_social_layout.addView(this)

            setOnClickListener {
                launchViewIntent("https://www.reddit.com/r/GalleryRAW/")
            }
        }
    }

    private fun setupTelegram() {
        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            if (about_social_layout.isEmpty()) {
                about_social.beGone()
                about_social_divider.beGone()
            }

            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            about_item_icon.setImageResource(R.drawable.ic_telegram_vector)
            about_item_label.setText(R.string.telegram)
            about_item_label.setTextColor(textColor)
            about_social_layout.addView(this)

            setOnClickListener {
                launchViewIntent("https://t.me/GalleryRAW")
            }
        }
    }

    private fun setupMoreApps() {
        if (resources.getBoolean(R.bool.hide_google_relations)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_heart_vector, R.string.more_apps_from_us)
            about_other_layout.addView(this)

            setOnClickListener {
                launchMoreAppsFromUsIntent()
            }
        }
    }

    private fun setupWebsite() {
        if (!resources.getBoolean(R.bool.show_donate_in_about) || resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_link_vector, R.string.website)
            about_other_layout.addView(this)

            setOnClickListener {
                launchViewIntent("https://gallery-raw.webflow.io/")
            }
        }
    }

    private fun setupPrivacyPolicy() {
        if (resources.getBoolean(R.bool.hide_all_external_links)) {
            return
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_unhide_vector, R.string.privacy_policy)
            about_other_layout.addView(this)

            setOnClickListener {
                val appId = baseConfig.appId.removeSuffix(".debug").removeSuffix(".pro").removePrefix("com.simplemobiletools.")
                val url = "https://simplemobiletools.com/privacy/$appId.txt"
                launchViewIntent(url)
            }
        }
    }

    private fun setupLicense() {
        inflater?.inflate(R.layout.item_about, null)?.apply {
            setupAboutItem(this, R.drawable.ic_article_vector, R.string.third_party_licences)
            about_other_layout.addView(this)

            setOnClickListener {
                Intent(applicationContext, LicenseActivity::class.java).apply {
                    putExtra(APP_ICON_IDS, getAppIconIDs())
                    putExtra(APP_LAUNCHER_NAME, getAppLauncherName())
                    putExtra(APP_LICENSES, intent.getLongExtra(APP_LICENSES, 0))
                    startActivity(this)
                }
            }
        }
    }

    private fun setupVersion() {
        var version = intent.getStringExtra(APP_VERSION_NAME) ?: ""
        if (baseConfig.appId.removeSuffix(".debug").endsWith(".pro")) {
            version += " ${getString(R.string.pro)}"
        }

        inflater?.inflate(R.layout.item_about, null)?.apply {
            about_item_icon.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_info_vector, textColor))
            val fullVersion = String.format(getString(R.string.version_placeholder, version))
            about_item_label.text = fullVersion
            about_item_label.setTextColor(textColor)
            about_other_layout.addView(this)

            setOnClickListener {
                if (firstVersionClickTS == 0L) {
                    firstVersionClickTS = System.currentTimeMillis()
                    Handler().postDelayed({
                        firstVersionClickTS = 0L
                        clicksSinceFirstClick = 0
                    }, EASTER_EGG_TIME_LIMIT)
                }

                clicksSinceFirstClick++
                if (clicksSinceFirstClick >= EASTER_EGG_REQUIRED_CLICKS) {
                    toast(R.string.hello)
                    firstVersionClickTS = 0L
                    clicksSinceFirstClick = 0
                }
            }
        }
    }

    private fun setupAboutItem(view: View, drawableId: Int, textId: Int) {
        view.apply {
            about_item_icon.setImageDrawable(resources.getColoredDrawableWithColor(drawableId, textColor))
            about_item_label.setText(textId)
            about_item_label.setTextColor(textColor)
        }
    }
}
