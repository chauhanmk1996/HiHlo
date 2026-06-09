package com.app.hihlo.utils

import com.app.hihlo.R
import com.app.hihlo.model.chat.MessageStatus
import com.app.hihlo.model.chat.Messages
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ChatUtils {
    private var userid: String = ""
    fun checkMessageStatus(message: Messages):Int{
        when (message.statusSent) {
//            MessageStatus.NOT_SENT.name -> {
//                return R.drawable.not_sent_icon
//            }
            MessageStatus.SENT.name -> {
                return R.drawable.send
            }
//            MessageStatus.DELIVERED.name -> {
//                return R.drawable.seen_icon
//            }
            MessageStatus.SEEN.name -> {
                return R.drawable.seen_icon
            }
        }
        return 0
    }

    fun getDay(time:String, inputFormat: String):String{
        var dateToShow= convertTimestampToAndroidTime(time, inputFormat , "dd MMM, yyyy")
        if (dateToShow== getFormattedDateToday("dd MMM, yyyy")){
            return "Today"
        } else if (dateToShow== getFormattedDateYesterday("dd MMM, yyyy")){
            return "Yesterday"
        } else{
            return dateToShow
        }
    }
    fun getChatListDay(time:String, inputFormat: String):String{
        var dateToShow= convertTimestampToAndroidTime(time, inputFormat , "dd MMM, yyyy")
        if (dateToShow== getFormattedDateToday("dd MMM, yyyy")){
            return "Today"
        } else if (dateToShow== getFormattedDateYesterday("dd MMM, yyyy")){
            return "Yesterday"
        } else{
            return convertTimestampToAndroidTime(time, inputFormat , "dd/MM/yy")
        }
    }
    fun getFormattedDateToday(requiredFormat: String): String {
        // Get the current date
        val currentDate = Date()

        // Create a SimpleDateFormat instance with the required format
        val dateFormat = SimpleDateFormat(requiredFormat, Locale.getDefault())

        // Set the timezone for formatting
        dateFormat.timeZone = TimeZone.getDefault()

        // Format the current date according to the timezone
        return dateFormat.format(currentDate)
    }

    fun getFormattedDateYesterday(requiredFormat: String): String {
        // Get the current date
        val currentDate = Date()

        // Calculate yesterday's date
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Subtract 1 day for yesterday

        // Create a SimpleDateFormat instance with the required format
        val yesterdayFormat = SimpleDateFormat(requiredFormat, Locale.getDefault())

        // Set the timezone for formatting
        yesterdayFormat.timeZone = TimeZone.getDefault()

        // Format yesterday's date according to the timezone
        return yesterdayFormat.format(calendar.time)
    }
    fun convertTimestampToAndroidTime(
        inputTimestamp: String,
        inputFormat: String,
        format: String,
        targetTimeZone: String = TimeZone.getDefault().id
    ): String {
        return try {
            // Corrected input format pattern
            val inputFormat = SimpleDateFormat(inputFormat, Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Input timestamp is in UTC by default

            val date = inputFormat.parse(inputTimestamp)

            // Format it to the desired time format and target timezone
            val outputFormat = SimpleDateFormat(format, Locale.getDefault())
            outputFormat.timeZone = TimeZone.getTimeZone(targetTimeZone) // Use target timezone

            outputFormat.format(date) ?: "Invalid date"
        } catch (e: Exception) {
            e.printStackTrace()
            "Error converting timestamp"
        }
    }
    fun getUidLoggedIn(): String {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            userid = auth.currentUser!!.uid
        }
        return userid
    }
    fun convertToTime(dateTimeString: String, oldFormat: String, newFormat: String): String {
        // Define the input format (yyyy-MM-dd HH:mm:ss)
        val inputFormatter = SimpleDateFormat(oldFormat, Locale.getDefault())

        // Define the output format (HH:mm)
        val outputFormatter = SimpleDateFormat(newFormat, Locale.getDefault())

        // Parse the input string into a Date object
        val date: Date = inputFormatter.parse(dateTimeString) ?: return "Invalid Date"

        // Format the Date object into the desired output format
        return outputFormatter.format(date).uppercase(Locale.getDefault())
    }
    val toggleChatsType = listOf("Chats", "Groups")

    fun getTime(): String {
        // Define the date and time format
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date: Date = Date(System.currentTimeMillis())

        // Format the current date and time
        return formatter.format(date)
    }
    fun getCurrentTime(requiredFormat: String): String {
        // Define the date and time format
        val formatter = SimpleDateFormat(requiredFormat)
        val date: Date = Date(System.currentTimeMillis())

        // Format the current date and time
        return formatter.format(date)
    }

}