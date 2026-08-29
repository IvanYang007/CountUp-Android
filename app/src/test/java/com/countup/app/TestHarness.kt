package com.countup.app

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory test harness for Android Context & SharedPreferences on the JVM.
 */
class TestContext(private val baseFilesDir: File) : android.content.ContextWrapper(null) {
    private val prefsMap = ConcurrentHashMap<String, TestSharedPreferences>()

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return prefsMap.computeIfAbsent(name) { TestSharedPreferences() }
    }

    override fun getFilesDir(): File = baseFilesDir
}

class TestSharedPreferences : SharedPreferences {
    private val data = ConcurrentHashMap<String, Any>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (data[key] as? Number)?.toInt() ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (data[key] as? Number)?.toLong() ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (data[key] as? Number)?.toFloat() ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = TestEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class TestEditor(private val backingMap: ConcurrentHashMap<String, Any>) : SharedPreferences.Editor {
        private val pending = HashMap<String, Any?>()
        private var clearPending = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = null
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearPending = true
        }

        override fun commit(): Boolean {
            if (clearPending) {
                backingMap.clear()
                clearPending = false
            }
            for ((k, v) in pending) {
                if (v == null) {
                    backingMap.remove(k)
                } else {
                    backingMap[k] = v
                }
            }
            pending.clear()
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
