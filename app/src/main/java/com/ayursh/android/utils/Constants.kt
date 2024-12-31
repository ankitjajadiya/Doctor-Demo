package com.ayursh.android.utils

interface Constants {
    companion object {
        const val DOC_ID: String = "user_id"
        const val IS_FIRST_RUN: String = "is_first_run"
        const val AUTH_TOKEN: String = "auth_token"
        const val PHONE_NUMBER: String = "phone_number"
        const val NotificationList ="notification_list"
        var msgPending: String ="false"
        const val FCM_TOKEN: String = "fcm_token"
    }

    interface USER {
        companion object {
            const val DOC_ID: String = "doctor_id"
            const val PHONE_NUMBER: String = "phone_number"
            const val FIRST_NAME: String = "first_name"
            const val LAST_NAME: String = "last_name"
            const val DISPLAY_NAME: String = "display_name"
            const val GENDER: String = "gender"
            const val EXP_IN_MON: String = "experience_in_months"
            const val CONSULTATION_FEE: String = "consultation_fee"
            const val IS_CLINIC_ASSOC: String = "is_clinic_associated"
            const val EMAIL: String = "email"
            const val DOB: String = "dob"
            const val ADDRESS: String = "address"
            const val REGISTRATION_NUMBER: String = "registration_number"
            const val QUALIFICATION: String = "qualification"
            const val IS_ACTIVE: String = "is_active"
            const val INTERNAL: String = "internal"
            const val EXPERTISE: String = "expertise"

        }
    }
}
