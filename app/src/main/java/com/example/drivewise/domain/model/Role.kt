package com.example.drivewise.domain.model

enum class Role(val key: String) {
    ADMIN("admin"),
    CLIENT("client");

    companion object {
        fun fromKey(key: String?): Role = values().firstOrNull { it.key == key } ?: CLIENT
    }
}
