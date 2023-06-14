package com.simplemobiletools.commons.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.StatFs
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.helpers.*
import org.joda.time.DateTime
import org.joda.time.Years
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.text.DateFormat
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

fun String.getFilenameFromPath() = substring(lastIndexOf("/") + 1)

fun String.getFilenameExtension() = substring(lastIndexOf(".") + 1)

fun String.getBasePath(context: Context): String {
    return when {
        startsWith(context.internalStoragePath) -> context.internalStoragePath
        context.isPathOnSD(this) -> context.sdCardPath
        context.isPathOnOTG(this) -> context.otgPath
        else -> "/"
    }
}

fun String.getFirstParentDirName(context: Context, level: Int): String? {
    val basePath = getBasePath(context)
    val startIndex = basePath.length + 1
    return if (length > startIndex) {
        val pathWithoutBasePath = substring(startIndex)
        val pathSegments = pathWithoutBasePath.split("/")
        if (level < pathSegments.size) {
            pathSegments.slice(0..level).joinToString("/")
        } else {
            null
        }
    } else {
        null
    }
}

fun String.getFirstParentPath(context: Context, level: Int): String {
    val basePath = getBasePath(context)
    val startIndex = basePath.length + 1
    return if (length > startIndex) {
        val pathWithoutBasePath = substring(basePath.length + 1)
        val pathSegments = pathWithoutBasePath.split("/")
        val firstParentPath = if (level < pathSegments.size) {
            pathSegments.slice(0..level).joinToString("/")
        } else {
            pathWithoutBasePath
        }
        "$basePath/$firstParentPath"
    } else {
        basePath
    }
}

fun String.isAValidFilename(): Boolean {
    val ILLEGAL_CHARACTERS = charArrayOf('/', '\n', '\r', '\t', '\u0000', '`', '?', '*', '\\', '<', '>', '|', '\"', ':')
    ILLEGAL_CHARACTERS.forEach {
        if (contains(it))
            return false
    }
    return true
}

fun String.getOTGPublicPath(context: Context) =
    "${context.baseConfig.OTGTreeUri}/document/${context.baseConfig.OTGPartition}%3A${substring(context.baseConfig.OTGPath.length).replace("/", "%2F")}"

fun String.isMediaFile() = isImageFast() || isVideoFast() || isGif() || isRawFast() || isSvg() || isPortrait()

fun String.isWebP() = endsWith(".webp", true)

fun String.isGif() = endsWith(".gif", true)

fun String.isPng() = endsWith(".png", true)

fun String.isApng() = endsWith(".apng", true)

fun String.isJpg() = endsWith(".jpg", true) or endsWith(".jpeg", true)

fun String.isSvg() = endsWith(".svg", true)

fun String.isPortrait() = getFilenameFromPath().contains("portrait", true) && File(this).parentFile?.name?.startsWith("img_", true) == true

// fast extension checks, not guaranteed to be accurate
fun String.isVideoFast() = videoExtensions.any { endsWith(it, true) }

fun String.isImageFast() = photoExtensions.any { endsWith(it, true) }
fun String.isAudioFast() = audioExtensions.any { endsWith(it, true) }
fun String.isRawFast() = rawExtensions.any { endsWith(it, true) }

fun String.isImageSlow() = isImageFast() || getMimeType().startsWith("image") || startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())
fun String.isVideoSlow() = isVideoFast() || getMimeType().startsWith("video") || startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())
fun String.isAudioSlow() = isAudioFast() || getMimeType().startsWith("audio") || startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())

fun String.canModifyEXIF() = extensionsSupportingEXIF.any { endsWith(it, true) }

fun String.getCompressionFormat() = when (getFilenameExtension().toLowerCase()) {
    "png" -> Bitmap.CompressFormat.PNG
    "webp" -> Bitmap.CompressFormat.WEBP
    else -> Bitmap.CompressFormat.JPEG
}

fun String.areDigitsOnly() = matches(Regex("[0-9]+"))

fun String.areLettersOnly() = matches(Regex("[a-zA-Z]+"))

fun String.getGenericMimeType(): String {
    if (!contains("/"))
        return this

    val type = substring(0, indexOf("/"))
    return "$type/*"
}

fun String.getParentPath() = removeSuffix("/${getFilenameFromPath()}")
fun String.relativizeWith(path: String) = this.substring(path.length)

fun String.containsNoMedia() = File(this).containsNoMedia()

fun String.doesThisOrParentHaveNoMedia(folderNoMediaStatuses: HashMap<String, Boolean>, callback: ((path: String, hasNoMedia: Boolean) -> Unit)?) =
    File(this).doesThisOrParentHaveNoMedia(folderNoMediaStatuses, callback)

fun String.getImageResolution(context: Context): Point? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    if (context.isRestrictedSAFOnlyRoot(this)) {
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(context.getAndroidSAFUri(this)), null, options)
    } else {
        BitmapFactory.decodeFile(this, options)
    }

    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) {
        Point(options.outWidth, options.outHeight)
    } else {
        null
    }
}

fun String.getPublicUri(context: Context) = context.getDocumentFile(this)?.uri ?: ""

fun String.substringTo(cnt: Int): String {
    return if (isEmpty()) {
        ""
    } else {
        substring(0, Math.min(length, cnt))
    }
}

fun String.highlightTextPart(textToHighlight: String, color: Int, highlightAll: Boolean = false, ignoreCharsBetweenDigits: Boolean = false): SpannableString {
    val spannableString = SpannableString(this)
    if (textToHighlight.isEmpty()) {
        return spannableString
    }

    var startIndex = normalizeString().indexOf(textToHighlight, 0, true)
    val indexes = ArrayList<Int>()
    while (startIndex >= 0) {
        if (startIndex != -1) {
            indexes.add(startIndex)
        }

        startIndex = normalizeString().indexOf(textToHighlight, startIndex + textToHighlight.length, true)
        if (!highlightAll) {
            break
        }
    }

    // handle cases when we search for 643, but in reality the string contains it like 6-43
    if (ignoreCharsBetweenDigits && indexes.isEmpty()) {
        try {
            val regex = TextUtils.join("(\\D*)", textToHighlight.toCharArray().toTypedArray())
            val pattern = Pattern.compile(regex)
            val result = pattern.matcher(normalizeString())
            if (result.find()) {
                spannableString.setSpan(ForegroundColorSpan(color), result.start(), result.end(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            }
        } catch (ignored: Exception) {
        }

        return spannableString
    }

    indexes.forEach {
        val endIndex = Math.min(it + textToHighlight.length, length)
        try {
            spannableString.setSpan(ForegroundColorSpan(color), it, endIndex, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }

    return spannableString
}

fun String.searchMatches(textToHighlight: String): ArrayList<Int> {
    val indexes = arrayListOf<Int>()
    var indexOf = indexOf(textToHighlight, 0, true)

    var offset = 0
    while (offset < length && indexOf != -1) {
        indexOf = indexOf(textToHighlight, offset, true)

        if (indexOf == -1) {
            break
        } else {
            indexes.add(indexOf)
        }

        offset = indexOf + 1
    }

    return indexes
}

fun String.getFileSignature(lastModified: Long? = null) = ObjectKey(getFileKey(lastModified))

fun String.getFileKey(lastModified: Long? = null): String {
    val file = File(this)
    val modified = if (lastModified != null && lastModified > 0) {
        lastModified
    } else {
        file.lastModified()
    }

    return "${file.absolutePath}$modified"
}

fun String.getAvailableStorageB(): Long {
    return try {
        val stat = StatFs(this)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        bytesAvailable
    } catch (e: Exception) {
        -1L
    }
}

// remove diacritics, for example č -> c
fun String.normalizeString() = Normalizer.normalize(this, Normalizer.Form.NFD).replace(normalizeRegex, "")

// checks if string is a phone number
fun String.isPhoneNumber(): Boolean {
    return this.matches("^[0-9+\\-\\)\\( *#]+\$".toRegex())
}

// if we are comparing phone numbers, compare just the last 9 digits
fun String.trimToComparableNumber(): String {
    // don't trim if it's not a phone number
    if (!this.isPhoneNumber()) {
        return this
    }
    val normalizedNumber = this.normalizeString()
    val startIndex = Math.max(0, normalizedNumber.length - 9)
    return normalizedNumber.substring(startIndex)
}

// get the contact names first letter at showing the placeholder without image
fun String.getNameLetter() = normalizeString().toCharArray().getOrNull(0)?.toString()?.toUpperCase(Locale.getDefault()) ?: "A"

fun String.normalizePhoneNumber() = PhoneNumberUtils.normalizeNumber(this)

fun String.highlightTextFromNumbers(textToHighlight: String, primaryColor: Int): SpannableString {
    val spannableString = SpannableString(this)
    val digits = PhoneNumberUtils.convertKeypadLettersToDigits(this)
    if (digits.contains(textToHighlight)) {
        val startIndex = digits.indexOf(textToHighlight, 0, true)
        val endIndex = Math.min(startIndex + textToHighlight.length, length)
        try {
            spannableString.setSpan(ForegroundColorSpan(primaryColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }

    return spannableString
}

fun String.getDateTimeFromDateString(showYearsSince: Boolean, viewToUpdate: TextView? = null): DateTime {
    val dateFormats = getDateFormats()
    var date = DateTime()
    for (format in dateFormats) {
        try {
            date = DateTime.parse(this, DateTimeFormat.forPattern(format))

            val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            var localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()

            val hasYear = format.contains("y")
            if (!hasYear) {
                localPattern = localPattern.replace("y", "").replace(",", "").trim()
                date = date.withYear(DateTime().year)
            }

            var formattedString = date.toString(localPattern)
            if (showYearsSince && hasYear) {
                formattedString += " (${Years.yearsBetween(date, DateTime.now()).years})"
            }

            viewToUpdate?.text = formattedString
            break
        } catch (ignored: Exception) {
        }
    }
    return date
}

fun String.getMimeType(): String {
    val typesMap = HashMap<String, String>().apply {
        put("323", "text/h323")
        put("3g2", "video/3gpp2")
        put("3gp", "video/3gpp")
        put("3gp2", "video/3gpp2")
        put("3gpp", "video/3gpp")
        put("7z", "application/x-7z-compressed")
        put("aa", "audio/audible")
        put("aac", "audio/aac")
        put("aaf", "application/octet-stream")
        put("aax", "audio/vnd.audible.aax")
        put("ac3", "audio/ac3")
        put("aca", "application/octet-stream")
        put("accda", "application/msaccess.addin")
        put("accdb", "application/msaccess")
        put("accdc", "application/msaccess.cab")
        put("accde", "application/msaccess")
        put("accdr", "application/msaccess.runtime")
        put("accdt", "application/msaccess")
        put("accdw", "application/msaccess.webapplication")
        put("accft", "application/msaccess.ftemplate")
        put("acx", "application/internet-property-stream")
        put("addin", "text/xml")
        put("ade", "application/msaccess")
        put("adobebridge", "application/x-bridge-url")
        put("adp", "application/msaccess")
        put("adt", "audio/vnd.dlna.adts")
        put("adts", "audio/aac")
        put("afm", "application/octet-stream")
        put("ai", "application/postscript")
        put("aif", "audio/aiff")
        put("aifc", "audio/aiff")
        put("aiff", "audio/aiff")
        put("air", "application/vnd.adobe.air-application-installer-package+zip")
        put("amc", "application/mpeg")
        put("anx", "application/annodex")
        put("apk", "application/vnd.android.package-archive")
        put("application", "application/x-ms-application")
        put("art", "image/x-jg")
        put("asa", "application/xml")
        put("asax", "application/xml")
        put("ascx", "application/xml")
        put("asd", "application/octet-stream")
        put("asf", "video/x-ms-asf")
        put("ashx", "application/xml")
        put("asi", "application/octet-stream")
        put("asm", "text/plain")
        put("asmx", "application/xml")
        put("aspx", "application/xml")
        put("asr", "video/x-ms-asf")
        put("asx", "video/x-ms-asf")
        put("atom", "application/atom+xml")
        put("au", "audio/basic")
        put("avi", "video/x-msvideo")
        put("axa", "audio/annodex")
        put("axs", "application/olescript")
        put("axv", "video/annodex")
        put("bas", "text/plain")
        put("bcpio", "application/x-bcpio")
        put("bin", "application/octet-stream")
        put("bmp", "image/bmp")
        put("c", "text/plain")
        put("cab", "application/octet-stream")
        put("caf", "audio/x-caf")
        put("calx", "application/vnd.ms-office.calx")
        put("cat", "application/vnd.ms-pki.seccat")
        put("cc", "text/plain")
        put("cd", "text/plain")
        put("cdda", "audio/aiff")
        put("cdf", "application/x-cdf")
        put("cer", "application/x-x509-ca-cert")
        put("cfg", "text/plain")
        put("chm", "application/octet-stream")
        put("class", "application/x-java-applet")
        put("clp", "application/x-msclip")
        put("cmd", "text/plain")
        put("cmx", "image/x-cmx")
        put("cnf", "text/plain")
        put("cod", "image/cis-cod")
        put("config", "application/xml")
        put("contact", "text/x-ms-contact")
        put("coverage", "application/xml")
        put("cpio", "application/x-cpio")
        put("cpp", "text/plain")
        put("crd", "application/x-mscardfile")
        put("crl", "application/pkix-crl")
        put("crt", "application/x-x509-ca-cert")
        put("cs", "text/plain")
        put("csdproj", "text/plain")
        put("csh", "application/x-csh")
        put("csproj", "text/plain")
        put("css", "text/css")
        put("csv", "text/csv")
        put("cur", "application/octet-stream")
        put("cxx", "text/plain")
        put("dat", "application/octet-stream")
        put("datasource", "application/xml")
        put("dbproj", "text/plain")
        put("dcr", "application/x-director")
        put("def", "text/plain")
        put("deploy", "application/octet-stream")
        put("der", "application/x-x509-ca-cert")
        put("dgml", "application/xml")
        put("dib", "image/bmp")
        put("dif", "video/x-dv")
        put("dir", "application/x-director")
        put("disco", "text/xml")
        put("divx", "video/divx")
        put("dll", "application/x-msdownload")
        put("dll.config", "text/xml")
        put("dlm", "text/dlm")
        put("dng", "image/x-adobe-dng")
        put("doc", "application/msword")
        put("docm", "application/vnd.ms-word.document.macroEnabled.12")
        put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        put("dot", "application/msword")
        put("dotm", "application/vnd.ms-word.template.macroEnabled.12")
        put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template")
        put("dsp", "application/octet-stream")
        put("dsw", "text/plain")
        put("dtd", "text/xml")
        put("dtsconfig", "text/xml")
        put("dv", "video/x-dv")
        put("dvi", "application/x-dvi")
        put("dwf", "drawing/x-dwf")
        put("dwp", "application/octet-stream")
        put("dxr", "application/x-director")
        put("eml", "message/rfc822")
        put("emz", "application/octet-stream")
        put("eot", "application/vnd.ms-fontobject")
        put("eps", "application/postscript")
        put("etl", "application/etl")
        put("etx", "text/x-setext")
        put("evy", "application/envoy")
        put("exe", "application/octet-stream")
        put("exe.config", "text/xml")
        put("fdf", "application/vnd.fdf")
        put("fif", "application/fractals")
        put("filters", "application/xml")
        put("fla", "application/octet-stream")
        put("flac", "audio/flac")
        put("flr", "x-world/x-vrml")
        put("flv", "video/x-flv")
        put("fsscript", "application/fsharp-script")
        put("fsx", "application/fsharp-script")
        put("generictest", "application/xml")
        put("gif", "image/gif")
        put("group", "text/x-ms-group")
        put("gsm", "audio/x-gsm")
        put("gtar", "application/x-gtar")
        put("gz", "application/x-gzip")
        put("h", "text/plain")
        put("hdf", "application/x-hdf")
        put("hdml", "text/x-hdml")
        put("hhc", "application/x-oleobject")
        put("hhk", "application/octet-stream")
        put("hhp", "application/octet-stream")
        put("hlp", "application/winhlp")
        put("hpp", "text/plain")
        put("hqx", "application/mac-binhex40")
        put("hta", "application/hta")
        put("htc", "text/x-component")
        put("htm", "text/html")
        put("html", "text/html")
        put("htt", "text/webviewhtml")
        put("hxa", "application/xml")
        put("hxc", "application/xml")
        put("hxd", "application/octet-stream")
        put("hxe", "application/xml")
        put("hxf", "application/xml")
        put("hxh", "application/octet-stream")
        put("hxi", "application/octet-stream")
        put("hxk", "application/xml")
        put("hxq", "application/octet-stream")
        put("hxr", "application/octet-stream")
        put("hxs", "application/octet-stream")
        put("hxt", "text/html")
        put("hxv", "application/xml")
        put("hxw", "application/octet-stream")
        put("hxx", "text/plain")
        put("i", "text/plain")
        put("ico", "image/x-icon")
        put("ics", "text/calendar")
        put("idl", "text/plain")
        put("ief", "image/ief")
        put("iii", "application/x-iphone")
        put("inc", "text/plain")
        put("inf", "application/octet-stream")
        put("ini", "text/plain")
        put("inl", "text/plain")
        put("ins", "application/x-internet-signup")
        put("ipa", "application/x-itunes-ipa")
        put("ipg", "application/x-itunes-ipg")
        put("ipproj", "text/plain")
        put("ipsw", "application/x-itunes-ipsw")
        put("iqy", "text/x-ms-iqy")
        put("isp", "application/x-internet-signup")
        put("ite", "application/x-itunes-ite")
        put("itlp", "application/x-itunes-itlp")
        put("itms", "application/x-itunes-itms")
        put("itpc", "application/x-itunes-itpc")
        put("ivf", "video/x-ivf")
        put("jar", "application/java-archive")
        put("java", "application/octet-stream")
        put("jck", "application/liquidmotion")
        put("jcz", "application/liquidmotion")
        put("jfif", "image/pjpeg")
        put("jnlp", "application/x-java-jnlp-file")
        put("jpb", "application/octet-stream")
        put("jpe", "image/jpeg")
        put("jpeg", "image/jpeg")
        put("jpg", "image/jpeg")
        put("js", "application/javascript")
        put("json", "application/json")
        put("jsx", "text/jscript")
        put("jsxbin", "text/plain")
        put("latex", "application/x-latex")
        put("library-ms", "application/windows-library+xml")
        put("lit", "application/x-ms-reader")
        put("loadtest", "application/xml")
        put("lpk", "application/octet-stream")
        put("lsf", "video/x-la-asf")
        put("lst", "text/plain")
        put("lsx", "video/x-la-asf")
        put("lzh", "application/octet-stream")
        put("m13", "application/x-msmediaview")
        put("m14", "application/x-msmediaview")
        put("m1v", "video/mpeg")
        put("m2t", "video/vnd.dlna.mpeg-tts")
        put("m2ts", "video/vnd.dlna.mpeg-tts")
        put("m2v", "video/mpeg")
        put("m3u", "audio/x-mpegurl")
        put("m3u8", "audio/x-mpegurl")
        put("m4a", "audio/m4a")
        put("m4b", "audio/m4b")
        put("m4p", "audio/m4p")
        put("m4r", "audio/x-m4r")
        put("m4v", "video/x-m4v")
        put("mac", "image/x-macpaint")
        put("mak", "text/plain")
        put("man", "application/x-troff-man")
        put("manifest", "application/x-ms-manifest")
        put("map", "text/plain")
        put("master", "application/xml")
        put("mda", "application/msaccess")
        put("mdb", "application/x-msaccess")
        put("mde", "application/msaccess")
        put("mdp", "application/octet-stream")
        put("me", "application/x-troff-me")
        put("mfp", "application/x-shockwave-flash")
        put("mht", "message/rfc822")
        put("mhtml", "message/rfc822")
        put("mid", "audio/mid")
        put("midi", "audio/mid")
        put("mix", "application/octet-stream")
        put("mk", "text/plain")
        put("mkv", "video/x-matroska")
        put("mmf", "application/x-smaf")
        put("mno", "text/xml")
        put("mny", "application/x-msmoney")
        put("mod", "video/mpeg")
        put("mov", "video/quicktime")
        put("movie", "video/x-sgi-movie")
        put("mp2", "video/mpeg")
        put("mp2v", "video/mpeg")
        put("mp3", "audio/mpeg")
        put("mp4", "video/mp4")
        put("mp4v", "video/mp4")
        put("mpa", "video/mpeg")
        put("mpe", "video/mpeg")
        put("mpeg", "video/mpeg")
        put("mpf", "application/vnd.ms-mediapackage")
        put("mpg", "video/mpeg")
        put("mpp", "application/vnd.ms-project")
        put("mpv2", "video/mpeg")
        put("mqv", "video/quicktime")
        put("ms", "application/x-troff-ms")
        put("msi", "application/octet-stream")
        put("mso", "application/octet-stream")
        put("mts", "video/vnd.dlna.mpeg-tts")
        put("mtx", "application/xml")
        put("mvb", "application/x-msmediaview")
        put("mvc", "application/x-miva-compiled")
        put("mxp", "application/x-mmxp")
        put("nc", "application/x-netcdf")
        put("nsc", "video/x-ms-asf")
        put("nws", "message/rfc822")
        put("ocx", "application/octet-stream")
        put("oda", "application/oda")
        put("odb", "application/vnd.oasis.opendocument.database")
        put("odc", "application/vnd.oasis.opendocument.chart")
        put("odf", "application/vnd.oasis.opendocument.formula")
        put("odg", "application/vnd.oasis.opendocument.graphics")
        put("odh", "text/plain")
        put("odi", "application/vnd.oasis.opendocument.image")
        put("odl", "text/plain")
        put("odm", "application/vnd.oasis.opendocument.text-master")
        put("odp", "application/vnd.oasis.opendocument.presentation")
        put("ods", "application/vnd.oasis.opendocument.spreadsheet")
        put("odt", "application/vnd.oasis.opendocument.text")
        put("oga", "audio/ogg")
        put("ogg", "audio/ogg")
        put("ogv", "video/ogg")
        put("ogx", "application/ogg")
        put("one", "application/onenote")
        put("onea", "application/onenote")
        put("onepkg", "application/onenote")
        put("onetmp", "application/onenote")
        put("onetoc", "application/onenote")
        put("onetoc2", "application/onenote")
        put("opus", "audio/ogg")
        put("orderedtest", "application/xml")
        put("osdx", "application/opensearchdescription+xml")
        put("otf", "application/font-sfnt")
        put("otg", "application/vnd.oasis.opendocument.graphics-template")
        put("oth", "application/vnd.oasis.opendocument.text-web")
        put("otp", "application/vnd.oasis.opendocument.presentation-template")
        put("ots", "application/vnd.oasis.opendocument.spreadsheet-template")
        put("ott", "application/vnd.oasis.opendocument.text-template")
        put("oxt", "application/vnd.openofficeorg.extension")
        put("p10", "application/pkcs10")
        put("p12", "application/x-pkcs12")
        put("p7b", "application/x-pkcs7-certificates")
        put("p7c", "application/pkcs7-mime")
        put("p7m", "application/pkcs7-mime")
        put("p7r", "application/x-pkcs7-certreqresp")
        put("p7s", "application/pkcs7-signature")
        put("pbm", "image/x-portable-bitmap")
        put("pcast", "application/x-podcast")
        put("pct", "image/pict")
        put("pcx", "application/octet-stream")
        put("pcz", "application/octet-stream")
        put("pdf", "application/pdf")
        put("pfb", "application/octet-stream")
        put("pfm", "application/octet-stream")
        put("pfx", "application/x-pkcs12")
        put("pgm", "image/x-portable-graymap")
        put("php", "text/plain")
        put("pic", "image/pict")
        put("pict", "image/pict")
        put("pkgdef", "text/plain")
        put("pkgundef", "text/plain")
        put("pko", "application/vnd.ms-pki.pko")
        put("pls", "audio/scpls")
        put("pma", "application/x-perfmon")
        put("pmc", "application/x-perfmon")
        put("pml", "application/x-perfmon")
        put("pmr", "application/x-perfmon")
        put("pmw", "application/x-perfmon")
        put("png", "image/png")
        put("pnm", "image/x-portable-anymap")
        put("pnt", "image/x-macpaint")
        put("pntg", "image/x-macpaint")
        put("pnz", "image/png")
        put("pot", "application/vnd.ms-powerpoint")
        put("potm", "application/vnd.ms-powerpoint.template.macroEnabled.12")
        put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template")
        put("ppa", "application/vnd.ms-powerpoint")
        put("ppam", "application/vnd.ms-powerpoint.addin.macroEnabled.12")
        put("ppm", "image/x-portable-pixmap")
        put("pps", "application/vnd.ms-powerpoint")
        put("ppsm", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12")
        put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow")
        put("ppt", "application/vnd.ms-powerpoint")
        put("pptm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12")
        put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
        put("prf", "application/pics-rules")
        put("prm", "application/octet-stream")
        put("prx", "application/octet-stream")
        put("ps", "application/postscript")
        put("psc1", "application/PowerShell")
        put("psd", "application/octet-stream")
        put("psess", "application/xml")
        put("psm", "application/octet-stream")
        put("psp", "application/octet-stream")
        put("pub", "application/x-mspublisher")
        put("pwz", "application/vnd.ms-powerpoint")
        put("py", "text/plain")
        put("qht", "text/x-html-insertion")
        put("qhtm", "text/x-html-insertion")
        put("qt", "video/quicktime")
        put("qti", "image/x-quicktime")
        put("qtif", "image/x-quicktime")
        put("qtl", "application/x-quicktimeplayer")
        put("qxd", "application/octet-stream")
        put("ra", "audio/x-pn-realaudio")
        put("ram", "audio/x-pn-realaudio")
        put("rar", "application/x-rar-compressed")
        put("ras", "image/x-cmu-raster")
        put("rat", "application/rat-file")
        put("rb", "text/plain")
        put("rc", "text/plain")
        put("rc2", "text/plain")
        put("rct", "text/plain")
        put("rdlc", "application/xml")
        put("reg", "text/plain")
        put("resx", "application/xml")
        put("rf", "image/vnd.rn-realflash")
        put("rgb", "image/x-rgb")
        put("rgs", "text/plain")
        put("rm", "application/vnd.rn-realmedia")
        put("rmi", "audio/mid")
        put("rmp", "application/vnd.rn-rn_music_package")
        put("roff", "application/x-troff")
        put("rpm", "audio/x-pn-realaudio-plugin")
        put("rqy", "text/x-ms-rqy")
        put("rtf", "application/rtf")
        put("rtx", "text/richtext")
        put("ruleset", "application/xml")
        put("s", "text/plain")
        put("safariextz", "application/x-safari-safariextz")
        put("scd", "application/x-msschedule")
        put("scr", "text/plain")
        put("sct", "text/scriptlet")
        put("sd2", "audio/x-sd2")
        put("sdp", "application/sdp")
        put("sea", "application/octet-stream")
        put("searchConnector-ms", "application/windows-search-connector+xml")
        put("setpay", "application/set-payment-initiation")
        put("setreg", "application/set-registration-initiation")
        put("settings", "application/xml")
        put("sgimb", "application/x-sgimb")
        put("sgml", "text/sgml")
        put("sh", "application/x-sh")
        put("shar", "application/x-shar")
        put("shtml", "text/html")
        put("sit", "application/x-stuffit")
        put("sitemap", "application/xml")
        put("skin", "application/xml")
        put("sldm", "application/vnd.ms-powerpoint.slide.macroEnabled.12")
        put("sldx", "application/vnd.openxmlformats-officedocument.presentationml.slide")
        put("slk", "application/vnd.ms-excel")
        put("sln", "text/plain")
        put("slupkg-ms", "application/x-ms-license")
        put("smd", "audio/x-smd")
        put("smi", "application/octet-stream")
        put("smx", "audio/x-smd")
        put("smz", "audio/x-smd")
        put("snd", "audio/basic")
        put("snippet", "application/xml")
        put("snp", "application/octet-stream")
        put("sol", "text/plain")
        put("sor", "text/plain")
        put("spc", "application/x-pkcs7-certificates")
        put("spl", "application/futuresplash")
        put("spx", "audio/ogg")
        put("src", "application/x-wais-source")
        put("srf", "text/plain")
        put("ssisdeploymentmanifest", "text/xml")
        put("ssm", "application/streamingmedia")
        put("sst", "application/vnd.ms-pki.certstore")
        put("stl", "application/vnd.ms-pki.stl")
        put("sv4cpio", "application/x-sv4cpio")
        put("sv4crc", "application/x-sv4crc")
        put("svc", "application/xml")
        put("svg", "image/svg+xml")
        put("swf", "application/x-shockwave-flash")
        put("t", "application/x-troff")
        put("tar", "application/x-tar")
        put("tcl", "application/x-tcl")
        put("testrunconfig", "application/xml")
        put("testsettings", "application/xml")
        put("tex", "application/x-tex")
        put("texi", "application/x-texinfo")
        put("texinfo", "application/x-texinfo")
        put("tgz", "application/x-compressed")
        put("thmx", "application/vnd.ms-officetheme")
        put("thn", "application/octet-stream")
        put("tif", "image/tiff")
        put("tiff", "image/tiff")
        put("tlh", "text/plain")
        put("tli", "text/plain")
        put("toc", "application/octet-stream")
        put("tr", "application/x-troff")
        put("trm", "application/x-msterminal")
        put("trx", "application/xml")
        put("ts", "video/vnd.dlna.mpeg-tts")
        put("tsv", "text/tab-separated-values")
        put("ttf", "application/font-sfnt")
        put("tts", "video/vnd.dlna.mpeg-tts")
        put("txt", "text/plain")
        put("u32", "application/octet-stream")
        put("uls", "text/iuls")
        put("user", "text/plain")
        put("ustar", "application/x-ustar")
        put("vb", "text/plain")
        put("vbdproj", "text/plain")
        put("vbk", "video/mpeg")
        put("vbproj", "text/plain")
        put("vbs", "text/vbscript")
        put("vcf", "text/x-vcard")
        put("vcproj", "application/xml")
        put("vcs", "text/calendar")
        put("vcxproj", "application/xml")
        put("vddproj", "text/plain")
        put("vdp", "text/plain")
        put("vdproj", "text/plain")
        put("vdx", "application/vnd.ms-visio.viewer")
        put("vml", "text/xml")
        put("vscontent", "application/xml")
        put("vsct", "text/xml")
        put("vsd", "application/vnd.visio")
        put("vsi", "application/ms-vsi")
        put("vsix", "application/vsix")
        put("vsixlangpack", "text/xml")
        put("vsixmanifest", "text/xml")
        put("vsmdi", "application/xml")
        put("vspscc", "text/plain")
        put("vss", "application/vnd.visio")
        put("vsscc", "text/plain")
        put("vssettings", "text/xml")
        put("vssscc", "text/plain")
        put("vst", "application/vnd.visio")
        put("vstemplate", "text/xml")
        put("vsto", "application/x-ms-vsto")
        put("vsw", "application/vnd.visio")
        put("vsx", "application/vnd.visio")
        put("vtx", "application/vnd.visio")
        put("wav", "audio/wav")
        put("wave", "audio/wav")
        put("wax", "audio/x-ms-wax")
        put("wbk", "application/msword")
        put("wbmp", "image/vnd.wap.wbmp")
        put("wcm", "application/vnd.ms-works")
        put("wdb", "application/vnd.ms-works")
        put("wdp", "image/vnd.ms-photo")
        put("webarchive", "application/x-safari-webarchive")
        put("webm", "video/webm")
        put("webp", "image/webp")
        put("webtest", "application/xml")
        put("wiq", "application/xml")
        put("wiz", "application/msword")
        put("wks", "application/vnd.ms-works")
        put("wlmp", "application/wlmoviemaker")
        put("wlpginstall", "application/x-wlpg-detect")
        put("wlpginstall3", "application/x-wlpg3-detect")
        put("wm", "video/x-ms-wm")
        put("wma", "audio/x-ms-wma")
        put("wmd", "application/x-ms-wmd")
        put("wmf", "application/x-msmetafile")
        put("wml", "text/vnd.wap.wml")
        put("wmlc", "application/vnd.wap.wmlc")
        put("wmls", "text/vnd.wap.wmlscript")
        put("wmlsc", "application/vnd.wap.wmlscriptc")
        put("wmp", "video/x-ms-wmp")
        put("wmv", "video/x-ms-wmv")
        put("wmx", "video/x-ms-wmx")
        put("wmz", "application/x-ms-wmz")
        put("woff", "application/font-woff")
        put("wpl", "application/vnd.ms-wpl")
        put("wps", "application/vnd.ms-works")
        put("wri", "application/x-mswrite")
        put("wrl", "x-world/x-vrml")
        put("wrz", "x-world/x-vrml")
        put("wsc", "text/scriptlet")
        put("wsdl", "text/xml")
        put("wvx", "video/x-ms-wvx")
        put("x", "application/directx")
        put("xaf", "x-world/x-vrml")
        put("xaml", "application/xaml+xml")
        put("xap", "application/x-silverlight-app")
        put("xbap", "application/x-ms-xbap")
        put("xbm", "image/x-xbitmap")
        put("xdr", "text/plain")
        put("xht", "application/xhtml+xml")
        put("xhtml", "application/xhtml+xml")
        put("xla", "application/vnd.ms-excel")
        put("xlam", "application/vnd.ms-excel.addin.macroEnabled.12")
        put("xlc", "application/vnd.ms-excel")
        put("xld", "application/vnd.ms-excel")
        put("xlk", "application/vnd.ms-excel")
        put("xll", "application/vnd.ms-excel")
        put("xlm", "application/vnd.ms-excel")
        put("xls", "application/vnd.ms-excel")
        put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12")
        put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12")
        put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        put("xlt", "application/vnd.ms-excel")
        put("xltm", "application/vnd.ms-excel.template.macroEnabled.12")
        put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template")
        put("xlw", "application/vnd.ms-excel")
        put("xml", "text/xml")
        put("xmta", "application/xml")
        put("xof", "x-world/x-vrml")
        put("xoml", "text/plain")
        put("xpm", "image/x-xpixmap")
        put("xps", "application/vnd.ms-xpsdocument")
        put("xrm-ms", "text/xml")
        put("xsc", "application/xml")
        put("xsd", "text/xml")
        put("xsf", "text/xml")
        put("xsl", "text/xml")
        put("xslt", "text/xml")
        put("xsn", "application/octet-stream")
        put("xss", "application/xml")
        put("xspf", "application/xspf+xml")
        put("xtp", "application/octet-stream")
        put("xwd", "image/x-xwindowdump")
        put("z", "application/x-compress")
        put("zip", "application/zip")
    }

    return typesMap[getFilenameExtension().toLowerCase()] ?: ""
}

fun String.isBlockedNumberPattern() = contains("*")
