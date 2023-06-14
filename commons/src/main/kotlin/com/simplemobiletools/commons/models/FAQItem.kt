package com.simplemobiletools.commons.models

import java.io.Serializable

data class FAQItem(val title: Any, val text: Any) : Serializable {
    companion object {
        private const val serialVersionUID = -6553345863512345L
    }
}
