package com.simplemobiletools.commons.adapters

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.getFilePlaceholderDrawables
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.item_filepicker_list.view.*
import java.util.*

class FilepickerItemsAdapter(
    activity: BaseSimpleActivity, val fileDirItems: List<FileDirItem>, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    private lateinit var fileDrawable: Drawable
    private lateinit var folderDrawable: Drawable
    private var fileDrawables = HashMap<String, Drawable>()
    private val hasOTGConnected = activity.hasOTGConnected()
    private var fontSize = 0f
    private val cornerRadius = resources.getDimension(R.dimen.rounded_corner_radius_small).toInt()
    private val dateFormat = activity.baseConfig.dateFormat
    private val timeFormat = activity.getTimeFormat()

    init {
        initDrawables()
        fontSize = activity.getTextSize()
    }

    override fun getActionMenuId() = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_filepicker_list, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileDirItem = fileDirItems[position]
        holder.bindView(fileDirItem, true, false) { itemView, adapterPosition ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = fileDirItems.size

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = fileDirItems.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemKeyPosition(key: Int) = fileDirItems.indexOfFirst { it.path.hashCode() == key }

    override fun getItemSelectionKey(position: Int) = fileDirItems[position].path.hashCode()

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.list_item_icon!!)
        }
    }

    private fun setupView(view: View, fileDirItem: FileDirItem) {
        view.apply {
            list_item_name.text = fileDirItem.name
            list_item_name.setTextColor(textColor)
            list_item_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            list_item_details.setTextColor(textColor)
            list_item_details.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            if (fileDirItem.isDirectory) {
                list_item_icon.setImageDrawable(folderDrawable)
                list_item_details.text = getChildrenCnt(fileDirItem)
            } else {
                list_item_details.text = fileDirItem.size.formatSize()
                val path = fileDirItem.path
                val placeholder = fileDrawables.getOrElse(fileDirItem.name.substringAfterLast(".").toLowerCase(Locale.getDefault()), { fileDrawable })
                val options = RequestOptions()
                    .signature(fileDirItem.getKey())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .error(placeholder)

                var itemToLoad = if (fileDirItem.name.endsWith(".apk", true)) {
                    val packageInfo = context.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
                    if (packageInfo != null) {
                        val appInfo = packageInfo.applicationInfo
                        appInfo.sourceDir = path
                        appInfo.publicSourceDir = path
                        appInfo.loadIcon(context.packageManager)
                    } else {
                        path
                    }
                } else {
                    path
                }

                if (!activity.isDestroyed && !activity.isFinishing) {
                    if (activity.isRestrictedSAFOnlyRoot(path)) {
                        itemToLoad = activity.getAndroidSAFUri(path)
                    } else if (hasOTGConnected && itemToLoad is String && activity.isPathOnOTG(itemToLoad)) {
                        itemToLoad = itemToLoad.getOTGPublicPath(activity)
                    }

                    if (itemToLoad.toString().isGif()) {
                        Glide.with(activity).asBitmap().load(itemToLoad).apply(options).into(list_item_icon)
                    } else {
                        Glide.with(activity)
                            .load(itemToLoad)
                            .transition(withCrossFade())
                            .apply(options)
                            .transform(CenterCrop(), RoundedCorners(cornerRadius))
                            .into(list_item_icon)
                    }
                }
            }
        }
    }

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return activity.resources.getQuantityString(R.plurals.items, children, children)
    }

    private fun initDrawables() {
        folderDrawable = resources.getColoredDrawableWithColor(R.drawable.ic_folder_vector, textColor)
        folderDrawable.alpha = 180
        fileDrawable = resources.getDrawable(R.drawable.ic_file_generic)
        fileDrawables = getFilePlaceholderDrawables(activity)
    }

    override fun onChange(position: Int) = fileDirItems.getOrNull(position)?.getBubbleText(activity, dateFormat, timeFormat) ?: ""
}
