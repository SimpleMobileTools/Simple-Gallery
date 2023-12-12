package org.fossify.gallery.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogFilterMediaBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.*

class FilterMediaDialog(val activity: BaseSimpleActivity, val callback: (result: Int) -> Unit) {
    private val binding = DialogFilterMediaBinding.inflate(activity.layoutInflater)

    init {
        val filterMedia = activity.config.filterMedia
        binding.apply {
            filterMediaImages.isChecked = filterMedia and TYPE_IMAGES != 0
            filterMediaVideos.isChecked = filterMedia and TYPE_VIDEOS != 0
            filterMediaGifs.isChecked = filterMedia and TYPE_GIFS != 0
            filterMediaRaws.isChecked = filterMedia and TYPE_RAWS != 0
            filterMediaSvgs.isChecked = filterMedia and TYPE_SVGS != 0
            filterMediaPortraits.isChecked = filterMedia and TYPE_PORTRAITS != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.filter_media)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        if (binding.filterMediaImages.isChecked)
            result += TYPE_IMAGES
        if (binding.filterMediaVideos.isChecked)
            result += TYPE_VIDEOS
        if (binding.filterMediaGifs.isChecked)
            result += TYPE_GIFS
        if (binding.filterMediaRaws.isChecked)
            result += TYPE_RAWS
        if (binding.filterMediaSvgs.isChecked)
            result += TYPE_SVGS
        if (binding.filterMediaPortraits.isChecked)
            result += TYPE_PORTRAITS

        if (result == 0) {
            result = getDefaultFileFilter()
        }

        if (activity.config.filterMedia != result) {
            activity.config.filterMedia = result
            callback(result)
        }
    }
}
