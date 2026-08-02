package com.dolo.core.engine

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WebDownloadInterceptorEntryPoint {
    fun interceptor(): WebDownloadInterceptor
}
