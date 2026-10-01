package com.tingjian.app.data

import com.google.gson.GsonBuilder
import com.tingjian.app.network.PrivacyExportResponse

internal fun PrivacyExportResponse.toPrettyJson(): String =
    GsonBuilder().setPrettyPrinting().create().toJson(this)
