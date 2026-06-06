package com.app.hihlo.model.staticModel

data class NotificationsTypeModel(
    var general: Boolean?=true,
    var audioCall: Boolean?=true,
    var videoCall: Boolean?=true,
    var payments: Boolean?=true,
    var following: Boolean?=true,
    var followers: Boolean?=true
)