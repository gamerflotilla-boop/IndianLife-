package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            "" // empty key handles fallback
        } else {
            key
        }
    }

    // 1. Text Generation & Google Maps Grounding with gemini-3.5-flash
    suspend fun generateTextWithMaps(prompt: String): MapResult {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return MapResult(
                text = "[Simulated Location Response] Here are top-rated spots near you: \n" +
                        "1. Cafe Coffee Day - 4.5★ (Grounded in Mumbai location)\n" +
                        "2. Starburst Bistro - 4.2★ (2.5 km away)\n" +
                        "Use AI premium to unlock real live search grounding with Google Maps.",
                isGrounded = false
            )
        }

        val url = "$BASE_URL/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val payload = JSONObject().apply {
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are an assistant with access to Google Maps. Ground your response in accurate geolocations: $prompt")
                        })
                    })
                })
            }
            put("contents", contentArray)
            
            // Add googleMaps tool
            val toolsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            }
            put("tools", toolsArray)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: $errBody")
                    return MapResult("AI grounding error: Response code ${response.code}", false)
                }
                val responseStr = response.body?.string() ?: ""
                val json = JSONObject(responseStr)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: "No response from Gemini API"
                
                // Real mapping grounding metadata can exist in candidate's groundingMetadata block
                val isGrounded = responseStr.contains("groundingMetadata") || responseStr.contains("googleMaps")
                MapResult(text, isGrounded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Maps Grounding call error: ${e.message}")
            MapResult("Connection failed: ${e.message}", false)
        }
    }

    // 2. TTS (Text to Speech) using gemini-3.1-flash-tts-preview
    // Returns full path of generated MP3 file, or null
    suspend fun generateSpeech(context: Context, text: String): String? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            // Generate basic local test beep / fallback file or simulate successful retrieval
            Log.d(TAG, "[Simulated TTS] Generated speech mockup for: '$text'")
            return simulateLocalSpeechFile(context, text)
        }

        val url = "$BASE_URL/gemini-3.1-flash-tts-preview:generateContent?key=$apiKey"

        val payload = JSONObject().apply {
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Say clearly and gracefully: $text")
                        })
                    })
                })
            }
            put("contents", contentArray)

            val genConfig = JSONObject().apply {
                val responseModalities = JSONArray().apply {
                    put("AUDIO")
                }
                put("responseModalities", responseModalities)

                val speechConfig = JSONObject().apply {
                    val voiceConfig = JSONObject().apply {
                        val prebuiltVoiceConfig = JSONObject().apply {
                            put("voiceName", "Kore") // Warm, friendly voice
                        }
                        put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                    }
                    put("voiceConfig", voiceConfig)
                }
                put("speechConfig", speechConfig)
            }
            put("generationConfig", genConfig)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "TTS request failed: ${response.code}")
                    return simulateLocalSpeechFile(context, text)
                }
                val responseStr = response.body?.string() ?: ""
                val json = JSONObject(responseStr)
                val candidates = json.optJSONArray("candidates")
                val part = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                
                val inlineData = part?.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data")
                    val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    
                    val file = File(context.cacheDir, "gemini_tts_${System.currentTimeMillis()}.wav")
                    FileOutputStream(file).use { fos ->
                        fos.write(audioBytes)
                    }
                    Log.d(TAG, "Speech audio file saved to path: ${file.absolutePath}")
                    file.absolutePath
                } else {
                    simulateLocalSpeechFile(context, text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS endpoint call error: ${e.message}")
            simulateLocalSpeechFile(context, text)
        }
    }

    // 3. Image Generation using gemini-3.1-flash-image-preview
    suspend fun generateImage(prompt: String): Bitmap? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.d(TAG, "[Simulated Image] Mocking image generation for prompt: '$prompt'")
            return null // Falling back to simulated artwork
        }

        val url = "$BASE_URL/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"

        val payload = JSONObject().apply {
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentArray)

            val genConfig = JSONObject().apply {
                val responseModalities = JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                }
                put("responseModalities", responseModalities)
                
                val imgConfig = JSONObject().apply {
                    put("aspectRatio", "1:1")
                    put("imageSize", "1K")
                }
                put("imageConfig", imgConfig)
            }
            put("generationConfig", genConfig)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Image generation error: ${response.code}")
                    return null
                }
                val responseStr = response.body?.string() ?: ""
                val json = JSONObject(responseStr)
                val candidates = json.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                
                var base64Img: String? = null
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null && inlineData.optString("mimeType").startsWith("image/")) {
                            base64Img = inlineData.optString("data")
                            break
                        }
                    }
                }

                if (base64Img != null) {
                    val bytes = Base64.decode(base64Img, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image generation connection error: ${e.message}")
            null
        }
    }

    private fun simulateLocalSpeechFile(context: Context, text: String): String {
        // Create an empty mockup file that plays silence or a simulated confirmation
        val file = File(context.cacheDir, "simulated_speech.mp3")
        try {
            FileOutputStream(file).use { fos ->
                val mockHeader = ByteArray(100)
                fos.write(mockHeader) // Write dummy content
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error simulating local speech file: ${e.message}")
        }
        return file.absolutePath
    }
}

data class MapResult(
    val text: String,
    val isGrounded: Boolean
)
