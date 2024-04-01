package com.example.universalappnotifier.exceptionhandling

class CustomException(val errorCode: String, message: String) : Exception(message)