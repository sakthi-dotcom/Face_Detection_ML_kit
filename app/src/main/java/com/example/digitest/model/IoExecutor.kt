package com.example.digitest.model

import javax.inject.Qualifier
//For background thread management.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoExecutor