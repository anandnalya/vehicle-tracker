package com.anandnalya.vehicletracker.data.network

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Custom converter factory to handle the non-standard JSON response
 * The API returns JavaScript-style object notation with unquoted keys
 * e.g., {root : [[{sts : 'Stopped',...}]]}
 *
 * This converter normalizes it to standard JSON before parsing
 */
class LenientJsonConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter<ResponseBody, Any> { body ->
            val rawJson = body.string()
            val normalizedJson = normalizeJson(rawJson)
            val newBody = ResponseBody.create(body.contentType(), normalizedJson)
            delegate.convert(newBody)
        }
    }

    private fun normalizeJson(input: String): String {
        // First, replace single quotes with double quotes for string values
        // This must happen before key fixing to avoid corrupting values
        var result = input.replace(Regex("""'([^']*)'""")) { match ->
            "\"${match.groupValues[1]}\""
        }

        // Now fix unquoted keys - only match keys at the start of objects or after commas
        // Match: {key : or ,key : (with possible whitespace)
        result = result.replace(Regex("""([{,])\s*(\w+)\s*:""")) { match ->
            "${match.groupValues[1]}\"${match.groupValues[2]}\":"
        }

        // Fix double-quoted keys that got extra quotes
        result = result.replace(Regex("""\"\"(\w+)\"\":""")) { match ->
            "\"${match.groupValues[1]}\":"
        }

        return result
    }
}
