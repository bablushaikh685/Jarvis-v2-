package com.example.bridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat

data class ContactInfo(
  val name: String,
  val phoneNumber: String,
  val type: String = "Mobile"
)

sealed class ContactCallResult {
  data class Initiated(
    val contact: ContactInfo,
    val directCall: Boolean,
    val message: String
  ) : ContactCallResult()

  data class DisambiguationRequired(
    val query: String,
    val matches: List<ContactInfo>,
    val message: String
  ) : ContactCallResult()

  data class NotFound(
    val query: String,
    val message: String
  ) : ContactCallResult()

  data class PermissionRequired(
    val permission: String,
    val message: String
  ) : ContactCallResult()

  data class Error(
    val message: String
  ) : ContactCallResult()
}

data class ActionResult(
  val success: Boolean,
  val message: String,
  val appOrTarget: String? = null,
  val packageName: String? = null
)

interface IAndroidActionBridge {
  fun openApp(appName: String): ActionResult
  fun makeCall(phoneNumber: String): ActionResult
  fun callContact(contactName: String): ContactCallResult
  fun openWhatsApp(): ActionResult
  fun openUrl(url: String): ActionResult
  fun isWhatsAppInstalled(): Boolean
  fun hasCallPermission(): Boolean
  fun hasContactsPermission(): Boolean
}

class AndroidAppActionBridge(private val context: Context) : IAndroidActionBridge {

  @JavascriptInterface
  override fun isWhatsAppInstalled(): Boolean {
    return isPackageInstalled("com.whatsapp") || isPackageInstalled("com.whatsapp.w4b")
  }

  @JavascriptInterface
  override fun hasCallPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED
  }

  @JavascriptInterface
  override fun hasContactsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
  }

  private fun isPackageInstalled(packageName: String): Boolean {
    return try {
      context.packageManager.getPackageInfo(packageName, 0)
      true
    } catch (_: PackageManager.NameNotFoundException) {
      false
    }
  }

  @JavascriptInterface
  override fun openWhatsApp(): ActionResult {
    val pm = context.packageManager
    val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    for (pkg in whatsappPackages) {
      val launchIntent = pm.getLaunchIntentForPackage(pkg)
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ActionResult(
          success = true,
          message = "Opening WhatsApp...",
          appOrTarget = "WhatsApp",
          packageName = pkg
        )
      }
    }

    // Try WhatsApp deep link intent
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(pm) != null) {
        context.startActivity(intent)
        return ActionResult(
          success = true,
          message = "Opening WhatsApp...",
          appOrTarget = "WhatsApp"
        )
      }
    } catch (_: Exception) {}

    // Not installed
    return ActionResult(
      success = false,
      message = "WhatsApp is not installed on this device.",
      appOrTarget = "WhatsApp"
    )
  }

  @JavascriptInterface
  override fun openApp(appName: String): ActionResult {
    val trimmedName = appName.trim().lowercase()

    // 1. Settings handling
    if (trimmedName.contains("setting") || trimmedName == "settings") {
      try {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(
          success = true,
          message = "Opening device Settings...",
          appOrTarget = "Settings"
        )
      } catch (e: Exception) {
        return ActionResult(
          success = false,
          message = "Failed to open Settings: ${e.localizedMessage}",
          appOrTarget = "Settings"
        )
      }
    }

    // 2. Browser handling
    if (trimmedName.contains("browser") || trimmedName.contains("internet") || trimmedName == "web" || trimmedName.contains("chrome")) {
      val browserIntents = listOfNotNull(
        // Priority 1: Standard Browser Category selector
        try {
          Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        } catch (_: Exception) { null },
        // Priority 2: Direct Chrome launch intent
        context.packageManager.getLaunchIntentForPackage("com.android.chrome")?.apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        // Priority 3: Direct Web URL view intent
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )

      for (bIntent in browserIntents) {
        try {
          context.startActivity(bIntent)
          return ActionResult(
            success = true,
            message = "Opening web browser...",
            appOrTarget = "Browser"
          )
        } catch (_: Exception) {}
      }
    }

    // 3. Camera handling
    if (trimmedName.contains("camera") || trimmedName.contains("cam")) {
      try {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
          context.startActivity(intent)
          return ActionResult(
            success = true,
            message = "Opening Camera...",
            appOrTarget = "Camera"
          )
        }
      } catch (_: Exception) {}
    }

    // 3. Known package lookup
    val knownPackages = mapOf(
      "whatsapp" to "com.whatsapp",
      "youtube" to "com.google.android.youtube",
      "instagram" to "com.instagram.android",
      "insta" to "com.instagram.android",
      "chrome" to "com.android.chrome",
      "google chrome" to "com.android.chrome",
      "maps" to "com.google.android.apps.maps",
      "google maps" to "com.google.android.apps.maps",
      "gmail" to "com.google.android.gm",
      "play store" to "com.android.vending",
      "playstore" to "com.android.vending",
      "clock" to "com.google.android.deskclock",
      "calculator" to "com.google.android.calculator"
    )

    val matchedPackage = knownPackages[trimmedName] ?: knownPackages.entries.firstOrNull {
      trimmedName.contains(it.key)
    }?.value

    val pm = context.packageManager
    if (matchedPackage != null) {
      val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ActionResult(
          success = true,
          message = "Opening $appName...",
          appOrTarget = appName,
          packageName = matchedPackage
        )
      }
    }

    // 4. Dynamic query of installed launcher applications
    try {
      val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val activities = pm.queryIntentActivities(mainIntent, 0)
      for (resolveInfo in activities) {
        val label = resolveInfo.loadLabel(pm).toString().lowercase()
        if (label.contains(trimmedName) || trimmedName.contains(label)) {
          val pkg = resolveInfo.activityInfo.packageName
          val launchIntent = pm.getLaunchIntentForPackage(pkg)
          if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return ActionResult(
              success = true,
              message = "Opening ${resolveInfo.loadLabel(pm)}...",
              appOrTarget = resolveInfo.loadLabel(pm).toString(),
              packageName = pkg
            )
          }
        }
      }
    } catch (e: Exception) {
      return ActionResult(
        success = false,
        message = "Error searching applications: ${e.localizedMessage}",
        appOrTarget = appName
      )
    }

    return ActionResult(
      success = false,
      message = "Could not find $appName installed on this device.",
      appOrTarget = appName
    )
  }

  @JavascriptInterface
  override fun makeCall(phoneNumber: String): ActionResult {
    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    if (cleanPhone.isBlank()) {
      return ActionResult(
        success = false,
        message = "Invalid phone number provided."
      )
    }

    val canDirectCall = hasCallPermission()
    return try {
      val action = if (canDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
      val intent = Intent(action, Uri.parse("tel:$cleanPhone")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(
        success = true,
        message = if (canDirectCall) "Calling $cleanPhone..." else "Opening dialer with $cleanPhone...",
        appOrTarget = cleanPhone
      )
    } catch (e: Exception) {
      // Fallback to DIAL
      try {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        ActionResult(
          success = true,
          message = "Opening phone dialer for $cleanPhone...",
          appOrTarget = cleanPhone
        )
      } catch (err: Exception) {
        ActionResult(
          success = false,
          message = "Failed to initiate call: ${err.localizedMessage}",
          appOrTarget = cleanPhone
        )
      }
    }
  }

  @JavascriptInterface
  override fun callContact(contactName: String): ContactCallResult {
    val cleanQuery = contactName.trim()
    if (cleanQuery.isBlank()) {
      return ContactCallResult.Error("Contact name cannot be empty.")
    }

    if (!hasContactsPermission()) {
      return ContactCallResult.PermissionRequired(
        permission = Manifest.permission.READ_CONTACTS,
        message = "Contacts permission is required to search and call '${cleanQuery}'. Please grant permission."
      )
    }

    val contactsList = mutableListOf<ContactInfo>()
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Phone.NUMBER,
      ContactsContract.CommonDataKinds.Phone.TYPE
    )

    val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%$cleanQuery%")

    try {
      context.contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        null
      )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

        val seen = mutableSetOf<String>()
        while (cursor.moveToNext()) {
          val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
          val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
          val typeInt = if (typeIndex >= 0) cursor.getInt(typeIndex) else 2
          val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
            context.resources,
            typeInt,
            "Mobile"
          ).toString()

          val key = "$name-$number"
          if (name.isNotBlank() && number.isNotBlank() && seen.add(key)) {
            contactsList.add(ContactInfo(name = name, phoneNumber = number, type = typeLabel))
          }
        }
      }
    } catch (e: Exception) {
      return ContactCallResult.Error("Error accessing contacts: ${e.localizedMessage}")
    }

    return when {
      contactsList.isEmpty() -> {
        ContactCallResult.NotFound(
          query = cleanQuery,
          message = "No contact found matching '$cleanQuery'."
        )
      }
      contactsList.size == 1 -> {
        val contact = contactsList.first()
        val directCall = hasCallPermission()
        makeCall(contact.phoneNumber)
        ContactCallResult.Initiated(
          contact = contact,
          directCall = directCall,
          message = "Calling ${contact.name} (${contact.phoneNumber})..."
        )
      }
      else -> {
        ContactCallResult.DisambiguationRequired(
          query = cleanQuery,
          matches = contactsList,
          message = "I found ${contactsList.size} contacts matching '$cleanQuery'. Which one should I call?"
        )
      }
    }
  }

  @JavascriptInterface
  override fun openUrl(url: String): ActionResult {
    val trimmed = url.trim()
    if (trimmed.startsWith("javascript:", ignoreCase = true) ||
        trimmed.startsWith("file:", ignoreCase = true) ||
        trimmed.startsWith("content:", ignoreCase = true) ||
        trimmed.startsWith("data:", ignoreCase = true)) {
      return ActionResult(
        success = false,
        message = "Blocked unsafe URI scheme.",
        appOrTarget = url
      )
    }

    val formattedUrl = when {
      trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
      trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
      else -> "https://$trimmed"
    }

    return try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(
        success = true,
        message = "Opening $formattedUrl...",
        appOrTarget = formattedUrl
      )
    } catch (e: Exception) {
      ActionResult(
        success = false,
        message = "Failed to open link: ${e.localizedMessage}",
        appOrTarget = formattedUrl
      )
    }
  }
}
