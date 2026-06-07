package com.app.hihlo.utils

import android.util.Patterns

class AppValidator {

    companion object {

        private var mobileRegex: String = "[0-9]{9,14}"
        private var otpRegex: String = "[0-9]{6}"
        private var nameRegex: String = "[a-zA-Z ]{2,}"
        private var numberRegex: String = "[0-9]{1,9}"
        private var cardRegex: String = "[0-9]{16}"
        private var cvvRegex: String = "[0-9]{3}"

        fun String.isANumber(): Boolean {
            return this.matches(numberRegex.toRegex())
        }

        fun isValidMobileNumber(mobile: String): Boolean {
            return mobile.matches(mobileRegex.toRegex())
        }

        fun isValidOtp(otp: String): Boolean {
            return otp.matches(otpRegex.toRegex())
        }

        fun isValidName(name: String): Boolean {
            return name.matches(nameRegex.toRegex())
        }

        fun isValidAmount(amount: String): Boolean {
            return amount.toInt() <= 1
        }

        fun isValidEmail(email: String): Boolean {
            return Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        fun isValidAddress(address: String): Boolean {
            return address.isNotEmpty()
        }

        fun isValidDOB(dob: String): Boolean {
            return dob.isNotEmpty()
        }

        fun isValidCardNumber(card: String): Boolean {
            return card.matches(cardRegex.toRegex())
        }

        fun isValidCvv(cvv: String): Boolean {
            return cvv.matches(cvvRegex.toRegex())
        }

        fun isValidPassword(password: String): Boolean {
            return if (!password.contains("[A-Z]".toRegex())) {
                false
            } else if (!password.contains("[a-z]".toRegex())) {
                false
            } else if (!password.contains("[0-9]".toRegex())) {
                false
            } else password.contains("[!\"#\$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]".toRegex())
        }
    }
}